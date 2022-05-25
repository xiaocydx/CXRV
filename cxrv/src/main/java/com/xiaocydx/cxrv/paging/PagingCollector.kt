package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.extension.*
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.UpdateOp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

private const val PAGING_COLLECTOR_KEY = "com.xiaocydx.cxrv.paging.PAGING_COLLECTOR_KEY"

/**
 * 分页数据收集器
 *
 * ### `Flow<PagingData>`
 * [PagingCollector.emit]负责收集指定流的[PagingData]。
 *
 * ### 加载状态
 * 1. [PagingCollector.loadStates]返回当前加载状态集合。
 * 2. [PagingCollector.addLoadStatesListener]添加加载状态集合已更改的监听。
 * 3. [PagingCollector.doOnLoadStatesChanged]添加加载状态集合已更改的处理程序。
 *
 * ### 分页操作
 * 1. [PagingCollector.refresh]刷新加载，获取新的[PagingData]。
 * 2. [PagingCollector.retry]重新加载，会对加载状态做判断，避免冗余请求。
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
@Deprecated(
    message = "其它相同形式的扩展函数，导致该函数调用体验不好，因此废弃",
    replaceWith = ReplaceWith(
        expression = "flow.collect(adapter)",
        imports = ["com.xiaocydx.cxrv.extension.collect"]
    )
)
suspend fun <T : Any> ListAdapter<T, *>.emitAll(
    flow: Flow<PagingData<T>>
) = pagingCollector.emitAll(flow)

/**
 * 开始处理分页事件的监听
 */
fun interface HandleEventListener<T : Any> {
    /**
     * [PagingCollector]接收到[event]后，[handleEvent]最先被调用，
     * 若`event.loadType == null`，则表示是列表状态更新事件或者视图控制器恢复事件。
     *
     * 对于刷新流程，可以在刷新开始时将列表滚动到首个item，并在滚动完成后才继续处理事件，
     * 此时下游在等待列表滚动完成，上游在获取分页数据，这是一种高效的处理方式。
     */
    suspend fun handleEvent(rv: RecyclerView, event: PagingEvent<T>)
}

/**
 * 分页数据收集器，负责收集指定流的[PagingData]
 */
class PagingCollector<T : Any> internal constructor(
    private val adapter: ListAdapter<T, *>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : FlowCollector<PagingData<T>> {
    private var version = 0
    private var mediator: PagingMediator? = null
    private var loadStatesListeners: ArrayList<LoadStatesListener>? = null
    private var handleEventListeners: ArrayList<HandleEventListener<in T>>? = null
    var loadStates: LoadStates = LoadStates.Incomplete
        private set

    init {
        assertMainThread()
        AppendTrigger(adapter, this)
        addHandleEventListener(RefreshStartScrollToFirst())
        adapter.addListExecuteListener { op ->
            mediator?.asListMediator<T>()?.updateList(op)
            version = mediator?.asListMediator<T>()?.version ?: 0
        }
    }

    /**
     * 刷新加载，获取新的[PagingData]
     */
    @MainThread
    fun refresh() {
        assertMainThread()
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
     * 末尾加载，该函数会对加载状态做判断，避免冗余请求
     *
     * 该函数由[AppendTrigger]调用，暂时不对外开放
     */
    @MainThread
    internal fun append() {
        assertMainThread()
        mediator?.append()
    }

    /**
     * 添加开始处理分页事件的监听
     *
     * [HandleEventListener.handleEvent]中可以调用[removeHandleEventListener]
     */
    @MainThread
    fun addHandleEventListener(listener: HandleEventListener<in T>) {
        assertMainThread()
        if (handleEventListeners == null) {
            handleEventListeners = arrayListOf()
        }
        if (!handleEventListeners!!.contains(listener)) {
            handleEventListeners!!.add(listener)
        }
    }

    /**
     * 移除开始处理分页事件的监听
     */
    @MainThread
    fun removeHandleEventListener(listener: HandleEventListener<in T>) {
        assertMainThread()
        handleEventListeners?.remove(listener)
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
        if (loadStatesListeners == null) {
            loadStatesListeners = arrayListOf()
        }
        if (!loadStatesListeners!!.contains(listener)) {
            loadStatesListeners!!.add(listener)
        }
    }

    /**
     * 移除加载状态集合已更改的监听
     */
    @MainThread
    fun removeLoadStatesListener(listener: LoadStatesListener) {
        assertMainThread()
        loadStatesListeners?.remove(listener)
    }

    /**
     * 处理[PagingData]
     *
     * * 确保在主线程中设置[mediator]和收集[PagingData.flow]。
     * * 恢复流程先执行恢复任务，再重新收集[PagingData.flow]。
     */
    override suspend fun emit(
        value: PagingData<T>
    ) = withContext(mainDispatcher.immediate) {
        mediator = value.mediator
        value.flow.collect(::handleEvent)
    }

    @MainThread
    private suspend fun handleEvent(event: PagingEvent<T>) {
        val rv = requireNotNull(adapter.recyclerView) {
            "ListAdapter已从RecyclerView上分离"
        }
        handleEventListeners?.reverseAccessEach { it.handleEvent(rv, event) }

        val op: UpdateOp<T>? = when (event) {
            is PagingEvent.ListStateUpdate -> event.op
            is PagingEvent.LoadDataSuccess -> event.toUpdateOp()
            is PagingEvent.LoadStateUpdate -> null
        }

        val isPreviousEmpty = !adapter.hasDisplayItem
        val listMediator = mediator?.asListMediator<T>()
        val newVersion = event.getVersionOrZero()

        // 若mediator的类型是ListMediator，则version < newVersion时才更新列表
        if (op != null && (listMediator == null || version < newVersion)) {
            adapter.awaitUpdateList(op, dispatch = false)
            // 更新列表完成后才保存版本号
            version = newVersion
        }

        when {
            loadStates == event.loadStates -> return
            event is PagingEvent.LoadDataSuccess
                    && isPreviousEmpty
                    && rv.isLayoutRequested
                    && rv.layoutManager is StaggeredGridLayoutManager -> {
                // 若此时将加载状态同步分发给listener，则listener可能会调用notifyItemRemoved()，
                // 那么在下一帧rv布局流程中，因为瀑布流布局的spanIndex变得不准确，
                // 导致ItemDecoration计算出错误的间距，例如item分割线显示异常。
                // 注意：协程主线程调度器发送的是异步消息，因此这里不能使用yield()，
                // 而是在下一帧rv布局完成后，才将加载状态分发给listener。
                rv.awaitPreDraw()
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
    private fun PagingEvent.LoadDataSuccess<T>.toUpdateOp(): UpdateOp<T> {
        return when (loadType) {
            LoadType.REFRESH -> UpdateOp.SubmitList(data)
            LoadType.APPEND -> UpdateOp.AddItems(adapter.currentList.size, data)
        }
    }

    @MainThread
    private fun setLoadStates(newStates: LoadStates) {
        if (loadStates == newStates) {
            return
        }
        val previous = loadStates
        loadStates = newStates
        loadStatesListeners?.reverseAccessEach {
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
        loadStatesListeners?.reverseAccessEach {
            it.onLoadStatesChanged(previous, loadStates)
        }
    }

    @MainThread
    private fun getLoadState(loadType: LoadType): LoadState {
        return loadStates.getState(loadType)
    }

    private class RefreshStartScrollToFirst : HandleEventListener<Any> {

        override suspend fun handleEvent(rv: RecyclerView, event: PagingEvent<Any>) {
            val loadType = event.loadType
            val loadState = event.loadStates.refresh
            if (loadType != LoadType.REFRESH || !loadState.isLoading) {
                return
            }
            if (rv.childCount == 0 || rv.isFirstItemCompletelyVisible) {
                return
            }
            rv.scrollToPosition(0)
            // 等待下一帧rv布局完成，确保滚动不受影响
            rv.awaitPreDraw()
        }
    }
}