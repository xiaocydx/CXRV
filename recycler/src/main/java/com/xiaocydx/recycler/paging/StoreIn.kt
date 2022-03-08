package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.list.CancellableFlow
import com.xiaocydx.recycler.list.ListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// FIXME: 2022/3/8 补充注释
fun <T : Any> Flow<PagingData<T>>.storeIn(
    state: ListState<T>,
    scope: CoroutineScope? = null
): Flow<PagingData<T>> {
    val flow = map { data ->
        val mediator = PagingListMediator(data, state)
        PagingData(mediator.flow, mediator)
    }
    return if (scope != null) flow.stateIn(scope) else flow
}

/**
 * 构建分页数据的状态流
 *
 * 构建的状态流可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到[scope]被取消。
 */
private fun <T : Any> Flow<PagingData<T>>.stateIn(
    scope: CoroutineScope
): Flow<PagingData<T>> {
    var flow: CancellableFlow<PagingEvent<T>>? = null
    return map { data ->
        flow?.cancel()
        flow = CancellableFlow(scope, data.flow)
        data.copy(flow = flow!!)
    }.let { PagingDataStateFlow(scope, it) }
}

/**
 * 分页数据状态流
 */
private class PagingDataStateFlow<T : Any>(
    scope: CoroutineScope,
    upstream: Flow<PagingData<T>>
) : CancellableFlow<PagingData<T>>(scope, upstream) {
    private var state: PagingData<T>? = null

    override suspend fun onActive(channel: SendChannel<PagingData<T>>) {
        if (state != null) {
            channel.send(state!!)
        }
    }

    override suspend fun onReceive(value: PagingData<T>) {
        state = value
    }
}