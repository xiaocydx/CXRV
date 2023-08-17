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
import com.xiaocydx.cxrv.paging.StoreInTest.Companion.storeInTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [broadcastIn]操作符的单元测试
 *
 * @author xcc
 * @date 2023/8/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class BroadcastInTest {

    @Test
    fun broadcastInAfterShoreIn() {
        val result = runCatching {
            runBlocking {
                val flow = TestPagingDataFlow()
                val scope = CoroutineScope(Job(coroutineContext.job))
                flow.storeInTest(scope).onEach { }.broadcastIn(scope).collect()
            }
        }
        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun lazyCollect(): Unit = runBlocking {
        var collectPagingData = false
        var collectPagingEvent = false
        val upstream = TestPagingDataFlow {
            collectPagingEvent = true
        }.onEach {
            collectPagingData = true
        }

        val scope = CoroutineScope(Job())
        upstream.broadcastIn(scope)
        awaitEventLoopScheduled()
        assertThat(collectPagingData).isFalse()
        assertThat(collectPagingEvent).isFalse()
    }

    @Test
    fun cancelChildren(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        var children = scope.coroutineContext.job.children.toList()
        assertThat(children).hasSize(0)

        upstream.broadcastIn(scope)
            .onEach { it.flow.collect() }
            .launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        children = scope.coroutineContext.job.children.toList()
        assertThat(children).hasSize(2)

        scope.coroutineContext.job.cancelAndJoin()
        children = scope.coroutineContext.job.children.toList()
        assertThat(children).hasSize(0)
    }

    @Test
    fun cancelCollector(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        val broadcastIn = upstream.broadcastIn(scope)

        val job = broadcastIn.onEach {
            it.flow.collect { delay(1000) }
        }.launchIn(UNDISPATCHED, this)
        assertThat(job.isActive).isTrue()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isFalse()

        scope.coroutineContext.job.cancelAndJoin()
        job.join()
        assertThat(job.isActive).isFalse()
        assertThat(job.isCancelled).isFalse()
        assertThat(job.isCompleted).isTrue()
    }

    @Test
    fun unlimitedCollector(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val result = runCatching {
            coroutineScope {
                val scope = CoroutineScope(Job())
                val broadcastIn = upstream.broadcastIn(scope)
                broadcastIn.onEach { it.flow.collect() }.launchIn(UNDISPATCHED, this)
                broadcastIn.onEach { it.flow.collect() }.launchIn(UNDISPATCHED, this)
                awaitEventLoopScheduled()
                coroutineContext.job.cancel()
            }
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test
    fun collectValue(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val upstreamPagingDataList = mutableListOf<PagingData<Int>>()
        val upstreamPagingEventList = mutableListOf<PagingEvent<Int>>()
        val job = upstream.onEach {
            upstreamPagingDataList.add(it)
            it.flow.collect(upstreamPagingEventList::add)
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        job.cancelAndJoin()

        val scope = CoroutineScope(Job())
        val broadcastIn = upstream.broadcastIn(scope)
        val broadcastInPagingDataList = mutableListOf<PagingData<Int>>()
        val broadcastInPagingEventList = mutableListOf<PagingEvent<Int>>()
        broadcastIn.onEach {
            broadcastInPagingDataList.add(it)
            it.flow.collect(broadcastInPagingEventList::add)
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        scope.coroutineContext.job.cancelAndJoin()
        assertThat(upstreamPagingDataList.size).isEqualTo(broadcastInPagingDataList.size)
        assertThat(upstreamPagingEventList.size).isEqualTo(broadcastInPagingEventList.size)
    }

    @Test
    fun repeatCollectValue(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        val broadcastIn = upstream.broadcastIn(scope)
        var prevPagingData: PagingData<Int>? = null
        var job = broadcastIn.onEach { data ->
            prevPagingData = data
            data.flow.collect()
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        job.cancelAndJoin()
        assertThat(prevPagingData).isNotNull()

        var lastPagingData: PagingData<Int>? = null
        var lastPagingEvent: PagingEvent<Int>? = null
        job = broadcastIn.onEach { data ->
            lastPagingData = data
            data.flow.collect { lastPagingEvent = it }
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        job.cancelAndJoin()
        assertThat(lastPagingData).isNotNull()
        assertThat(lastPagingEvent).isNotNull()
        assertThat(prevPagingData).isNotEqualTo(lastPagingData)
    }

    @Test
    fun multipleCollectValue(): Unit = runBlocking {
        // 单元测试是runBlocking()首次恢复进入EventLoop，
        // 实际场景是对Pager.refreshEvent发送刷新事件后，
        // 恢复收集Pager.refreshEvent的协程进入EventLoop。
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        val broadcastIn = upstream.broadcastIn(scope)

        val pagingDataList1 = mutableListOf<PagingData<Int>>()
        val pagingEventList1 = mutableListOf<PagingEvent<Int>>()
        val job1 = broadcastIn.onEach { data ->
            pagingDataList1.add(data)
            data.flow.collect(pagingEventList1::add)
        }.launchIn(UNDISPATCHED, this)

        val pagingDataList2 = mutableListOf<PagingData<Int>>()
        val pagingEventList2 = mutableListOf<PagingEvent<Int>>()
        val job2 = broadcastIn.onEach { data ->
            pagingDataList2.add(data)
            data.flow.collect(pagingEventList2::add)
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        job1.cancelAndJoin()
        job2.cancelAndJoin()
        assertThat(pagingDataList1.size).isEqualTo(pagingDataList2.size)
        assertThat(pagingEventList1.size).isEqualTo(pagingEventList2.size)
    }
}