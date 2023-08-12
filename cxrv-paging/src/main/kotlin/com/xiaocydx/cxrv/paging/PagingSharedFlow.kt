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

package com.xiaocydx.cxrv.paging

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 用于分页加载的可取消[SharedFlow]
 *
 * [PagingSharedFlow]可以被重复收集，同时只能被[limitCollectorCount]个收集器收集，
 * 当被首次收集时，才开始收集[upstream]，此时[withoutCollectorNeedCancel]的作用：
 * 1. 若为`false`，则直到主动调用[cancel]或者`scope`被取消，才取消收集[upstream]。
 * 2. 若为`true`，则直到主动调用[cancel]或者`scope`被取消，才取消收集[upstream]，
 * 或者当收集器数量为`0`时取消收集[upstream]，大于`0`时重新收集[upstream]。
 *
 * 当[withoutCollectorNeedCancel]为`true`时，[canRepeatCollectAfterCancel]的作用：
 * 1. 若为`false`，则收集器数量为`0`取消收集[upstream]后，不再重复收集[upstream]。
 * 2. 若为`true`，则收集器数量为`0`取消收集[upstream]后，能再重复收集[upstream]。
 *
 * **注意**：当[upstream]发射完成时，[PagingSharedFlow]会对其结束收集，
 * 并且转换至取消状态，取消所有收集器对[cancellableSharedFlow]的收集。
 *
 * @author xcc
 * @date 2023/8/3
 */
internal open class PagingSharedFlow<T : Any>(
    scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val limitCollectorCount: Int,
    private val withoutCollectorNeedCancel: Boolean,
    private val canRepeatCollectAfterCancel: Boolean
) : Flow<T> {
    private val collectJob: Job
    private val cancelValue: T? = null
    private val sharedFlow = MutableSharedFlow<T?>()
    private val cancellableSharedFlow = sharedFlow.takeWhile { it != cancelValue }.mapNotNull { it }
    private val collectorCount = sharedFlow.subscriptionCount

    init {
        // 单元测试是runBlocking()首次恢复进入EventLoop，
        // 实际场景是对Pager.refreshEvent发送刷新事件后，
        // 恢复收集Pager.refreshEvent的协程进入EventLoop。
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName, start = UNDISPATCHED) {
            try {
                if (withoutCollectorNeedCancel) {
                    var childJob: Job? = null
                    val parentJob = coroutineContext.job
                    collectorCount.collect { count ->
                        if (count > 0 && childJob == null) {
                            childJob = launch(start = UNDISPATCHED) {
                                upstream.collect(sharedFlow::emit)
                                // upstream发射完成，对其结束收集，转换至取消状态
                                parentJob.cancel()
                            }
                        } else if (count == 0 && childJob != null) {
                            childJob!!.cancelAndJoin()
                            childJob = null
                            if (!canRepeatCollectAfterCancel) parentJob.cancel()
                        }
                    }
                } else {
                    collectorCount.firstOrNull { it > 0 } ?: return@launch
                    upstream.collect(sharedFlow::emit)
                }
            } finally {
                // 当前协程可能被取消，用NonCancellable确保发射cancelValue
                withContext(NonCancellable) { cancelSharedFlow() }
            }
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) {
        // 快路径判断
        if (!collectJob.isActive) return
        checkCollectorCount()
        coroutineScope {
            launch {
                // 处理边界情况，在collectJob调用cancelSharedFlow()之后进行收集，
                // 此时需要基于当前协程上下文，挂起等待collectJob.join()执行完成，
                // 再次调用cancelSharedFlow()，当前协程没有用UNDISPATCHED启动，
                // 结合EventLoop的调度处理，当前协程一定比收集sharedFlow后执行。
                collectJob.join()
                cancelSharedFlow()
            }

            beforeCollect()
            // 如果收集处协程都是UNDISPATCHED启动，并且是立即收集sharedFlow，
            // 那么对于EventLoop，collectorCount = 1的恢复会进入调度事件队列，
            // 这也就表示collectJob收集collectorCount，当collectorCount = 1时，
            // 全部收集器已完成对sharedFlow的收集，upstream发射的事件都能收到。
            cancellableSharedFlow.collect(collector)
        }
    }

    private fun checkCollectorCount() {
        val count = limitCollectorCount.takeIf { it >= 0 } ?: return
        check(collectorCount.value < count) {
            "${javaClass.simpleName}只能被${count}个收集器收集"
        }
    }

    private suspend fun cancelSharedFlow() = sharedFlow.emit(cancelValue)

    protected suspend fun emitSharedFlow(value: T) = sharedFlow.emit(value)

    /**
     * 每个收集器收集[cancellableSharedFlow]之前，都会调用该函数，
     * 若启动的子协程需要在收集之后运行，则不能使用[UNDISPATCHED]，
     * 并且不能替换当前上下文的调度器。
     */
    protected open fun CoroutineScope.beforeCollect() = Unit

    suspend fun cancel() = collectJob.cancelAndJoin()

    companion object {
        const val UNLIMITED = -1
    }
}