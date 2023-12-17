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

import android.os.Build
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PagingStateFlow]的单元测试
 *
 * @author xcc
 * @date 2023/8/6
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class PagingStateFlowTest {

    @Test
    fun lazyCollect(): Unit = runBlocking {
        var collect = false
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { collect = true }
        PagingStateFlow(scope, upstream)
        awaitEventLoopScheduled()
        assertThat(collect).isFalse()
    }

    @Test
    fun unlimitedCollector(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val stateFlow = PagingStateFlow(scope, upstream)
        val result = runCatching {
            coroutineScope {
                launch(start = UNDISPATCHED) { stateFlow.collect() }
                launch(start = UNDISPATCHED) { stateFlow.collect() }
                awaitEventLoopScheduled()
                coroutineContext.job.cancel()
            }
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun closeStateFlowBeforeCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val stateFlow = PagingStateFlow(scope, upstream)
        stateFlow.close()

        val job = launch(start = UNDISPATCHED) { stateFlow.collect() }
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun cancelCoroutineScopeBeforeCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val stateFlow = PagingStateFlow(scope, upstream)
        scope.cancel()

        val job = launch(start = UNDISPATCHED) { stateFlow.collect() }
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun closeStateFlowAfterCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val stateFlow = PagingStateFlow(scope, upstream)
        val job = launch(start = UNDISPATCHED) { stateFlow.collect() }
        assertThat(job.isActive).isTrue()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isFalse()

        stateFlow.close()
        job.join()
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun cancelCoroutineScopeAfterCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val stateFlow = PagingStateFlow(scope, upstream)
        val job = launch(start = UNDISPATCHED) { stateFlow.collect() }
        assertThat(job.isActive).isTrue()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isFalse()

        scope.cancel()
        job.join()
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun upstreamComplete(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow {
            delay(100)
            emit(1)
        }
        val stateFlow = PagingStateFlow(scope, upstream)
        val job1 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        val job2 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        job1.join()
        job2.join()
        assertThat(job1.isCompleted).isTrue()
        assertThat(job2.isCompleted).isTrue()
    }

    @Test
    fun closeWhenCollectorEmpty(): Unit = runBlocking {
        var collectCount = 0
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> {
            collectCount++
            awaitCancellation()
        }
        val stateFlow = PagingStateFlow(scope, upstream, WhenCollectorEmpty.CLOSE)
        val job1 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        val job2 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        job1.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)
        job2.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        // 此处断言Job3立即完成，不需要取消Job3
        launch(start = UNDISPATCHED) { stateFlow.collect() }
        assertThat(collectCount).isEqualTo(1)
    }

    @Test
    fun closeWhenUpstreamComplete(): Unit = runBlocking {
        var collectCount = 0
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { collectCount++ }
        val stateFlow = PagingStateFlow(scope, upstream, WhenCollectorEmpty.CLOSE)
        val job1 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        val job2 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        job1.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        job2.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        // 此处断言Job3立即完成，不需要取消Job3
        launch(start = UNDISPATCHED) { stateFlow.collect() }
        assertThat(collectCount).isEqualTo(1)
    }

    @Test
    fun repeatWhenCollectorEmpty(): Unit = runBlocking {
        var collectCount = 0
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> {
            collectCount++
            awaitCancellation()
        }
        val stateFlow = PagingStateFlow(scope, upstream, WhenCollectorEmpty.REPEAT)
        val job1 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        val job2 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        awaitEventLoopScheduled()
        job1.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        awaitEventLoopScheduled()
        job2.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        val job3 = launch(start = UNDISPATCHED) { stateFlow.collect() }
        awaitEventLoopScheduled()
        job3.cancelAndJoin()
        assertThat(collectCount).isEqualTo(2)
    }

    @Test
    fun multipleCollectValue(): Unit = runBlocking {
        val upstream = (1..2).asFlow().onEach { delay(100) }
        val scope = CoroutineScope(Job())
        val stateFlow = PagingStateFlow(scope, upstream)
        val outcome1 = mutableListOf<Int>()
        val outcome2 = mutableListOf<Int>()
        val job1 = launch(start = UNDISPATCHED) { stateFlow.toList(outcome1) }
        val job2 = launch(start = UNDISPATCHED) { stateFlow.toList(outcome2) }
        job1.join()
        job2.join()

        // upstream发射完成，PagingStateFlow会立即发射取消标志，
        // 这可能会覆盖最后一次发射的值，因此断言至少收集的数量。
        assertThat(outcome1.size > 0).isTrue()
        assertThat(outcome1.size > 0).isTrue()
    }
}