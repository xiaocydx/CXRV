/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.awaitNextLayout
import com.xiaocydx.cxrv.internal.trace
import com.xiaocydx.cxrv.itemvisible.isFirstItemCompletelyVisible
import com.xiaocydx.cxrv.list.InlineList
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.UpdateOp
import com.xiaocydx.cxrv.list.reverseAccessEach
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
    private var loadStatesListeners = InlineList<LoadStatesListener>()
    private var handleEventListeners = InlineList<HandleEventListener<in T>>()
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
        handleEventListeners += listener
    }

    /**
     * 移除开始处理分页事件的监听
     */
    @MainThread
    fun removeHandleEventListener(listener: HandleEventListener<in T>) {
        assertMainThread()
        handleEventListeners -= listener
    }

    /**
     * 添加加载状态集合已更改的监听
     *
     * 1. [LoadStatesListener.onLoadStatesChanged]在列表更改后才会被触发。
     * 2. [LoadStatesListener.onLoadStatesChanged]中可以调用[removeLoadStatesListener]。
     */
    @MainThread
    fun addLoadStatesListener(listener: LoadStatesListener) {
        assertMainThread()
        loadStatesListeners += listener
    }

    /**
     * 移除加载状态集合已更改的监听
     */
    @MainThread
    fun removeLoadStatesListener(listener: LoadStatesListener) {
        assertMainThread()
        loadStatesListeners -= listener
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
        handleEventListeners.reverseAccessEach { it.handleEvent(rv, event) }

        val op: UpdateOp<T>? = when (event) {
            is PagingEvent.ListStateUpdate -> event.op
            is PagingEvent.LoadDataSuccess -> event.toUpdateOp()
            is PagingEvent.LoadStateUpdate -> null
        }

        val listMediator = mediator?.asListMediator<T>()
        val newVersion = event.getVersionOrZero()

        // 若mediator的类型是ListMediator，则version < newVersion时才更新列表
        if (op != null && (listMediator == null || version < newVersion)) {
            adapter.updateList(op, dispatch = false).await()
            // 更新列表完成后才保存版本号
            version = newVersion
        }

        if (loadStates == event.loadStates) return

        if (rv.isComputingLayout
                || rv.scrollState != SCROLL_STATE_IDLE
                || rv.mDispatchScrollCounter > 0) {
            // 执行onBindViewHolder()或者滚动过程中可能触发末尾加载，
            // 上游加载下一页之前，会发送加载中事件，整个过程在一个消息中完成，
            // 此时对listeners分发加载状态，则会因为listeners调用notifyXXX()，
            // 导致RecyclerView内部逻辑断言为异常情况。
            yield()
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
        loadStatesListeners.reverseAccessEach {
            it.onLoadStatesChanged(previous, loadStates)
        }
    }

    @MainThread
    @VisibleForTesting
    internal fun setLoadState(loadType: LoadType, newState: LoadState) {
        if (loadStates.getState(loadType) == newState) return
        val previous = loadStates
        loadStates = loadStates.modifyState(loadType, newState)
        loadStatesListeners.reverseAccessEach {
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
            val parent = rv.parent
            if (parent is ViewPager2) {
                parent.setCurrentItem(0, false)
            } else {
                rv.scrollToPosition(0)
            }
            if (rv.isLayoutRequested) {
                // 等待下一帧rv布局完成，确保滚动不受影响
                rv.awaitNextLayout()
            }
        }
    }

    private companion object {
        const val TRACE_DISPATCH_LOAD_STATES_TAG = "PagingCollector Dispatch LoadStates"
        val mDispatchScrollCounterField = runCatching {
            RecyclerView::class.java.getDeclaredField("mDispatchScrollCounter")
        }.onSuccess { it.isAccessible = true }.getOrNull()

        val RecyclerView.mDispatchScrollCounter: Int
            get() = (mDispatchScrollCounterField?.get(this) as? Int) ?: 0
    }
}