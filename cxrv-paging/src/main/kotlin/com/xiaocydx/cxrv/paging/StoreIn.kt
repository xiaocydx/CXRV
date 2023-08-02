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
import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.UpdateOp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * ### 函数作用
 * 1. 将[PagingData.flow]发射的事件的列表数据保存到[state]中。
 * 2. [state]和视图控制器建立基于[ListOwner]的双向通信。
 * 3. 将`Flow<PagingData<T>>`转换为热流，
 * 热流可以被重复收集，但同时只能被一个收集器收集，
 * 热流被首次收集时，才会开始收集它的上游，直到[scope]被取消。
 *
 * ### 调用顺序
 * 不允许调用[storeIn]之后，还调用[multiple]或[flowMap]转换[PagingData.flow]，
 * [state]和视图控制器会建立双向通信，需要确保[state]跟视图控制器的数据一致。
 *
 * 在ViewModel中使用[storeIn]：
 * ```
 * class FooViewModel : ViewModel(private val repository: FooRepository) {
 *     private val state = ListState<Foo>()
 *     val flow = repository.flow
 *         .flowMap { eventFlow ->
 *             eventFlow.itemMap { loadType, item -> ... }
 *         }
 *         .storeIn(state, viewModelScope)
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(
    state: ListState<T>,
    scope: CoroutineScope
): Flow<PagingData<T>> {
    if (this is PagingDataCancellableFlow) return this
    var previous: PagingEventCancellableFlow<T>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        previous?.cancel()
        val mediator = PagingListMediator(data, state)
        val flow = PagingEventCancellableFlow(scope, mediator.flow, mediator)
        previous = flow
        PagingData(flow, mediator)
    }
    return PagingDataCancellableFlow(scope, upstream)
}

private class PagingDataCancellableFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingData<T>>
) : CancellableFlow<PagingData<T>>(scope, upstream) {
    private var state: PagingData<T>? = null

    override fun onActive(): PagingData<T>? = state

    override fun onReceive(value: PagingData<T>) {
        state = value
    }
}

private class PagingEventCancellableFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingEvent<T>>,
    private val mediator: PagingListMediator<T>
) : CancellableFlow<PagingEvent<T>>(scope, upstream) {

    override fun onActive(): PagingEvent<T>? = mediator.run {
        if (version == 0
                && loadStates.refresh.isIncomplete
                && loadStates.append.isIncomplete) {
            return@run null
        }
        val op: UpdateOp<T> = UpdateOp.SubmitList(currentList)
        PagingEvent.ListStateUpdate(op, loadStates).fusion(version)
    }
}

/**
 * 可取消的接收流
 *
 * [CancellableFlow]可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到主动调用`cancel()`或者`scope`被取消。
 */
internal open class CancellableFlow<T>(
    private val scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : Flow<T> {
    private var isCollected = false
    @Volatile private var collectJob: Job? = null
    private val channel: Channel<T> = Channel(RENDEZVOUS)

    override suspend fun collect(collector: FlowCollector<T>) {
        val active = withMainDispatcher {
            check(!isCollected) { "CancellableFlow只能被一个收集器收集" }
            isCollected = true
            launchCollectJob()
            onActive()
        }
        try {
            if (active != null) {
                collector.emit(active)
            }
            for (value in channel) {
                collector.emit(value)
            }
        } finally {
            // 当前协程可能已被取消，用NonCancellable确保执行
            withMainDispatcher(NonCancellable) {
                isCollected = false
                onInactive()
            }
        }
    }

    @MainThread
    private fun launchCollectJob() {
        if (collectJob != null) return
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName + mainDispatcher.immediate) {
            upstream.collect {
                onReceive(it)
                if (!isCollected) {
                    return@collect
                }
                try {
                    channel.send(it)
                } catch (ignored: CancellationException) {
                } catch (ignored: ClosedSendChannelException) {
                }
            }
        }
        collectJob!!.invokeOnCompletion {
            // MainThread
            collectJob = null
            channel.close()
        }
    }

    private suspend inline fun <R> withMainDispatcher(
        context: CoroutineContext = EmptyCoroutineContext,
        crossinline block: suspend () -> R
    ): R {
        val dispatcher = mainDispatcher.immediate
        return if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            withContext(dispatcher + context) { block() }
        } else {
            block()
        }
    }

    @MainThread
    protected open fun onActive(): T? = null

    @MainThread
    protected open fun onReceive(value: T) = Unit

    @MainThread
    protected open fun onInactive() = Unit

    suspend fun cancel() {
        collectJob?.cancelAndJoin()
    }
}