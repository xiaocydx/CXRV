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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.list.ListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [storeIn]操作符的单元测试
 *
 * @author xcc
 * @date 2023/8/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class StoreInTest {

    @Test
    fun singleStoreIn() {
        val result = runCatching {
            runBlocking {
                val flow = TestPagingDataFlow()
                val scope = CoroutineScope(Job(coroutineContext.job))
                flow.storeInTest(scope).onEach { }.storeInTest(scope).collect()
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
        upstream.storeInTest(scope)
        awaitEventLoopScheduled()
        assertThat(collectPagingData).isFalse()
        assertThat(collectPagingEvent).isFalse()
    }

    @Test
    fun cancelChildren(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        var children = scope.coroutineContext.job.children.toList()
        assertThat(children).isEmpty()

        upstream.storeInTest(scope)
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
        val storeIn = upstream.storeInTest(scope)

        val job = storeIn.onEach {
            // delay(1000)用于模拟在背压情况下也能正常结束收集
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
    fun limitedOneCollector(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val result = runCatching {
            coroutineScope {
                val scope = CoroutineScope(Job())
                val storeIn = upstream.storeInTest(scope)
                storeIn.onEach { it.flow.collect() }.launchIn(UNDISPATCHED, this)
                storeIn.onEach { it.flow.collect() }.launchIn(UNDISPATCHED, this)
                awaitEventLoopScheduled()
                coroutineContext.job.cancel()
            }
        }
        assertThat(result.exceptionOrNull()).isNotNull()
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
        val storeIn = upstream.storeInTest(scope)
        val storeInPagingDataList = mutableListOf<PagingData<Int>>()
        val storeInPagingEventList = mutableListOf<PagingEvent<Int>>()
        storeIn.onEach {
            storeInPagingDataList.add(it)
            it.flow.collect(storeInPagingEventList::add)
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        scope.coroutineContext.job.cancelAndJoin()
        assertThat(upstreamPagingDataList.size).isEqualTo(storeInPagingDataList.size)
        assertThat(upstreamPagingEventList.size).isEqualTo(storeInPagingEventList.size)
    }

    @Test
    fun repeatCollectValue(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        val storeIn = upstream.storeInTest(scope)
        var prevPagingData: PagingData<Int>? = null
        var job = storeIn.onEach { data ->
            prevPagingData = data
            data.flow.collect()
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        job.cancelAndJoin()
        assertThat(prevPagingData).isNotNull()

        var lastPagingData: PagingData<Int>? = null
        var lastPagingEvent: PagingEvent<Int>? = null
        job = storeIn.onEach { data ->
            lastPagingData = data
            data.flow.collect { lastPagingEvent = it }
        }.launchIn(UNDISPATCHED, this)

        awaitEventLoopScheduled()
        job.cancelAndJoin()
        assertThat(lastPagingData).isEqualTo(prevPagingData)
        assertThat(lastPagingEvent).isInstanceOf(PagingEvent.ListStateUpdate::class.java)
    }

    companion object {

        fun <T : Any> Flow<PagingData<T>>.storeInTest(
            scope: CoroutineScope,
            state: ListState<T> = ListState()
        ): Flow<PagingData<T>> = storeInInternal(state, scope, ::TestPagingListMediator)

        private class TestPagingListMediator<T : Any>(
            data: PagingData<T>,
            listState: ListState<T>
        ) : PagingListMediator<T>(data, listState) {

            /**
             * [PagingListMediator.flow]的主线程调度无法完成，修改实现以完成测试
             */
            override val flow = callbackFlow {
                data.flow.onCompletion { awaitCancellation() }.collect(::send)
                awaitClose { }
            }.buffer(UNLIMITED)
        }
    }
}