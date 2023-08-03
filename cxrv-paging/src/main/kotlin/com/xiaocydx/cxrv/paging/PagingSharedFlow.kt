package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 用于分页加载的可取消[SharedFlow]
 *
 * [PagingSharedFlow]可以被重复收集，同时只能被[limitCollectorCount]个收集器收集，
 * 在被首次收集时，才会开始收集[upstream]，直到主动调用[cancel]或者`scope`被取消。
 *
 * @author xcc
 * @date 2023/8/3
 */
internal open class PagingSharedFlow<T : Any>(
    private val scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val limitCollectorCount: Int = -1,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : Flow<T> {
    @Volatile private var isCancelled = false
    private var collectJob: Job? = null

    // extraBufferCapacity = 1确保能处理cancelValue
    private val cancelValue: T? = null
    private val sharedFlow = MutableSharedFlow<T?>(extraBufferCapacity = 1)
    private val cancellableSharedFlow = sharedFlow.takeWhile { it != cancelValue }.mapNotNull { it }

    override suspend fun collect(collector: FlowCollector<T>) {
        var canCollect = true
        var activeValue: T? = null
        withMainDispatcher {
            checkCollectorCount()
            if (isCancelled) {
                canCollect = false
            } else {
                activeValue = onActive()
                launchCollectJob()
            }
        }
        // 调度过程中，isCancelled可能被赋值为true，需要重新判断
        if (canCollect && !isCancelled) {
            if (activeValue != null) collector.emit(activeValue!!)
            collector.emitAll(cancellableSharedFlow)
        }
    }

    @MainThread
    private fun checkCollectorCount() {
        val count = limitCollectorCount
        if (count >= 0 && sharedFlow.subscriptionCount.value >= count) {
            throw IllegalStateException("${javaClass.simpleName}只能被${count}个收集器收集")
        }
    }

    @MainThread
    @Suppress("OPT_IN_USAGE")
    private fun launchCollectJob() {
        if (collectJob != null) return
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName + mainDispatcher) {
            sharedFlow.subscriptionCount.firstOrNull { it > 0 } ?: return@launch
            sharedFlow.emitAll(upstream.onEach(::onReceive))
        }
        // 若scope已被取消，则直接执行invokeOnCompletion
        collectJob!!.invokeOnCompletion {
            // MainThread
            isCancelled = true
            GlobalScope.launch(
                context = mainDispatcher,
                start = CoroutineStart.UNDISPATCHED
            ) {
                sharedFlow.emit(cancelValue)
            }
        }
    }

    private suspend inline fun <R> withMainDispatcher(
        context: CoroutineContext = EmptyCoroutineContext,
        crossinline block: suspend () -> R
    ): R {
        val dispatcher = mainDispatcher
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

    suspend fun cancel() {
        withMainDispatcher {
            isCancelled = true
            collectJob?.cancelAndJoin()
        }
    }
}