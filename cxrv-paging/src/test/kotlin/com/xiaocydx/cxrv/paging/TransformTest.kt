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
import com.xiaocydx.cxrv.paging.PagingEvent.LoadDataSuccess
import com.xiaocydx.cxrv.paging.StoreInTest.Companion.storeInTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `Flow<PagingData<T>>`转换操作符的单元测试
 *
 * @author xcc
 * @date 2023/8/3
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class TransformTest {

    @Test
    fun flowMapAfterShoreIn(): Unit = runBlocking {
        val result = runCatching {
            runBlocking {
                val flow = TestPagingDataFlow()
                val scope = CoroutineScope(Job(coroutineContext.job))
                flow.storeInTest(scope).onEach {  }.flowMap { it }.collect()
            }
        }
        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun itemMap(): Unit = runBlocking {
        val upstream = TestPagingDataFlow(awaitCancellation = false)
        val upstreamList = mutableListOf<Int>()
        upstream.collect {
            it.flow.filterIsInstance<LoadDataSuccess<Int>>()
                .collect { event -> upstreamList.addAll(event.data) }
        }

        val transform = upstream.flowMap {
            it.itemMap { _, item -> item.toString() }
        }
        val transformList = mutableListOf<String>()
        transform.collect {
            it.flow.filterIsInstance<LoadDataSuccess<String>>()
                .collect { event -> transformList.addAll(event.data) }
        }

        assertThat(upstreamList.map { it.toString() }).isEqualTo(transformList)
    }

    @Test
    fun dataMap(): Unit = runBlocking {
        val upstream = TestPagingDataFlow(awaitCancellation = false)
        val upstreamList = mutableListOf<Int>()
        upstream.collect {
            it.flow.filterIsInstance<LoadDataSuccess<Int>>()
                .collect { event -> upstreamList.addAll(event.data) }
        }

        val transform = upstream.flowMap {
            it.dataMap { _, data -> data.filter { item -> item % 2 == 0 } }
        }
        val transformList = mutableListOf<Int>()
        transform.collect {
            it.flow.filterIsInstance<LoadDataSuccess<Int>>()
                .collect { event -> transformList.addAll(event.data) }
        }

        assertThat(upstreamList.filter { it % 2 == 0 }).isEqualTo(transformList)
    }

    @Test
    fun appendPrefetch(): Unit = runBlocking {
        val upstream = TestPagingDataFlow(awaitCancellation = false)
        val upstreamPrefetch = upstream.map { it.mediator.appendPrefetch }.first()

        val transform = upstream.appendPrefetch(PagingPrefetch.None)
        val transformPrefetch = transform.map { it.mediator.appendPrefetch }.first()

        assertThat(upstreamPrefetch).isNotEqualTo(transformPrefetch)
    }
}