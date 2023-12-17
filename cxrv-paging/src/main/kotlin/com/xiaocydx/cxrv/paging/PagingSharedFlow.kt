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

import com.xiaocydx.cxrv.paging.WhenCollectorEmpty.CLOSE
import com.xiaocydx.cxrv.paging.WhenCollectorEmpty.NONE
import com.xiaocydx.cxrv.paging.WhenCollectorEmpty.REPEAT
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
 * 用于分页加载的可关闭[SharedFlow]，当被首次收集时，才开始收集[upstream]
 *
 * [whenCollectorEmpty]的作用：
 * 1. [NONE]表示当`collector`数量为`0`时，不做任何处理。
 * 3. [CLOSE]表示当`collector`数量为`0`时，取消收集[upstream]，关闭[SharedFlow]。
 * 2. [REPEAT]表示当`collector`数量为`0`时，取消收集[upstream]，大于`0`时重新收集[upstream]。
 *
 * **注意**：[closeSharedFlow]不是让`collector`立即取消收集[closableSharedFlow]，
 * 而是让`collector`在收集到[Closed]时，能够结束对[closableSharedFlow]的收集。
 *
 * @author xcc
 * @date 2023/8/3
 */
internal open class PagingSharedFlow<T : Any>(
    scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val whenCollectorEmpty: WhenCollectorEmpty = NONE
) : Flow<T> {
    private val collectJob: Job
    private val sharedFlow = MutableSharedFlow<Any>()
    private val closableSharedFlow = sharedFlow.takeWhile { it !== Closed }
    private val collectorCount = sharedFlow.subscriptionCount

    init {
        // 单元测试是runBlocking()首次恢复进入EventLoop，
        // 实际场景是对Pager.refreshEvent发送刷新事件后，
        // 恢复收集Pager.refreshEvent的协程进入EventLoop。
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName, start = UNDISPATCHED) {
            try {
                if (whenCollectorEmpty !== NONE) {
                    var childJob: Job? = null
                    val parentJob = coroutineContext.job
                    collectorCount.collect { count ->
                        if (count > 0 && childJob == null) {
                            childJob = launch(start = UNDISPATCHED) {
                                upstream.collect(sharedFlow::emit)
                                // upstream发射完成，结束收集
                                parentJob.cancel()
                            }
                        } else if (count == 0 && childJob != null) {
                            childJob!!.cancelAndJoin()
                            childJob = null
                            if (whenCollectorEmpty === CLOSE) parentJob.cancel()
                        }
                    }
                } else {
                    collectorCount.firstOrNull { it > 0 } ?: return@launch
                    upstream.collect(sharedFlow::emit)
                }
            } finally {
                // 当前协程可能被取消，用NonCancellable确保发射Closed
                withContext(NonCancellable) { closeSharedFlow() }
            }
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) {
        // 快路径判断
        if (!collectJob.isActive) return
        coroutineScope {
            launch {
                // 处理边界情况，在collectJob调用closeSharedFlow()之后进行收集，
                // 此时需要基于当前协程上下文，挂起等待collectJob.join()执行完成，
                // 再次调用closeSharedFlow()，当前协程没有用UNDISPATCHED启动，
                // 结合EventLoop的调度处理，当前协程一定比收集sharedFlow后执行。
                collectJob.join()
                closeSharedFlow()
            }

            val collectorId = System.identityHashCode(collector)
            beforeCollect(collectorId)
            // 如果收集处协程都是UNDISPATCHED启动，并且是立即收集sharedFlow，
            // 那么对于EventLoop，collectorCount = 1的恢复会进入调度事件队列，
            // 这也就表示collectJob收集collectorCount，当collectorCount = 1时，
            // 全部collector已完成对sharedFlow的收集，upstream发射的事件都能收到。
            @Suppress("UNCHECKED_CAST")
            closableSharedFlow.mapNotNull {
                val value = it as? CollectorValue<T>
                if (value != null) value.get(collectorId) else it as T
            }.collect(collector)
        }
    }

    private suspend fun closeSharedFlow() = sharedFlow.emit(Closed)

    protected suspend fun emitSharedFlow(collectorId: Int, value: T) {
        sharedFlow.emit(CollectorValue(collectorId, value))
    }

    /**
     * 每个`collector`收集[closableSharedFlow]之前，都会调用该函数，
     * 若启动的子协程需要在收集之后运行，则不能使用[UNDISPATCHED]，
     * 并且不能替换当前上下文的调度器。
     */
    protected open fun CoroutineScope.beforeCollect(collectorId: Int) = Unit

    private class CollectorValue<T : Any>(private val id: Int, private val value: T) {
        fun get(collectorId: Int) = if (id == collectorId) value else null
    }

    suspend fun close() = collectJob.cancelAndJoin()
}