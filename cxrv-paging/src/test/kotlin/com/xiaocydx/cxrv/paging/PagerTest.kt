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
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [Pager]的单元测试
 *
 * @author xcc
 * @date 2023/8/14
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class PagerTest {

    @Test
    fun limitedOneCollector() {
        val result = runCatching {
            runBlocking {
                val pager = Pager<Int, String>(
                    initKey = 1,
                    config = PagingConfig(10),
                    source = { awaitCancellation() }
                )
                pager.flow.launchIn(this)
                pager.flow.launchIn(this)
            }
        }
        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun repeatCollectRefresh() {
        var refreshCount = 0
        val repeatCount = 2
        val result = runCatching {
            runBlocking {
                val pager = Pager<Int, String>(
                    initKey = 1,
                    config = PagingConfig(10),
                    source = {
                        refreshCount++
                        awaitCancellation()
                    }
                )
                repeat(repeatCount) {
                    val job = pager.flow
                        .onEach { it.flow.collect() }
                        .launchIn(this)
                    delay(200)
                    job.cancelAndJoin()
                }
            }
        }
        assertThat(refreshCount).isEqualTo(repeatCount)
        assertThat(result.exceptionOrNull()).isNull()
    }

    @Test
    fun refreshBeforeCollect(): Unit = runBlocking {
        var refreshCount = 0
        val pager = Pager<Int, String>(
            initKey = 1,
            config = PagingConfig(10),
            source = {
                refreshCount++
                awaitCancellation()
            }
        )
        pager.refresh()
        delay(200)
        assertThat(refreshCount).isEqualTo(0)
    }

    @Test
    fun refreshAfterCollect(): Unit = runBlocking {
        var refreshCount = 0
        val pager = Pager<Int, String>(
            initKey = 1,
            config = PagingConfig(10),
            source = {
                refreshCount++
                awaitCancellation()
            }
        )
        val job = pager.flow
            .onEach { it.flow.collect() }
            .launchIn(this)
        delay(200)
        pager.refresh()
        delay(200)
        job.cancelAndJoin()
        assertThat(refreshCount).isEqualTo(2)
    }

    @Test
    fun appendBeforeCollect(): Unit = runBlocking {
        var appendCount = 0
        val pager = Pager(
            initKey = 1,
            config = PagingConfig(10),
            source = { params ->
                if (params is LoadParams.Append) appendCount++
                val start = params.pageSize * (params.key - 1) + 1
                val end = start + params.pageSize - 1
                val data = (start..end).map { it.toString() }
                LoadResult.Success(data, nextKey = params.key + 1)
            }
        )
        pager.refresh()
        pager.append()
        delay(200)
        assertThat(appendCount).isEqualTo(0)
    }

    @Test
    fun appendAfterCollect(): Unit = runBlocking {
        var appendCount = 0
        val pager = Pager(
            initKey = 1,
            config = PagingConfig(10),
            source = { params ->
                if (params is LoadParams.Append) appendCount++
                val start = params.pageSize * (params.key - 1) + 1
                val end = start + params.pageSize - 1
                val data = (start..end).map { it.toString() }
                LoadResult.Success(data, nextKey = params.key + 1)
            }
        )
        val job = pager.flow
            .onEach { it.flow.collect() }
            .launchIn(this)
        delay(200)
        pager.append()
        delay(200)
        job.cancelAndJoin()
        assertThat(appendCount).isEqualTo(1)
    }

    @Test
    fun retryBeforeCollect(): Unit = runBlocking {
        var retryCount = -1 // 去除一次refresh
        val pager = Pager<Int, String>(
            initKey = 1,
            config = PagingConfig(10),
            source = {
                retryCount++
                throw IllegalArgumentException()
            }
        )
        pager.refresh()
        pager.retry()
        delay(200)
        assertThat(retryCount).isEqualTo(-1)
    }

    @Test
    fun retryAfterCollect(): Unit = runBlocking {
        var retryCount = -1 // 去除一次refresh
        val pager = Pager<Int, String>(
            initKey = 1,
            config = PagingConfig(10),
            source = {
                retryCount++
                throw IllegalArgumentException()
            }
        )
        val job = pager.flow
            .onEach { it.flow.collect() }
            .launchIn(this)
        delay(200)
        pager.retry()
        delay(200)
        job.cancelAndJoin()
        assertThat(retryCount).isEqualTo(1)
    }

    @Test
    fun clearAppendAndRetry(): Unit = runBlocking {
        var refreshCount = 0
        var appendCount = 0
        var retryCount = -2 // 去除两次refresh
        val pager = Pager<Int, String>(
            initKey = 1,
            config = PagingConfig(10),
            source = { params ->
                if (params is LoadParams.Refresh) refreshCount++
                if (params is LoadParams.Append) appendCount++
                retryCount++
                awaitCancellation()
            }
        )
        val job = pager.flow
            .onEach { it.flow.collect() }
            .launchIn(this)

        delay(200)
        pager.append()
        delay(200)
        pager.retry()
        delay(200)
        pager.refresh()
        delay(200)
        job.cancelAndJoin()

        assertThat(refreshCount).isEqualTo(2)
        assertThat(appendCount).isEqualTo(0)
        assertThat(retryCount).isEqualTo(0)
    }
}