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
import kotlinx.coroutines.flow.FlowCollector

/**
 * 分页数据收集器
 *
 * @author xcc
 * @date 2021/11/26
 */
class PagingCollector<T : Any> internal constructor(
    internal val adapter: ListAdapter<T, *>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : FlowCollector<PagingData<T>> {
    private var updateVersion = 0
    private var resumeJob: Job? = null
    private var mediator: PagingMediator<T>? = null
    private var listeners: ArrayList<LoadStatesListener>? = null
    private var refreshCompleteWhen = IMMEDIATE_COMPLETE
    private var refreshScrollEnabled = true
    private val RecyclerView.isStaggered: Boolean
        get() = layoutManager is StaggeredGridLayoutManager
    var loadStates: LoadStates = LoadStates.Incomplete
        private set

    init {
        AppendTrigger(this)
        adapter.addListExecuteListener { op ->
            mediator?.updateList(op)
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
     * 末尾加载，该函数会对加载状态做判断，避免冗余请求
     *
     * **注意**：该函数由内部[AppendTrigger]调用，不对外开放
     */
    internal fun append() {
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
        // 此处resumeJob不使用局部变量，
        // 避免收集事件流期间，一直持有resumeJob对象。
        resumeJob = launchResumeJob(value.mediator)
        resumeJob?.invokeOnCompletion { resumeJob = null }
        value.flow.collect {
            // 等待恢复任务执行完毕，才处理后续的分页事件
            resumeJob?.join()
            handleEvent(it)
        }
    }

    @MainThread
    private fun CoroutineScope.launchResumeJob(mediator: PagingMediator<T>): Job? {
        val resumeEvent: PagingEvent<T> = when {
            mediator.updateVersion > updateVersion -> PagingEvent.ListUpdate(
                loadType = null,
                loadStates = mediator.loadStates,
                op = UpdateOp.SubmitList(mediator.currentList)
            )
            mediator.loadStates != loadStates -> PagingEvent.LoadUpdate(
                loadType = null,
                loadStates = mediator.loadStates
            )
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
        val rv = adapter.recyclerView
                ?: throw CancellationException("ListAdapter已从RecyclerView上分离。")
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

        val beforeIsEmpty = !adapter.hasDisplayItem
        if (event is PagingEvent.ListUpdate) {
            adapter.awaitUpdateList(event.op, dispatch = false)
            updateVersion = mediator?.updateVersion ?: 0
            if (event.loadType == LoadType.APPEND) {
                // 确保ItemDecoration能正常显示
                adapter.invalidateItemDecorations()
            }
        }

        when {
            loadStates == event.loadStates -> return
            event is PagingEvent.ListUpdate && beforeIsEmpty && rv.isStaggered -> {
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
    private suspend fun checkRefreshScrollToFirst() {
        val rv = adapter.recyclerView
                ?: throw CancellationException("ListAdapter已从RecyclerView上分离。")
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

    private companion object {
        const val IMMEDIATE_COMPLETE = 0L
    }
}