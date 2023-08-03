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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PagingSharedFlow]的单元测试
 *
 * @author xcc
 * @date 2023/8/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class PagingSharedFlowTest {

    @Test
    fun lazyCollect(): Unit = runBlocking {
        var collect = false
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { collect = true }
        val sharedFlow = PagingSharedFlow(scope, upstream)
        assertThat(collect).isFalse()
        launch(start = UNDISPATCHED) { sharedFlow.collect() }
        assertThat(collect).isTrue()
    }

    @Test
    fun limitCollectorCount(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val sharedFlow = PagingSharedFlow(scope, upstream, limitCollectorCount = 1)
        val result = runCatching {
            coroutineScope {
                launch(start = UNDISPATCHED) { sharedFlow.collect() }
                launch(start = UNDISPATCHED) { sharedFlow.collect() }
            }
        }
        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun cancelSharedFlowBeforeCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val sharedFlow = PagingSharedFlow(scope, upstream)
        sharedFlow.cancel()

        val job = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun cancelCoroutineScopeBeforeCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val sharedFlow = PagingSharedFlow(scope, upstream)
        scope.cancel()

        val job = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun cancelSharedFlowAfterCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val sharedFlow = PagingSharedFlow(scope, upstream)
        val job = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        assertThat(job.isActive).isTrue()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isFalse()

        sharedFlow.cancel()
        job.join()
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun cancelCoroutineScopeAfterCollect(): Unit = runBlocking {
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { awaitCancellation() }
        val sharedFlow = PagingSharedFlow(scope, upstream)
        val job = launch(start = UNDISPATCHED) { sharedFlow.collect() }
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
        val upstream = flow<Int> { }
        val sharedFlow = PagingSharedFlow(scope, upstream)
        val job1 = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        val job2 = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        assertThat(job1.isCompleted).isTrue()
        assertThat(job2.isCompleted).isTrue()
    }

    @Test
    fun withoutCollectorNeedCancelUpstreamComplete(): Unit = runBlocking {
        var collectCount = 0
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> { collectCount++ }
        val sharedFlow = PagingSharedFlow(scope, upstream, withoutCollectorNeedCancel = true)

        val job1 = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        val job2 = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        job1.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        job2.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        // 此处断言Job3立即完成，不需要取消Job3
        launch(start = UNDISPATCHED) { sharedFlow.collect() }
        assertThat(collectCount).isEqualTo(1)
    }

    @Test
    fun withoutCollectorNeedCancelRepeatCollect(): Unit = runBlocking {
        var collectCount = 0
        val scope = CoroutineScope(Job())
        val upstream = flow<Int> {
            collectCount++
            awaitCancellation()
        }
        val sharedFlow = PagingSharedFlow(scope, upstream, withoutCollectorNeedCancel = true)

        val job1 = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        val job2 = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        job1.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        job2.cancelAndJoin()
        assertThat(collectCount).isEqualTo(1)

        val job3 = launch(start = UNDISPATCHED) { sharedFlow.collect() }
        job3.cancelAndJoin()
        assertThat(collectCount).isEqualTo(2)
    }
}