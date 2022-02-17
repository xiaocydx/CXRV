package com.xiaocydx.recycler.list

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.flowOnMain
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*

/**
 * 可取消的接收流
 *
 * [CancellableFlow]可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到主动调用`cancel()`或者`scope`被取消。
 *
 * @author xcc
 * @date 2021/12/14
 */
internal open class CancellableFlow<T>(
    private val scope: CoroutineScope,
    private val upstream: Flow<T>? = null,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : Flow<T> {
    private var isCompleted = false
    private var collectJob: Job? = null
    private var channel: Channel<T>? = null

    /**
     * [CancellableFlow]不继承[AbstractFlow]，而是通过构建器创建委托对象，
     * 避免后续[AbstractFlow]被改动，导致需要修改[CancellableFlow]。
     */
    private val flow = flow {
        require(!isCompleted) {
            "CancellableFlow已完成，不能再被收集。"
        }
        require(channel == null) {
            "CancellableFlow只能被一个收集器收集。"
        }

        channel = Channel(CONFLATED)
        onActive(channel!!)
        launchCollectJob()
        emitAll(channel!!)
    }.onCompletion {
        channel?.close()
        channel = null
        onInactive()
    }.flowOnMain(mainDispatcher.immediate)

    override suspend fun collect(collector: FlowCollector<T>) {
        flow.collect(collector)
    }

    @MainThread
    private fun launchCollectJob() {
        if (collectJob != null) {
            return
        }
        collectJob = scope.launch(mainDispatcher.immediate) {
            if (upstream == null) {
                // 没有上游可收集，等待被取消
                awaitCancellation()
            }
            upstream.collect {
                onReceive(it)
                try {
                    channel?.send(it)
                } catch (ignored: CancellationException) {
                } catch (ignored: ClosedSendChannelException) {
                }
            }
        }
        collectJob!!.invokeOnCompletion {
            isCompleted = true
            collectJob = null
            channel?.close()
            channel = null
        }
    }

    @MainThread
    protected open suspend fun onActive(channel: SendChannel<T>): Unit = Unit

    @MainThread
    protected open suspend fun onReceive(value: T): Unit = Unit

    @MainThread
    protected open suspend fun onInactive(): Unit = Unit

    suspend fun cancel() {
        collectJob?.cancelAndJoin()
    }
}