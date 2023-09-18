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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PagingFetcher]的单元测试
 *
 * @author xcc
 * @date 2021/12/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class PagingFetcherTest {

    @Test
    fun limitedCollectOnce() {
        val result = runCatching {
            runBlocking {
                val fetcher = getTestFetcher(maxPage = 1, result = Result.NORMAL)
                fetcher.flow.launchIn(this)
                fetcher.flow.launchIn(this)
            }
        }
        assertThat(result.exceptionOrNull()).isNotNull()
    }

    @Test
    fun collectRefreshSuccessPagingEvent(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 1, result = Result.NORMAL)
        val events = mutableListOf<PagingEvent<String>>()
        launch { fetcher.flow.toList(events) }

        delay(100)
        fetcher.close()

        assertThat(events.size).isEqualTo(2)
        assertThat(events.first().loadStates.refresh.isLoading).isTrue()
        assertThat(events.last().loadStates.refresh.isSuccess).isTrue()
        assertThat(events.last().loadStates.refresh.isFully).isTrue()
    }

    @Test
    fun collectRefreshFailurePagingEvent(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 1, result = Result.REFRESH_FAILURE)
        val events = mutableListOf<PagingEvent<String>>()
        launch { fetcher.flow.toList(events) }

        delay(100)
        fetcher.close()

        assertThat(events.size).isEqualTo(2)
        assertThat(events.first().loadStates.refresh.isLoading).isTrue()
        assertThat(events.last().loadStates.refresh.isFailure).isTrue()
        assertThat(events.last().loadStates.refresh.exception).isNotNull()
    }

    @Test
    fun collectRefreshRetryPagingEvent(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 1, result = Result.REFRESH_FAILURE)
        val events = mutableListOf<PagingEvent<String>>()
        launch { fetcher.flow.toList(events) }

        delay(100)
        events.clear()
        fetcher.retry()
        delay(100)
        fetcher.close()

        assertThat(events.size).isEqualTo(2)
        assertThat(events.first().loadStates.refresh.isLoading).isTrue()
        assertThat(events.last().loadStates.refresh.isSuccess).isTrue()
        assertThat(events.last().loadStates.refresh.isFully).isTrue()
    }

    @Test
    fun collectAppendSuccessPagingEvent(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 2, result = Result.NORMAL)
        val events = mutableListOf<PagingEvent<String>>()
        launch {
            fetcher.flow.filter {
                it.loadType == LoadType.APPEND
            }.toList(events)
        }

        delay(100)
        fetcher.append()
        delay(100)
        fetcher.close()

        assertThat(events.size).isEqualTo(2)
        assertThat(events.first().loadStates.append.isLoading).isTrue()
        assertThat(events.last().loadStates.append.isSuccess).isTrue()
        assertThat(events.last().loadStates.append.isFully).isTrue()
    }

    @Test
    fun collectAppendFailurePagingEvent(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 2, result = Result.APPEND_FAILURE)
        val events = mutableListOf<PagingEvent<String>>()
        launch {
            fetcher.flow.filter {
                it.loadType == LoadType.APPEND
            }.toList(events)
        }

        delay(100)
        fetcher.append()
        delay(100)
        fetcher.close()

        assertThat(events.size).isEqualTo(2)
        assertThat(events.first().loadStates.append.isLoading).isTrue()
        assertThat(events.last().loadStates.append.isFailure).isTrue()
        assertThat(events.last().loadStates.append.exception).isNotNull()
    }

    @Test
    fun collectAppendRetryPagingEvent(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 2, result = Result.APPEND_FAILURE)
        val events = mutableListOf<PagingEvent<String>>()
        launch {
            fetcher.flow.filter {
                it.loadType == LoadType.APPEND
            }.toList(events)
        }

        delay(100)
        fetcher.append()
        delay(100)

        events.clear()
        fetcher.retry()
        delay(100)
        fetcher.close()

        assertThat(events.size).isEqualTo(2)
        assertThat(events.first().loadStates.append.isLoading).isTrue()
        assertThat(events.last().loadStates.append.isSuccess).isTrue()
        assertThat(events.last().loadStates.append.isFully).isTrue()
    }

    @Test
    fun refreshFailureDisallowAppend(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 1, result = Result.REFRESH_FAILURE)
        val events = mutableListOf<PagingEvent<String>>()
        launch {
            fetcher.flow.toList(events)
        }

        delay(100)
        assertThat(events.size).isEqualTo(2)

        fetcher.append()
        delay(100)
        fetcher.close()
        assertThat(events.size).isEqualTo(2)
    }

    @Test
    fun refreshFullyDisallowAppend(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 1, result = Result.NORMAL)
        val events = mutableListOf<PagingEvent<String>>()
        launch {
            fetcher.flow.toList(events)
        }

        delay(100)
        assertThat(events.size).isEqualTo(2)

        fetcher.append()
        delay(100)
        fetcher.close()
        assertThat(events.size).isEqualTo(2)
    }

    @Test
    fun appendFullyDisallowAppend(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 2, result = Result.NORMAL)
        val events = mutableListOf<PagingEvent<String>>()
        launch {
            fetcher.flow.filter {
                it.loadType == LoadType.APPEND
            }.toList(events)
        }

        delay(100)
        fetcher.append()
        delay(100)
        fetcher.append()
        delay(100)
        fetcher.close()
        assertThat(events.size).isEqualTo(2)
    }

    @Test
    fun closedDisallowSendPagingEvent(): Unit = runBlocking {
        val fetcher = getTestFetcher(maxPage = 2, result = Result.NORMAL)
        val events = mutableListOf<PagingEvent<String>>()
        fetcher.close()
        launch { fetcher.flow.toList(events) }

        delay(100)
        assertThat(events.size).isEqualTo(0)
    }

    private fun getTestFetcher(
        maxPage: Int,
        result: Result
    ): PagingFetcher<Int, String> = PagingFetcher(
        initKey = 1,
        config = PagingConfig(pageSize = 10),
        source = TestSource(maxPage, retryCount = 1, result),
        appendEvent = ConflatedEvent(),
        retryEvent = ConflatedEvent()
    )

    private class TestSource(
        private val maxPage: Int,
        private var retryCount: Int,
        private val result: Result
    ) : PagingSource<Int, String> {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> {
            return when (result) {
                Result.NORMAL -> normalResult(params)
                Result.REFRESH_FAILURE -> failureResult(params, result)
                Result.APPEND_FAILURE -> failureResult(params, result)
            }
        }

        private fun normalResult(params: LoadParams<Int>): LoadResult<Int, String> {
            val range = when (params) {
                is LoadParams.Refresh -> {
                    (1..params.pageSize)
                }
                is LoadParams.Append -> {
                    val start = params.pageSize * (params.key - 1) + 1
                    val end = start + params.pageSize - 1
                    start..end
                }
            }

            val data = range.map { it.toString() }
            val nextKey = if (params.key >= maxPage) null else params.key + 1
            return LoadResult.Success(data, nextKey = nextKey)
        }

        private fun failureResult(
            params: LoadParams<Int>,
            type: Result
        ): LoadResult<Int, String> {
            val result = normalResult(params)
            val isRetryNeeded = when {
                retryCount <= 0 -> false
                type == Result.REFRESH_FAILURE
                        && params is LoadParams.Refresh -> true
                type == Result.APPEND_FAILURE
                        && params is LoadParams.Append -> true
                else -> false
            }
            if (isRetryNeeded) {
                --retryCount
                return LoadResult.Failure(IllegalArgumentException())
            }
            return result
        }
    }

    private enum class Result {
        NORMAL, REFRESH_FAILURE, APPEND_FAILURE
    }
}