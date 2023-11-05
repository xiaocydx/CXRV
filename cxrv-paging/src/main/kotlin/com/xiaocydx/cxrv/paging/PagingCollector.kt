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
import androidx.recyclerview.widget.optimizeNextFrameScroll
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.awaitNextLayout
import com.xiaocydx.cxrv.internal.log
import com.xiaocydx.cxrv.internal.trace
import com.xiaocydx.cxrv.itemvisible.isFirstItemCompletelyVisible
import com.xiaocydx.cxrv.list.InlineList
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.UpdateOp
import com.xiaocydx.cxrv.list.UpdateResult
import com.xiaocydx.cxrv.list.accessEach
import com.xiaocydx.cxrv.list.reverseAccessEach
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
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
     * 当[PagingCollector]收集到[event]时，最先分发调用[handleEvent]，
     * 若`event.loadType == null`，则是[PagingEvent.ListStateUpdate]，
     * 此时[PagingCollector]会同步最新的列表状态和加载状态。
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
        addHandleEventListener(PostponeHandleEventForDoFrameMessage())
        adapter.addListExecuteListener { op ->
            // 先得到期望的version，用于拦截同步发送的分页事件
            version++
            mediator?.getListMediator<T>()?.updateList(op)
            // 再得到实际的version，确保不会因为失败而增加version
            version = mediator?.getListMediator<T>()?.version ?: 0
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
     * 2. 重新收集[PagingData.flow]，先处理恢复事件，再处理常规事件。
     */
    override suspend fun emit(
        value: PagingData<T>
    ) = withContext(mainDispatcher.immediate) {
        if (mediator !== value.mediator) {
            setMediator(value.mediator)
            setRefreshScroll(value.mediator)
            setAppendTrigger(value.mediator)
        }

        // 使用Channel的原因：
        // 当op是SubmitList时，adapter会清除更新队列，并取消差异计算，
        // 正在等待的result会结束挂起，保存在Channel的result不会挂起，
        // for循环快速处理完Channel的result后，挂起等待新的result完成。
        /*
           listState.submitList(newList1) // op1
           listState.addItem(0, item1)    // op2
           listState.addItem(0, item2)    // op3
           listState.submitList(newList2) // 清除op2和op3，取消op1
         */
        val channel = Channel<PagingEventResult>(UNLIMITED)
        launch {
            value.flow.collect { event ->
                val rv = requireNotNull(adapter.recyclerView) { "ListAdapter未添加到RecyclerView" }
                handleEventListeners.reverseAccessEach { it.handleEvent(rv, event) }
                val op: UpdateOp<T>? = when (event) {
                    is PagingEvent.ListStateUpdate -> event.op
                    is PagingEvent.LoadDataSuccess -> event.toUpdateOp()
                    is PagingEvent.LoadStateUpdate -> null
                }
                // 若mediator的类型是ListMediator，则version < newVersion才更新列表
                val listMediator = mediator?.getListMediator<T>()
                val newVersion = event.getVersionOrZero()
                var result: UpdateResult? = null
                if (op != null && (listMediator == null || version < newVersion)) {
                    result = adapter.updateList(op, dispatch = false)
                }
                channel.send(PagingEventResult(newVersion, event.loadStates, result))
            }
            channel.close()
        }

        for (result in channel) {
            if (!result.await()) continue
            val newVersion = result.newVersion
            if (version < newVersion) version = newVersion
            if (loadStates == result.loadStates) continue
            trace(TRACE_DISPATCH_LOAD_STATES_TAG) { setLoadStates(result.loadStates) }
        }
    }

    @MainThread
    private fun setMediator(mediator: PagingMediator) {
        val previousVersion = version
        val previousListMediator = this.mediator?.getListMediator<T>()
        val currentListMediator = mediator.getListMediator<T>()
        if (previousListMediator == null || currentListMediator == null
                || !previousListMediator.isSameList(currentListMediator)) {
            version = 0
        }
        this.mediator = mediator
        log {
            """setMediator {
                |   mediator = $mediator
                |   previousListMediator = $previousListMediator
                |   currentListMediator = $currentListMediator
                |   previousVersion = $previousVersion
                |   currentVersion = $version
                |}
            """.trimMargin()
        }
    }

    @MainThread
    private fun setRefreshScroll(mediator: PagingMediator) {
        val listener: PostponeHandleEventForRefreshScroll? = run {
            handleEventListeners.accessEach {
                if (it is PostponeHandleEventForRefreshScroll) return@run it
            }
            null
        }
        if (listener != null && !mediator.refreshStartScrollToFirst) {
            removeHandleEventListener(listener)
        } else if (listener == null && mediator.refreshStartScrollToFirst) {
            addHandleEventListener(PostponeHandleEventForRefreshScroll())
        }
    }

    @MainThread
    private fun setAppendTrigger(mediator: PagingMediator) {
        val config = AppendTrigger.Config(
            failureAutToRetry = mediator.appendFailureAutToRetry,
            prefetchEnabled = mediator.appendPrefetch !is PagingPrefetch.None,
            prefetchItemCount = (mediator.appendPrefetch as? PagingPrefetch.ItemCount)?.value ?: 0
        )
        val previousTrigger = appendTrigger
        if (previousTrigger == null || previousTrigger.config != config) {
            appendTrigger?.detach()
            appendTrigger = AppendTrigger(config, adapter, this)
            appendTrigger?.attach()
        }
        log {
            """setAppendTrigger {
                |   previousConfig = ${previousTrigger?.config}
                |   currentConfig = ${appendTrigger?.config}
                |}
            """.trimMargin()
        }
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

    private class PostponeHandleEventForRefreshScroll : HandleEventListener<Any> {

        override suspend fun handleEvent(rv: RecyclerView, event: PagingEvent<Any>) {
            val loadType = event.loadType
            val loadState = event.loadStates.refresh
            if (!(loadType == null || loadType == LoadType.REFRESH)
                    || !(loadState.isIncomplete || loadState.isLoading)
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
                rv.optimizeNextFrameScroll()
                // 等待下一帧rv布局完成，确保滚动不受影响
                rv.takeIf { it.itemAnimator != null }?.awaitNextLayout()
            }
        }
    }

    private inner class PostponeHandleEventForDoFrameMessage : HandleEventListener<Any> {

        override suspend fun handleEvent(rv: RecyclerView, event: PagingEvent<Any>) {
            if (loadStates == event.loadStates) return
            val rv = adapter.recyclerView
            if (rv != null && (rv.isComputingLayout
                            || rv.scrollState != SCROLL_STATE_IDLE
                            || rv.mDispatchScrollCounter > 0)) {
                // DoFrame消息执行onBindViewHolder()或者滚动过程可能触发末尾加载，
                // 上游加载下一页之前，会发送加载中事件，整个过程在一个消息中完成，
                // 此时对listeners分发加载状态，则会因为listeners调用notifyXXX()，
                // 导致RecyclerView内部逻辑断言为异常情况。
                yield()
            }
        }
    }

    private class PagingEventResult(
        val newVersion: Int,
        val loadStates: LoadStates,
        private val result: UpdateResult?
    ) {
        suspend fun await() = result == null || result.await()
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