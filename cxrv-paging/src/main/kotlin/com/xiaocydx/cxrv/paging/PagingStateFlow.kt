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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 用于分页加载的可关闭[StateFlow]，当被首次收集时，才开始收集[upstream]
 *
 * [whenCollectorEmpty]的作用：
 * 1. [NONE]表示当`collector`数量为`0`时，不做任何处理。
 * 3. [CLOSE]表示当`collector`数量为`0`时，取消收集[upstream]，关闭[StateFlow]。
 * 2. [REPEAT]表示当`collector`数量为`0`时，取消收集[upstream]，大于`0`时重新收集[upstream]。
 *
 * **注意**：[closeStateFlow]不是让`collector`立即取消收集[closableStateFlow]，
 * 而是让`collector`在收集到[Closed]时，能够结束对[closableStateFlow]的收集。
 *
 * @author xcc
 * @date 2023/8/6
 */
internal open class PagingStateFlow<T : Any>(
    scope: CoroutineScope,
    private val upstream: Flow<T>,
    private val whenCollectorEmpty: WhenCollectorEmpty = NONE
) : Flow<T> {
    private val collectJob: Job
    private val stateFlow = MutableStateFlow<Any?>(null)
    private val closableStateFlow = stateFlow.filterNotNull().takeWhile { it !== Closed }
    private val collectorCount = stateFlow.subscriptionCount

    init {
        val coroutineName = CoroutineName(javaClass.simpleName)
        collectJob = scope.launch(coroutineName, start = UNDISPATCHED) {
            try {
                if (whenCollectorEmpty !== NONE) {
                    var childJob: Job? = null
                    val parentJob = coroutineContext.job
                    collectorCount.collect { count ->
                        if (count > 0 && childJob == null) {
                            childJob = launch(start = UNDISPATCHED) {
                                upstream.collect(stateFlow::emit)
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
                    upstream.collect(stateFlow::emit)
                }
            } finally {
                // 当前协程可能被取消，用NonCancellable确保发射Closed
                withContext(NonCancellable) { closeStateFlow() }
            }
        }
    }

    override suspend fun collect(collector: FlowCollector<T>) {
        // 快路径判断
        if (!collectJob.isActive) return
        coroutineScope {
            launch {
                // 处理边界情况，在collectJob调用closeStateFlow()之后进行收集，
                // 此时需要基于当前协程上下文，挂起等待collectJob.join()执行完成，
                // 再次调用closeStateFlow()，当前协程没有用UNDISPATCHED启动，
                // 结合EventLoop的调度处理，当前协程一定比收集stateFlow后执行。
                collectJob.join()
                closeStateFlow()
            }

            @Suppress("UNCHECKED_CAST")
            (closableStateFlow as Flow<T>).collect(collector)
        }
    }

    private suspend fun closeStateFlow() = stateFlow.emit(Closed)

    suspend fun close() = collectJob.cancelAndJoin()
}