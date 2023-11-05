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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * 用足够长的延时等待EventLoop处理完前面和将要添加的调度事件，
 * 不需要分析调度事件具体的执行时机，这是一个比较麻烦的工作。
 */
internal suspend fun awaitEventLoopScheduled() = delay(200)

@Suppress("TestFunctionName")
internal fun TestPagingLoadingEvent() = PagingEvent.LoadStateUpdate<Int>(
    loadType = LoadType.REFRESH,
    loadStates = LoadStates(LoadState.Loading, LoadState.Incomplete)
)

@Suppress("TestFunctionName")
internal fun TestPagingSuccessEvent() = PagingEvent.LoadDataSuccess(
    data = (1..10).toList(),
    loadType = LoadType.REFRESH,
    loadStates = LoadStates(LoadState.Success(isFully = true), LoadState.Incomplete)
)

@Suppress("TestFunctionName")
internal fun TestPagingDataFlow(
    awaitCancellation: Boolean = true,
    block: suspend FlowCollector<PagingEvent<Int>>.() -> Unit = {}
) = flow {
    val flow = flow {
        block()
        emit(TestPagingLoadingEvent())
        emit(TestPagingSuccessEvent())
        if (awaitCancellation) awaitCancellation()
    }
    val mediator = TestPagingMediator()
    emit(PagingData(flow, mediator))
    if (awaitCancellation) awaitCancellation()
}

internal class TestPagingMediator : PagingMediator {
    override val loadStates = LoadStates.Incomplete
    override val refreshStartScrollToFirst = true
    override val appendFailureAutToRetry = true
    override val appendPrefetch = PagingPrefetch.Default
    override fun refresh() = Unit
    override fun append() = Unit
    override fun retry() = Unit
}

internal fun <T> Flow<T>.launchIn(
    start: CoroutineStart,
    scope: CoroutineScope
) = scope.launch(start = start) { collect() }