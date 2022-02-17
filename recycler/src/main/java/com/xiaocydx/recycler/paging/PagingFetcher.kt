package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.extension.flowOnMain
import com.xiaocydx.recycler.list.ListUpdater
import com.xiaocydx.recycler.list.UpdateOp
import com.xiaocydx.recycler.list.ensureMutable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*
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
    private val source: PagingSource<K, T>,
    initList: List<T>? = null,
) {
    private var collected = false
    private var nextKey: K? = null
    private val appendEvent = ConflatedEvent<Unit>()
    private val retryEvent = ConflatedEvent<Unit>()
    private val channelFlowJob: Job = Job()
    private val updater: ListUpdater<T> =
            ListUpdater(sourceList = initList?.ensureMutable() ?: mutableListOf())
    val currentList: List<T>
        get() = updater.currentList
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

        updater.setUpdatedListener { operate ->
            val event: PagingEvent<T> = PagingEvent.ListUpdate(
                loadType = null, loadStates, operate
            )
            channel.trySend(event)
                .takeIf { result ->
                    result.isFailure && !result.isClosed
                }?.let {
                    // 同步发送失败，原因可能是buffer满了，
                    // 启动协程，调用send挂起等待buffer空位，
                    // 确保更新操作事件不会丢失。
                    launch { channel.send(event) }
                }
        }

        channel.doLoad(LoadType.REFRESH)

        launch {
            appendEvent.flow
                .filter { loadStates.isAllowAppend }
                .collect { channel.doLoad(LoadType.APPEND) }
        }

        launch {
            retryEvent.flow
                .mapNotNull { loadStates.failureLoadType }
                .collect { channel.doLoad(it) }
        }
    }.flowOnMain()

    private suspend fun SendChannel<PagingEvent<T>>.doLoad(loadType: LoadType) {
        setSourceState(loadType, LoadState.Loading)
        send(PagingEvent.LoadUpdate(loadType, loadStates))

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
                val operate = when (loadType) {
                    LoadType.REFRESH -> UpdateOp.SubmitList(loadResult.data)
                    LoadType.APPEND -> UpdateOp.AddItems(currentList.size, loadResult.data)
                }
                updateList(operate, dispatch = false)
                send(PagingEvent.ListUpdate(loadType, loadStates, operate))
            }
            is LoadResult.Failure -> {
                setSourceState(loadType, LoadState.Failure(loadResult.exception))
                send(PagingEvent.LoadUpdate(loadType, loadStates))
            }
        }
    }

    private fun setSourceState(loadType: LoadType, newState: LoadState): LoadStates {
        loadStates = loadStates.modifyState(loadType, newState)
        return loadStates
    }

    private fun loadParams(
        loadType: LoadType
    ): LoadParams<K> = LoadParams.create(
        loadType = loadType,
        key = when (loadType) {
            LoadType.REFRESH -> initKey
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

    fun updateList(op: UpdateOp<T>, dispatch: Boolean) {
        updater.updateList(op, dispatch)
    }

    fun close() {
        channelFlowJob.cancel()
    }

    private inline fun <E> safeChannelFlow(
        crossinline block: suspend CoroutineScope.(SendChannel<E>) -> Unit
    ): Flow<E> = channelFlow {
        block(SafeSendChannel(this))
    }

    private class SafeSendChannel<E>(
        private val channel: SendChannel<E>
    ) : SendChannel<E> by channel {

        override suspend fun send(element: E) {
            try {
                channel.send(element)
            } catch (e: ClosedSendChannelException) {
                throw CancellationException(e.message)
            }
        }
    }
}