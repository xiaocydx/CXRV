package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import com.xiaocydx.cxrv.internal.flowOnMain
import com.xiaocydx.cxrv.internal.log
import kotlinx.coroutines.*
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

/**
 * 分页提取器，从[PagingSource]中加载结果
 *
 * @author xcc
 * @date 2021/9/13
 */
internal class PagingFetcher<K : Any, T : Any>(
    private val initKey: K,
    private val config: PagingConfig,
    private val source: PagingSource<K, T>
) {
    private var isCollected = false
    private var nextKey: K? = null
    private val appendEvent = ConflatedEvent<Unit>()
    private val retryEvent = ConflatedEvent<Unit>()
    private val completableJob: CompletableJob = Job()
    @Volatile var loadStates: LoadStates = LoadStates.Incomplete; private set

    val flow: Flow<PagingEvent<T>> = safeChannelFlow { channel ->
        check(!isCollected) { "分页事件流Flow<PagingEvent<*>>只能被收集一次" }
        isCollected = true
        completableJob.invokeOnCompletion {
            // 注意：此处不要用channel::close简化代码，
            // 这会将invokeOnCompletion的异常恢复给下游。
            channel.close()
        }

        launch(start = UNDISPATCHED) {
            appendEvent.flow.filter {
                val loadStates = loadStates
                if (loadStates.append.isFailure) {
                    config.appendFailureAutToRetry
                } else {
                    loadStates.isAllowAppend
                }
            }.collect {
                channel.doLoad(LoadType.APPEND)
            }
        }

        launch(start = UNDISPATCHED) {
            retryEvent.flow.mapNotNull {
                loadStates.failureLoadType
            }.collect { loadType ->
                channel.doLoad(loadType)
            }
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
            loadResult = try {
                source.load(loadParams(loadType))
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                LoadResult.Failure(e)
            }

            if (loadResult !is LoadResult.Success
                    || loadResult.data.isNotEmpty()
                    || loadResult.nextKey == null) {
                continue
            }

            loadResult = if (config.loadResultEmptyFetchNext) {
                nextKey = loadResult.nextKey
                // 防止在一个消息中出现死循环
                yield()
                null
            } else {
                LoadResult.Failure(IllegalArgumentException(
                    "不合理的加载结果，data为空但nextKey不为空"
                ))
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

    fun append() {
        appendEvent.send(Unit)
    }

    fun retry() {
        retryEvent.send(Unit)
    }

    fun close() {
        completableJob.cancel()
    }
}