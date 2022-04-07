package com.xiaocydx.recycler.paging

import androidx.annotation.MainThread
import com.xiaocydx.recycler.list.ListOwner
import com.xiaocydx.recycler.list.ListState
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.RENDEZVOUS
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.*

/**
 * ### 函数作用
 * 1. 将[PagingData.flow]事件流发射的列表数据保存到[state]中。
 * 2. [state]和视图控制器建立基于[ListOwner]的双向通信。
 * 3. 若[scope]不为`null`，则会构建[PagingData]的状态流作为返回结果，
 * 构建的状态流可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到[scope]被取消。
 *
 * ### 调用顺序
 * 不能在调用[storeIn]之后，再调用[transformEventFlow]转换事件流，
 * 因为[state]和视图控制器会建立双向通信，所以需要确保[state]和视图控制器中的数据一致。
 *
 * 在ViewModel中使用[storeIn]的例子：
 * ```
 * class FooViewModel: ViewModel(
 *     private val repository: FooRepository
 * ) {
 *     private val listState = ListState<Foo>()
 *     val flow = repository.flow
 *         .transformEventFlow { eventFlow ->
 *             eventFlow.transformItem { loadType, item ->
 *                 ...
 *             }
 *         }
 *         .storeIn(listState, viewModelScope)
 * }
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.storeIn(
    state: ListState<T>,
    scope: CoroutineScope? = null
): Flow<PagingData<T>> {
    val flow = map { data ->
        val mediator = PagingListMediator(data, state)
        PagingData(mediator.flow, mediator)
    }
    return if (scope != null) flow.stateIn(scope) else flow
}

/**
 * 构建[PagingData]的状态流
 *
 * 构建的状态流可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到[scope]被取消。
 */
private fun <T : Any> Flow<PagingData<T>>.stateIn(
    scope: CoroutineScope
): Flow<PagingData<T>> {
    var previous: CancellableFlow<PagingEvent<T>>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        previous?.cancel()
        val flow = CancellableFlow(scope, data.flow)
        previous = flow
        data.modifyFlow(flow)
    }
    return PagingDataStateFlow(scope, upstream)
}

/**
 * [PagingData]状态流
 */
private class PagingDataStateFlow<T : Any>(
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
    private var collectJob: Job? = null
    private val completableJob: CompletableJob = Job()
    private val channel: Channel<T> = Channel(RENDEZVOUS)

    override suspend fun collect(collector: FlowCollector<T>) {
        val active = withContext(mainDispatcher.immediate) {
            require(!isCompleted) {
                "CancellableFlow已完成，不能再被收集。"
            }
            require(!isCollected) {
                "CancellableFlow只能被一个收集器收集。"
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
            withContext(NonCancellable + mainDispatcher.immediate) {
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
            val job = coroutineContext.job
            completableJob.invokeOnCompletion {
                // AnyThread
                job.cancel()
            }

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

    @MainThread
    protected open fun onActive(): T? = null

    @MainThread
    protected open fun onReceive(value: T): Unit = Unit

    @MainThread
    protected open fun onInactive(): Unit = Unit

    fun cancel() {
        completableJob.complete()
    }
}