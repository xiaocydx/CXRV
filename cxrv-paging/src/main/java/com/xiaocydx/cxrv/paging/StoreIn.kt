@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.UpdateOp
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
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
 * 不能在调用[storeIn]之后，再调用[flowMap]转换[PagingData.flow]，
 * 因为[state]和视图控制器会建立双向通信，所以需要确保[state]和视图控制器中的数据一致。
 *
 * 在ViewModel中使用[storeIn]：
 * ```
 * class FooViewModel : ViewModel(private val repository: FooRepository) {
 *     private val listState = ListState<Foo>()
 *     val flow = repository.flow
 *         .flowMap { eventFlow ->
 *             eventFlow.itemMap { loadType, item ->
 *                 ...
 *             }
 *         }
 *         .storeIn(listState, viewModelScope)
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(
    state: ListState<T>,
    scope: CoroutineScope
): Flow<PagingData<T>> {
    var previous: PagingEventFlow<T>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        previous?.cancel()
        val mediator = PagingListMediator(data, state)
        val flow = PagingEventFlow(scope, mediator.flow, mediator)
        previous = flow
        PagingData(flow, mediator)
    }
    return PagingDataFlow(scope, upstream)
}

/**
 * 可取消的`Flow<PagingData<T>>`
 */
private class PagingDataFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingData<T>>
) : CancellableFlow<PagingData<T>>(scope, upstream) {
    private var state: PagingData<T>? = null

    override fun onActive(): PagingData<T>? = state

    override fun onReceive(value: PagingData<T>) {
        state = value
    }
}

/**
 * 可取消的`Flow<PagingEvent<T>>`
 */
private class PagingEventFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingEvent<T>>,
    private val mediator: PagingListMediator<T>
) : CancellableFlow<PagingEvent<T>>(scope, upstream) {
    private var isFirstActive = true

    override fun onActive(): PagingEvent<T>? = when {
        isFirstActive -> {
            isFirstActive = false
            null
        }
        else -> mediator.run {
            val op: UpdateOp<T> = UpdateOp.SubmitList(currentList)
            PagingEvent.ListStateUpdate(op, loadStates).fusion(version)
        }
    }
}

/**
 * 可取消的接收流
 *
 * [CancellableFlow]可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到主动调用`cancel()`或者`scope`被取消。
 */
private open class CancellableFlow<T>(
    private val scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : Flow<T> {
    private var isCompleted = false
    private var isCollected = false
    @Volatile private var collectJob: Job? = null
    private val channel: Channel<T> = Channel(RENDEZVOUS)

    override suspend fun collect(collector: FlowCollector<T>) {
        val active = withMainDispatcher {
            require(!isCompleted) {
                "CancellableFlow已完成，不能再被收集"
            }
            require(!isCollected) {
                "CancellableFlow只能被一个收集器收集"
            }
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
            // 当前协程可能已经被取消，因此用NonCancellable替换Job
            withMainDispatcher(NonCancellable) {
                isCollected = false
                onInactive()
            }
        }
    }

    @MainThread
    private fun launchCollectJob() {
        if (collectJob != null) {
            return
        }
        collectJob = scope.launch(mainDispatcher.immediate) {
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
            isCompleted = true
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