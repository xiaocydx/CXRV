package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.extension.flowOnMain
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
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
    private var collected = false
    private var nextKey: K? = null
    private val appendEvent = ConflatedEvent<Unit>()
    private val retryEvent = ConflatedEvent<Unit>()
    private val channelFlowJob: Job = Job()
    var loadStates: LoadStates = LoadStates.Incomplete
        private set

    val flow: Flow<PagingEvent<T>> = safeChannelFlow<PagingEvent<T>> { channel ->
        check(!collected) { "分页事件流Flow<PagingEvent<*>>只能被收集一次。" }
        collected = true
        channelFlowJob.invokeOnCompletion {
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
                .collect { channel.doLoad(it) }
        }

        channel.doLoad(LoadType.REFRESH)
    }.flowOnMain()

    private suspend fun SendChannel<PagingEvent<T>>.doLoad(loadType: LoadType) {
        setSourceState(loadType, LoadState.Loading)
        send(PagingEvent.LoadStateUpdate(loadType, loadStates))

        val loadResult: LoadResult<K, T> = try {
            source.load(loadParams(loadType))
        } catch (e: Throwable) {
            LoadResult.Failure(e)
        }

        when (loadResult) {
            is LoadResult.Success -> {
                nextKey = loadResult.nextKey
                setSourceState(loadType, LoadState.Success(
                    dataSize = loadResult.data.size,
                    isFully = nextKey == null
                ))
                send(PagingEvent.LoadDataSuccess(loadResult.data, loadType, loadStates))
            }
            is LoadResult.Failure -> {
                setSourceState(loadType, LoadState.Failure(loadResult.exception))
                send(PagingEvent.LoadStateUpdate(loadType, loadStates))
            }
        }
    }

    private fun setSourceState(loadType: LoadType, newState: LoadState) {
        loadStates = loadStates.modifyState(loadType, newState)
    }

    private fun loadParams(
        loadType: LoadType
    ): LoadParams<K> = LoadParams.create(
        loadType = loadType,
        key = when (loadType) {
            LoadType.REFRESH -> initKey
            LoadType.APPEND -> requireNotNull(nextKey) {
                "nextKey == `null`表示加载完成，不能再进行末尾加载。"
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
        channelFlowJob.cancel()
    }
}