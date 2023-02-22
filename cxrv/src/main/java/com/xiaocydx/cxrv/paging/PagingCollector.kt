package com.xiaocydx.cxrv.paging

import android.os.SystemClock
import android.view.Choreographer
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.xiaocydx.cxrv.internal.*
import com.xiaocydx.cxrv.itemvisible.isFirstItemCompletelyVisible
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.UpdateOp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.onEach
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
 * `Flow<PagingData<T>>`的值发射给[pagingCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.onEach { adapter.pagingCollector.emit(it) }
 *
 * // 简化上面的写法
 * flow.onEach(adapter.pagingCollector)
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.onEach(
    collector: PagingCollector<T>
): Flow<PagingData<T>> = onEach(collector::emit)

/**
 * `Flow<PagingData<T>>`的值发射给[pagingCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.onEach { adapter.pagingCollector.emit(it) }
 *
 * // 简化上面的写法
 * flow.onEach(adapter)
 * ```
 */
@Deprecated(
    message = "虽然简化了代码，但是降低了可读性",
    replaceWith = ReplaceWith(expression = "onEach(adapter.pagingCollector)")
)
fun <T : Any> Flow<PagingData<T>>.onEach(
    adapter: ListAdapter<T, *>
): Flow<PagingData<T>> = onEach(adapter.pagingCollector::emit)

/**
 * `Flow<PagingData<T>>`的值发射给[pagingCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.collect { value ->
 *     adapter.pagingCollector.emit(value)
 * }
 *
 * // 简化上面的写法
 * flow.collect(adapter)
 * ```
 */
@Deprecated(
    message = "虽然简化了代码，但是降低了可读性",
    replaceWith = ReplaceWith(expression = "collect(adapter.pagingCollector)")
)
suspend fun <T : Any> Flow<PagingData<T>>.collect(
    adapter: ListAdapter<T, *>
): Unit = collect(adapter.pagingCollector)

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
    private var appendTrigger: AppendTrigger? = null
    private var loadStatesListeners: ArrayList<LoadStatesListener>? = null
    private var handleEventListeners: ArrayList<HandleEventListener<in T>>? = null
    var loadStates: LoadStates = LoadStates.Incomplete
        private set

    init {
        assertMainThread()
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
     * 1. [LoadStatesListener.onLoadStatesChanged]在列表更改后才会被触发。
     * 2. [LoadStatesListener.onLoadStatesChanged]在下一帧执行布局流程前被触发。
     * 3. [LoadStatesListener.onLoadStatesChanged]中可以调用[removeLoadStatesListener]。
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
     * 1. 确保在主线程中设置[mediator]和收集[PagingData.flow]。
     * 2. 恢复流程先执行恢复任务，再重新收集[PagingData.flow]。
     */
    override suspend fun emit(
        value: PagingData<T>
    ) = withContext(mainDispatcher.immediate) {
        if (mediator !== value.mediator) {
            version = 0
            mediator = value.mediator
            setAppendTrigger(value.mediator)
        }
        value.flow.collect(::handleEvent)
    }

    @MainThread
    private fun setAppendTrigger(mediator: PagingMediator) {
        val prefetch = mediator.appendPrefetch
        val previousTrigger = appendTrigger
        val prefetchEnabled = prefetch !is PagingPrefetch.None
        val prefetchItemCount = (prefetch as? PagingPrefetch.ItemCount)?.value ?: 0
        if (previousTrigger != null
                && previousTrigger.prefetchEnabled == prefetchEnabled
                && previousTrigger.prefetchItemCount == prefetchItemCount) {
            return
        }
        appendTrigger?.detach()
        appendTrigger = AppendTrigger(prefetchEnabled, prefetchItemCount, adapter, this)
        appendTrigger?.attach()
    }

    @MainThread
    private suspend fun handleEvent(event: PagingEvent<T>) {
        val rv = requireNotNull(adapter.recyclerView) { "ListAdapter已从RecyclerView上分离" }
        handleEventListeners?.reverseAccessEach { it.handleEvent(rv, event) }

        val op: UpdateOp<T>? = when (event) {
            is PagingEvent.ListStateUpdate -> event.op
            is PagingEvent.LoadDataSuccess -> event.toUpdateOp()
            is PagingEvent.LoadStateUpdate -> null
        }

        val listMediator = mediator?.asListMediator<T>()
        val newVersion = event.getVersionOrZero()

        var latestFrameVsyncMs = -1L
        // 若mediator的类型是ListMediator，则version < newVersion时才更新列表
        if (op != null && (listMediator == null || version < newVersion)) {
            latestFrameVsyncMs = rv.drawingTime.coerceAtLeast(0L)
            adapter.awaitUpdateList(op, dispatch = false)
            // 更新列表完成后才保存版本号
            version = newVersion
        }

        if (loadStates == event.loadStates) return
        // 执行onBindViewHolder()或者滚动过程中可能触发末尾加载，
        // 上游加载下一页之前，会发送加载中事件，整个过程在一个消息中完成，
        // 此时对listeners分发加载状态，则会因为listeners调用notifyXXX()，
        // 导致RecyclerView内部逻辑断言为异常情况。
        when {
            !rv.isComputingLayout
                    && rv.scrollState == SCROLL_STATE_IDLE
                    && rv.dispatchScrollCounter <= 0 -> {
                // 以下条件满足其中之一则为异常情况：
                // 1. rv.isComputingLayout == true
                // 2. rv.scrollState != SCROLL_STATE_IDLE
                // 3. rv.dispatchScrollCounter > 0
                // 当这些条件都不满足时，才能直接分发加载状态
            }
            latestFrameVsyncMs == -1L -> {
                // 快路径，在下一个异步消息中分发加载状态
                yield()
            }
            else -> {
                // 假设下一帧布局流程在Animation回调下执行，添加负延时的Animation回调进行插队，
                // 确保下一帧执行布局流程之前，先分发加载状态，让listeners完成对加载状态的处理。
                // 异步消息无法确保这种插队行为，因为异步消息可能被doFrame消息按vsync时间插队。
                val beforeNextRvLayoutDelay = -(SystemClock.uptimeMillis() - latestFrameVsyncMs)
                Choreographer.getInstance().awaitFrame(beforeNextRvLayoutDelay)
            }
        }

        trace(TRACE_DISPATCH_LOAD_STATES_TAG) { setLoadStates(event.loadStates) }
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
        if (loadStates == newStates) return
        val previous = loadStates
        loadStates = newStates
        loadStatesListeners?.reverseAccessEach {
            it.onLoadStatesChanged(previous, loadStates)
        }
    }

    @MainThread
    @VisibleForTesting
    internal fun setLoadState(loadType: LoadType, newState: LoadState) {
        if (loadStates.getState(loadType) == newState) return
        val previous = loadStates
        loadStates = loadStates.modifyState(loadType, newState)
        loadStatesListeners?.reverseAccessEach {
            it.onLoadStatesChanged(previous, loadStates)
        }
    }

    private class RefreshStartScrollToFirst : HandleEventListener<Any> {

        override suspend fun handleEvent(rv: RecyclerView, event: PagingEvent<Any>) {
            val loadType = event.loadType
            val loadState = event.loadStates.refresh
            if (loadType != LoadType.REFRESH || !loadState.isLoading
                    || rv.childCount == 0 || rv.isFirstItemCompletelyVisible) {
                return
            }
            rv.scrollToPosition(0)
            // 等待下一帧rv布局完成，确保滚动不受影响
            rv.awaitNextLayout()
        }
    }

    private companion object {
        const val TRACE_DISPATCH_LOAD_STATES_TAG = "PagingCollector Dispatch LoadStates"
        private val dispatchScrollCounterField = runCatching {
            RecyclerView::class.java.getDeclaredField("mDispatchScrollCounter")
        }.onSuccess { it.isAccessible = true }.getOrNull()

        val RecyclerView.dispatchScrollCounter: Int
            get() = (dispatchScrollCounterField?.get(this) as? Int) ?: 0
    }
}