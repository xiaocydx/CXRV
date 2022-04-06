package com.xiaocydx.recycler.paging

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.flowOnMain
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

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
    var loadStates: LoadStates = LoadStates.Incomplete
        private set

    val flow: Flow<PagingEvent<T>> = channelFlow {
        check(!isCollected) { "分页事件流Flow<PagingEvent<*>>只能被收集一次" }
        isCollected = true
        completableJob.invokeOnCompletion {
            // 注意：此处不要用函数引用::close简化代码，
            // 这会将invokeOnCompletion的异常恢复给下游。
            close()
        }

        launch(start = UNDISPATCHED) {
            appendEvent.flow.filter {
                if (loadStates.append.isFailure) {
                    config.appendFailureAutToRetry
                } else {
                    loadStates.isAllowAppend
                }
            }.collect {
                doLoad(LoadType.APPEND)
            }
        }

        launch(start = UNDISPATCHED) {
            retryEvent.flow.mapNotNull {
                loadStates.failureLoadType
            }.collect { loadType ->
                doLoad(loadType)
            }
        }

        doLoad(LoadType.REFRESH)
    }.flowOnMain()

    @MainThread
    private suspend fun SendChannel<PagingEvent<T>>.doLoad(loadType: LoadType) {
        setLoadState(loadType, LoadState.Loading)
        send(PagingEvent.LoadStateUpdate(loadType, loadStates))

        var loadResult: LoadResult<K, T>? = null
        while (loadResult == null) {
            loadResult = try {
                source.load(loadParams(loadType))
            } catch (e: Throwable) {
                LoadResult.Failure(e)
            }

            if (loadResult !is LoadResult.Success
                    || loadResult.data.isNotEmpty()
                    || loadResult.nextKey == null) {
                continue
            }

            loadResult = if (config.loadResultEmptyFetchNext) {
                nextKey = loadResult.nextKey
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
    private fun loadParams(
        loadType: LoadType
    ): LoadParams<K> = LoadParams.create(
        loadType = loadType,
        key = when (loadType) {
            LoadType.REFRESH -> nextKey ?: initKey
            LoadType.APPEND -> requireNotNull(nextKey) {
                "nextKey == `null`表示加载完成，不能再进行末尾加载"
            }
        },
        pageSize = when (loadType) {
            LoadType.REFRESH -> config.initPageSize
            else -> config.pageSize
        }
    )

    fun append() {
        appendEvent.send(Unit)
    }

    fun retry() {
        retryEvent.send(Unit)
    }

    fun close() {
        completableJob.complete()
    }
}