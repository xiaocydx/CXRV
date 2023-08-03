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
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
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
    fun lazyCollect(): Unit = runBlocking {
        var collectPagingData = false
        var collectPagingEvent = false
        val upstream = TestPagingDataFlow {
            collectPagingEvent = true
        }.onEach {
            collectPagingData = true
        }

        val scope = CoroutineScope(Job())
        upstream.storeIn(scope)
        // 跟启动调度错开，等待所有children启动
        yield()
        assertThat(collectPagingData).isFalse()
        assertThat(collectPagingEvent).isFalse()
    }

    @Test
    fun cancelChildren(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        var children = scope.coroutineContext.job.children.toList()
        assertThat(children).hasSize(0)

        // storeIn转换后的事件流融合了列表状态的事件，因此分页事件发射完毕也不会结束
        launch(start = CoroutineStart.UNDISPATCHED) {
            upstream.storeIn(scope).onEach { it.flow.collect() }.collect()
        }

        // 跟启动调度错开，等待所有children启动
        yield()
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
        val storeIn = upstream.storeIn(scope)

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            // delay(1000)用于模拟在背压情况下也能正常结束收集
            storeIn.onEach { it.flow.collect { delay(1000) } }.collect()
        }
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
                val storeIn = upstream.storeIn(scope)
                storeIn.onEach { it.flow.collect() }.launchIn(this)
                storeIn.onEach { it.flow.collect() }.launchIn(this)
                delay(10)
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
        }.launchIn(this)
        // 不需要知道收集具体什么时候完成，用延时错开即可
        delay(10)
        job.cancelAndJoin()

        val scope = CoroutineScope(Job())
        val storeIn = upstream.storeIn(scope)
        val storeInPagingDataList = mutableListOf<PagingData<Int>>()
        val storeInPagingEventList = mutableListOf<PagingEvent<Int>>()
        storeIn.onEach {
            storeInPagingDataList.add(it)
            it.flow.collect(storeInPagingEventList::add)
        }.launchIn(this)
        // 不需要知道收集具体什么时候完成，用延时错开即可
        delay(10)
        scope.coroutineContext.job.cancelAndJoin()

        assertThat(upstreamPagingDataList.size).isEqualTo(storeInPagingDataList.size)
        assertThat(upstreamPagingEventList.size).isEqualTo(storeInPagingEventList.size)
    }

    @Test
    fun repeatCollectValue(): Unit = runBlocking {
        val upstream = TestPagingDataFlow()
        val scope = CoroutineScope(Job())
        val storeIn = upstream.storeIn(scope)
        var prevPagingData: PagingData<Int>? = null
        var job = storeIn.onEach { data ->
            prevPagingData = data
            data.flow.collect()
        }.launchIn(this)
        // 不需要知道收集具体什么时候完成，用延时错开即可
        delay(10)
        job.cancelAndJoin()
        assertThat(prevPagingData).isNotNull()

        var lastPagingData: PagingData<Int>? = null
        var lastPagingEvent: PagingEvent<Int>? = null
        job = storeIn.onEach { data ->
            lastPagingData = data
            data.flow.collect { lastPagingEvent = it }
        }.launchIn(this)
        delay(10)
        job.cancelAndJoin()

        assertThat(lastPagingData).isEqualTo(prevPagingData)
        assertThat(lastPagingEvent).isInstanceOf(PagingEvent.ListStateUpdate::class.java)
    }
}