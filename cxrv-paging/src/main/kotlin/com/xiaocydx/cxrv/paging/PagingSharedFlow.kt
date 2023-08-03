package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
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
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * 用于分页加载的可取消[SharedFlow]
 *
 * [PagingSharedFlow]可以被重复收集，同时只能被[limitCollectorCount]个收集器收集，
 * 当被首次收集时，才开始收集[upstream]，以下[withoutCollectorNeedCancel]的作用：
 * 1. 若为`false`，则直到主动调用[cancel]或者`scope`被取消，才取消收集[upstream]。
 * 2. 若为`true`，则直到主动调用[cancel]或者`scope`被取消，才取消收集[upstream]，
 * 或者当收集器数量为`0`时取消收集[upstream]，大于`0`时重新收集[upstream]。
 *
 * **注意**：当[upstream]发射完成时，[PagingSharedFlow]会对其结束收集，
 * 并且转换至取消状态，取消所有收集器对[cancellableSharedFlow]的收集。
 *
 * @author xcc
 * @date 2023/8/3
 */
internal open class PagingSharedFlow<T : Any>(
    private val scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val limitCollectorCount: Int = -1,
    private val withoutCollectorNeedCancel: Boolean = false,
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
        // 调度过程isCancelled可能被赋值为true，需要重新判断
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
        // coroutineName用于调式阶段理清楚Job树
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName + mainDispatcher) {
            if (withoutCollectorNeedCancel) {
                var childJob: Job? = null
                val parentJob = coroutineContext.job
                sharedFlow.subscriptionCount.collect { count ->
                    if (count > 0 && childJob == null) {
                        childJob = launch(start = UNDISPATCHED) {
                            sharedFlow.emitAll(upstream.onEach(::onReceive))
                            // upstream发射完成，对其结束收集，转换至取消状态
                            parentJob.cancel()
                        }
                    } else if (count == 0 && childJob != null) {
                        onReceive(null)
                        childJob!!.cancelAndJoin()
                        childJob = null
                    }
                }
            } else {
                sharedFlow.subscriptionCount.firstOrNull { it > 0 } ?: return@launch
                sharedFlow.emitAll(upstream.onEach(::onReceive))
            }
        }

        // 三种情况执行invokeOnCompletion：
        // 1. scope已被取消，直接执行。
        // 2. collectJob被cancel()取消。
        // 3. upstream发射完成，collectJob结束收集。
        // 第1种情况，靠isCancelled = true避免收集器收集cancellableSharedFlow，
        // 第2、3种情况，靠sharedFlow.emit(cancelValue)结束收集cancellableSharedFlow。
        collectJob!!.invokeOnCompletion {
            // MainThread
            isCancelled = true
            onReceive(null)
            GlobalScope.launch(
                context = mainDispatcher,
                start = UNDISPATCHED
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

    /**
     * 每个收集器收集[cancellableSharedFlow]之前，都会调用该函数，
     * 若返回不为`null`的值，则先对收集器发射该值，然后再进行收集。
     */
    @MainThread
    protected open fun onActive(): T? = null

    /**
     * 当收集[upstream]时，每个值都会传递给该函数，可记录为状态值，
     * 若因为收集器数量等于`0`而取消收集[upstream]，则传递`null`。
     */
    @MainThread
    protected open fun onReceive(value: T?) = Unit

    suspend fun cancel() {
        withMainDispatcher {
            isCancelled = true
            collectJob?.cancelAndJoin()
        }
    }
}