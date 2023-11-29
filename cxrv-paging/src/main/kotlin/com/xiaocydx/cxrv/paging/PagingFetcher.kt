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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.flowOnMain
import com.xiaocydx.cxrv.internal.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * 分页提取器，从[PagingSource]中加载结果
 *
 * @author xcc
 * @date 2021/9/13
 */
internal class PagingFetcher<K : Any, T : Any>(
    private val initKey: K,
    private val config: PagingConfig,
    private val source: PagingSource<K, T>,
    private val appendEvent: ConflatedEvent<Unit>,
    private val retryEvent: ConflatedEvent<Unit>
) {
    private var isCollected = false
    private var nextKey: K? = null
    private val completableJob: CompletableJob = Job()
    @Volatile var loadStates: LoadStates = LoadStates.Incomplete; private set

    val flow: Flow<PagingEvent<T>> = safeChannelFlow { channel ->
        assertMainThread()
        check(!isCollected) { "分页事件流Flow<PagingEvent<T>>只能被收集1次" }
        isCollected = true
        completableJob.invokeOnCompletion {
            // 注意：此处不要用channel::close简化代码，
            // 这会将invokeOnCompletion的异常恢复给下游。
            channel.close()
        }

        launch(start = UNDISPATCHED) {
            appendEvent.flow
                .filter { loadStates.isAllowAppend }
                .collect { channel.doLoad(LoadType.APPEND) }
        }

        launch(start = UNDISPATCHED) {
            retryEvent.flow
                .mapNotNull { loadStates.failureLoadType }
                .collect { loadType -> channel.doLoad(loadType) }
        }

        channel.doLoad(LoadType.REFRESH)
    }.flowOnMain()

    @MainThread
    private suspend fun SendChannel<PagingEvent<T>>.doLoad(loadType: LoadType) {
        log {
            when (loadType) {
                LoadType.REFRESH -> "PagingFetcher refresh load"
                LoadType.APPEND -> "PagingFetcher append load"
            }
        }

        setLoadState(loadType, LoadState.Loading)
        send(PagingEvent.LoadStateUpdate(loadType, loadStates))

        var loadResult: LoadResult<K, T>? = null
        while (loadResult == null) {
            try {
                val isSuspended: Boolean
                loadResult = suspendCoroutineUninterceptedOrReturn { uCont ->
                    @Suppress("UNCHECKED_CAST")
                    uCont as Continuation<LoadResult<*, *>>
                    val outcome = load(source, loadParams(loadType), uCont)
                    isSuspended = outcome === COROUTINE_SUSPENDED
                    outcome
                }
                // 当source.load()没有产生挂起时，调用yield()解决两个问题：
                // 1. config.loadResultEmptyFetchNext为true，避免在一个消息中出现死循环加载。
                // 2. 兼容共享分页数据流的场景，在初始化阶段之后发送PagingEvent，避免丢失数据。
                if (!isSuspended) yield()
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                loadResult = LoadResult.Failure(e)
            }

            if (loadResult is LoadResult.Success
                    && loadResult.data.isEmpty()
                    && loadResult.nextKey != null) {
                loadResult = if (config.loadResultEmptyFetchNext) {
                    nextKey = loadResult.nextKey
                    null
                } else {
                    LoadResult.Failure(IllegalArgumentException("data为空但nextKey不为空"))
                }
            }
        }

        when (loadResult) {
            is LoadResult.Success -> {
                nextKey = loadResult.nextKey
                setLoadState(loadType, LoadState.Success(isFully = nextKey == null))
                send(PagingEvent.LoadDataSuccess(loadResult.data, loadType, loadStates))
            }
            is LoadResult.Failure -> {
                setLoadState(loadType, LoadState.Failure(loadResult.exception))
                send(PagingEvent.LoadStateUpdate(loadType, loadStates))
            }
        }
    }

    @MainThread
    private fun setLoadState(loadType: LoadType, newState: LoadState) {
        loadStates = loadStates.modifyState(loadType, newState)
    }

    @MainThread
    private fun loadParams(loadType: LoadType): LoadParams<K> = LoadParams.create(
        loadType = loadType,
        key = when (loadType) {
            LoadType.REFRESH -> nextKey ?: initKey
            LoadType.APPEND -> requireNotNull(nextKey) { "nextKey == null表示末尾加载完成" }
        },
        pageSize = if (loadType === LoadType.REFRESH) config.initPageSize else config.pageSize
    )

    @VisibleForTesting
    fun append() = appendEvent.send(Unit)

    @VisibleForTesting
    fun retry() = retryEvent.send(Unit)

    fun close() = completableJob.cancel()
}

@Suppress("UNCHECKED_CAST")
private val load = PagingSource<Any, Any>::load
        as Function3<PagingSource<*, *>, LoadParams<*>, Continuation<LoadResult<*, *>>, *>