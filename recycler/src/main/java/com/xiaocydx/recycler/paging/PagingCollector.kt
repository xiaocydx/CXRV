package com.xiaocydx.recycler.paging

import android.os.SystemClock
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.recycler.extension.*
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.UpdateOp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll

private const val PAGING_COLLECTOR_KEY = "com.xiaocydx.recycler.paging.PAGING_COLLECTOR_KEY"

/**
 * 分页数据收集器
 *
 * ### `Flow<PagingData>`
 * [PagingCollector.emit]负责收集指定流的[PagingData]。
 *
 * ### 加载状态
 * * [PagingCollector.loadStates]返回当前加载状态集合。
 * * [PagingCollector.addLoadStatesListener]添加加载状态集合已更改的监听。
 * * [PagingCollector.doOnLoadStatesChanged]添加加载状态集合已更改的处理程序。
 *
 * ### 分页操作
 * * [PagingCollector.refresh]刷新加载，获取新的[PagingData]。
 * * [PagingCollector.retry]重新加载，会对加载状态做判断，避免冗余请求。
 */
val <T : Any> ListAdapter<T, *>.pagingCollector: PagingCollector<T>
    get() {
        var collector: PagingCollector<T>? = getTag(PAGING_COLLECTOR_KEY)
        if (collector == null) {
            collector = PagingCollector(this)
            setTag(PAGING_COLLECTOR_KEY, collector)
        }
        return collector
    }

/**
 * 收集[flow]的所有值，并将它们发送给[pagingCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.collect { value ->
 *     adapter.pagingCollector.emit(value)
 * }
 *
 * // 简化上面的写法
 * adapter.pagingCollector.emitAll(flow)
 *
 * // 再进行简化
 * adapter.emitAll(flow)
 * ```
 */
suspend fun <T : Any> ListAdapter<T, *>.emitAll(
    flow: Flow<PagingData<T>>
) = pagingCollector.emitAll(flow)

/**
 * 分页数据收集器，负责收集指定流的[PagingData]
 */
class PagingCollector<T : Any> internal constructor(
    internal val adapter: ListAdapter<T, *>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : FlowCollector<PagingData<T>> {
    private var updateVersion = 0
    private var resumeJob: Job? = null
    private var mediator: PagingMediator? = null
    private var listeners: ArrayList<LoadStatesListener>? = null
    private var refreshCompleteWhen = IMMEDIATE_COMPLETE
    private var refreshScrollEnabled = true
    private val RecyclerView.isStaggered: Boolean
        get() = layoutManager is StaggeredGridLayoutManager
    var loadStates: LoadStates = LoadStates.Incomplete
        private set

    init {
        assertMainThread()
        AppendTrigger(this)
        adapter.addListExecuteListener { op ->
            mediator?.asListMediator<T>()?.updateList(op)
        }
    }

    /**
     * 刷新加载，获取新的[PagingData]
     */
    @MainThread
    fun refresh() {
        refreshAtLeast(IMMEDIATE_COMPLETE)
    }

    /**
     * 刷新加载，获取新的[PagingData]
     *
     * [duration]表示加载开始到加载完成这个过程的至少持续时间，单位ms，
     * 例如[duration]为200ms，加载完成耗时为150ms，则挂起50ms，50ms后才更新列表和加载状态。
     *
     * 下拉刷新场景可以调用该函数，对[duration]传入下拉刷新动画的至少持续时间，
     * 可以避免刷新加载太快完成，导致下拉刷新动画很快结束的问题，提升用户体验。
     */
    @MainThread
    fun refreshAtLeast(duration: Long) {
        assertMainThread()
        refreshCompleteWhen = SystemClock.uptimeMillis() + duration
        mediator?.refresh()
    }

    /**
     * 重新加载，该函数会对加载状态做判断，避免冗余请求
     */
    @MainThread
    fun retry() {
        assertMainThread()
        mediator?.retry()
    }

    /**
     * 是否启用刷新加载时滚动到第一个item
     */
    @MainThread
    fun setRefreshScrollEnabled(enabled: Boolean) {
        assertMainThread()
        refreshScrollEnabled = enabled
    }

    /**
     * 添加加载状态集合已更改的监听
     *
     * * [LoadStatesListener.onLoadStatesChanged]中可以调用[removeLoadStatesListener]。
     * * [LoadStatesListener.onLoadStatesChanged]在列表更改后才会被触发。
     */
    @MainThread
    fun addLoadStatesListener(listener: LoadStatesListener) {
        assertMainThread()
        if (listeners == null) {
            listeners = arrayListOf()
        }
        if (!listeners!!.contains(listener)) {
            listeners!!.add(listener)
        }
    }

    /**
     * 移除加载状态集合已更改的监听
     */
    @MainThread
    fun removeLoadStatesListener(listener: LoadStatesListener) {
        assertMainThread()
        listeners?.remove(listener)
    }

    /**
     * 末尾加载，该函数会对加载状态做判断，避免冗余请求
     *
     * **注意**：该函数由内部[AppendTrigger]调用，不对外开放
     */
    @MainThread
    internal fun append() {
        assertMainThread()
        mediator?.append()
    }

    /**
     * 收集[PagingData.flow]
     *
     * 使用[mainDispatcher]确保在主线程中设置[mediator]和收集事件流。
     *
     * ### 初始化或者刷新流程
     * 先调用[launchResumeJob]执行恢复任务，再收集[PagingData.flow]，
     * 此时下游正在执行恢复任务，上游正在获取分页数据，这是一种高效的处理方式。
     *
     * ### 重新收集[PagingData.flow]流程
     * 先调用[launchResumeJob]执行恢复任务，再重新收集[PagingData.flow]。
     */
    override suspend fun emit(
        value: PagingData<T>
    ) = withContext(mainDispatcher.immediate) {
        mediator = value.mediator
        // 此处resumeJob若使用var局部变量，则编译后resumeJob会是ObjectRef对象，
        // 收集事件流期间，value.flow传入的FlowCollector会一直持有ObjectRef对象引用，
        // resumeJob执行完之后置空，ObjectRef对象内的Job可以被GC，但ObjectRef对象本身无法被GC。
        resumeJob = launchResumeJob(value.mediator)
        resumeJob?.invokeOnCompletion { resumeJob = null }
        value.flow.collect {
            // 等待恢复任务执行完毕，才处理后续的分页事件
            resumeJob?.join()
            handleEvent(it)
        }
    }

    @MainThread
    private fun CoroutineScope.launchResumeJob(mediator: PagingMediator): Job? {
        val listMediator = mediator.asListMediator<T>()
        val resumeEvent: PagingEvent<T> = when {
            listMediator != null && listMediator.updateVersion > updateVersion -> {
                listMediator.resumeListStateEvent()
            }
            mediator.loadStates != loadStates -> mediator.resumeLoadStateEvent()
            else -> return null
        }
        // 无需调度时，协程的启动恢复不是同步执行，而是通过事件循环进行恢复，
        // 因此启动模式设为UNDISPATCHED，确保在收集事件流之前执行恢复任务。
        return launch(start = CoroutineStart.UNDISPATCHED) {
            handleEvent(resumeEvent)
        }
    }

    @MainThread
    private suspend fun handleEvent(event: PagingEvent<T>) {
        val rv = ensureRecyclerView()
        val loadType = event.loadType
        val refresh = event.loadStates.refresh
        when {
            loadType == null && refresh.isIncomplete -> {
                // 刷新加载开始之前，检查是否需要滚动到首个item
                checkRefreshScrollToFirst()
            }
            loadType == LoadType.REFRESH && refresh.isComplete -> {
                // 刷新加载完成之后，检查是否需要延迟执行后续逻辑
                checkRefreshCompleteDelay()
            }
        }

        val op = when (event) {
            is PagingEvent.ListStateUpdate -> event.op
            is PagingEvent.LoadDataSuccess -> event.toUpdateOp()
            is PagingEvent.LoadStateUpdate -> null
        }

        val beforeIsEmpty = !adapter.hasDisplayItem
        if (op != null) {
            adapter.awaitUpdateList(op, dispatch = false)
            // 更新列表完成后才保存版本号
            updateVersion = mediator?.asListMediator<T>()?.updateVersion ?: 0
            if (event.loadType == LoadType.APPEND) {
                // 确保ItemDecoration能正常显示
                adapter.invalidateItemDecorations()
            }
        }

        when {
            loadStates == event.loadStates -> return
            event is PagingEvent.LoadDataSuccess && beforeIsEmpty && rv.isStaggered -> {
                // 若此时将加载状态同步分发给listener，则listener可能会调用notifyItemRemoved()，
                // 那么在下一帧的绘制流程中，因为瀑布流布局的spanIndex变得不准确，
                // 导致ItemDecoration计算出错误的间距，例如item分割线显示异常。
                // 注意：协程主线程调度器发送的是异步消息，因此这里不能使用yield()，
                // 而是通过View发送同步消息，确保在下一帧解除同步屏障后才分发加载状态。
                rv.awaitPost()
            }
            rv.isComputingLayout -> {
                // 此时可能是onBindViewHolder()触发了末尾加载，
                // 上游加载下一页之前，会发送加载中事件，整个过程在一个消息中完成。
                // 若此时将加载状态同步分发给listener，则listener可能会调用notifyXXX()函数，
                // 导致RecyclerView内部抛出异常，因此在下一个异步消息中分发加载状态。
                yield()
            }
        }
        setLoadStates(event.loadStates)
    }

    @MainThread
    private fun PagingListMediator<T>.resumeListStateEvent(): PagingEvent.ListStateUpdate<T> {
        val op = UpdateOp.SubmitList(currentList)
        return PagingEvent.ListStateUpdate(op, loadStates)
    }

    @MainThread
    private fun PagingMediator.resumeLoadStateEvent(): PagingEvent.LoadStateUpdate<T> {
        return PagingEvent.LoadStateUpdate(loadType = null, loadStates)
    }

    @MainThread
    private fun PagingEvent.LoadDataSuccess<T>.toUpdateOp(): UpdateOp<T> {
        return when (loadType) {
            LoadType.REFRESH -> UpdateOp.SubmitList(data)
            LoadType.APPEND -> UpdateOp.AddItems(adapter.currentList.size, data)
        }
    }

    @MainThread
    private fun ensureRecyclerView(): RecyclerView {
        return adapter.recyclerView
                ?: throw CancellationException("ListAdapter已从RecyclerView上分离。")
    }

    @MainThread
    private suspend fun checkRefreshScrollToFirst() {
        val rv = ensureRecyclerView()
        if (!refreshScrollEnabled
                || rv.childCount == 0
                || rv.isFirstItemCompletelyVisible) {
            return
        }
        rv.scrollToPosition(0)
        // 在下一个同步消息中执行后续逻辑，确保滚动不受影响
        rv.awaitPost()
    }

    @MainThread
    private suspend fun checkRefreshCompleteDelay() {
        val timeMillis = refreshCompleteWhen - SystemClock.uptimeMillis()
        if (timeMillis > 0) {
            delay(timeMillis)
        }
    }

    @MainThread
    private fun setLoadStates(newStates: LoadStates) {
        if (loadStates == newStates) {
            return
        }
        val previous = loadStates
        loadStates = newStates
        listeners?.reverseAccessEach {
            it.onLoadStatesChanged(previous, loadStates)
        }
    }

    @MainThread
    @VisibleForTesting
    internal fun setLoadState(loadType: LoadType, newState: LoadState) {
        if (getLoadState(loadType) == newState) {
            return
        }
        val previous = loadStates
        loadStates = loadStates.modifyState(loadType, newState)
        listeners?.reverseAccessEach {
            it.onLoadStatesChanged(previous, loadStates)
        }
    }

    @MainThread
    private fun getLoadState(loadType: LoadType): LoadState {
        return loadStates.getState(loadType)
    }

    private companion object {
        const val IMMEDIATE_COMPLETE = 0L
    }
}