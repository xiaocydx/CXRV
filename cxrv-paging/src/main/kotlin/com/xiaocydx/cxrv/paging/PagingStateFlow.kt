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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 用于分页加载的可取消[StateFlow]
 *
 * [PagingStateFlow]可以被重复收集，同时只能被[limitCollectorCount]个收集器收集，
 * 当被首次收集时，才开始收集[upstream]，以下[withoutCollectorNeedCancel]的作用：
 * 1. 若为`false`，则直到主动调用[cancel]或者`scope`被取消，才取消收集[upstream]。
 * 2. 若为`true`，则直到主动调用[cancel]或者`scope`被取消，才取消收集[upstream]，
 * 或者当收集器数量为`0`时取消收集[upstream]，大于`0`时重新收集[upstream]。
 *
 * 当[withoutCollectorNeedCancel]为`true`时，[canRepeatCollectAfterCancel]的作用：
 * 1. 若为`false`，则收集器数量为`0`取消收集[upstream]后，不再重复收集[upstream]。
 * 2. 若为`true`，则收集器数量为`0`取消收集[upstream]后，能再重复收集[upstream]。
 *
 * **注意**：当[upstream]发射完成时，[PagingStateFlow]会对其结束收集，
 * 并且转换至取消状态，取消所有收集器对[cancellableStateFlow]的收集。
 *
 * @author xcc
 * @date 2023/8/6
 */
internal open class PagingStateFlow<T : Any>(
    scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val limitCollectorCount: Int,
    private val withoutCollectorNeedCancel: Boolean,
    private val canRepeatCollectAfterCancel: Boolean,
) : Flow<T> {
    private val collectJob: Job
    private val stateFlow = MutableStateFlow<Any?>(null)
    private val cancellableStateFlow = stateFlow.mapNotNull { it }.takeWhile { it != CancelValue }
    private val collectorCount = stateFlow.subscriptionCount

    init {
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName, start = UNDISPATCHED) {
            try {
                if (withoutCollectorNeedCancel) {
                    var childJob: Job? = null
                    val parentJob = coroutineContext.job
                    collectorCount.collect { count ->
                        if (count > 0 && childJob == null) {
                            childJob = launch(start = UNDISPATCHED) {
                                upstream.collect(stateFlow::emit)
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
                    upstream.collect(stateFlow::emit)
                }
            } finally {
                // 当前协程可能被取消，用NonCancellable确保发射CancelValue
                withContext(NonCancellable) { cancelStateFlow() }
            }
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) {
        // 快路径判断
        if (!collectJob.isActive) return
        checkCollectorCount()
        coroutineScope {
            launch {
                // 处理边界情况，在collectJob调用cancelStateFlow()之后进行收集，
                // 此时需要基于当前协程上下文，挂起等待collectJob.join()执行完成，
                // 再次调用cancelStateFlow()，当前协程没有用UNDISPATCHED启动，
                // 结合EventLoop的调度处理，当前协程一定比收集stateFlow后执行。
                collectJob.join()
                cancelStateFlow()
            }

            @Suppress("UNCHECKED_CAST")
            (cancellableStateFlow as Flow<T>).collect(collector)
        }
    }

    private fun checkCollectorCount() {
        val count = limitCollectorCount.takeIf { it >= 0 } ?: return
        check(collectorCount.value < count) {
            "${javaClass.simpleName}只能被${count}个收集器收集"
        }
    }

    private suspend fun cancelStateFlow() {
        stateFlow.emit(CancelValue)
    }

    suspend fun cancel() = collectJob.cancelAndJoin()

    companion object CancelValue {
        const val UNLIMITED = -1
    }
}