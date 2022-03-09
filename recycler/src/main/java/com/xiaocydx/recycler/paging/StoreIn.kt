package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.list.CancellableFlow
import com.xiaocydx.recycler.list.ListOwner
import com.xiaocydx.recycler.list.ListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ### 函数作用
 * 1. 将[PagingData.flow]事件流发射的列表数据保存到[state]中。
 * 2. [state]和视图控制器建立基于[ListOwner]的双向通信。
 * 3. 若[scope]不为`null`，则会构建[PagingData]的状态流作为返回结果，
 * 构建的状态流可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到[scope]被取消。
 *
 * ### 调用顺序
 * 不能在调用[stateIn]之后，再调用[transformEventFlow]转换事件流，
 * 因为[state]和视图控制器会建立双向通信，所以需要确保[state]和视图控制器中的数据一致。
 *
 * 在ViewModel中使用[stateIn]的例子：
 * ```
 * class FooViewModel: ViewModel(
 *     private val repository: FooRepository
 * ) {
 *     private val listState = ListState<Foo>()
 *     val flow = repository.flow
 *         .transformEventFlow { eventFlow ->
 *             eventFlow.transformItem { loadType, item ->
 *                 ...
 *             }
 *         }
 *         .storeIn(listState, viewModelScope)
 * }
 * ```
 */
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
 * 构建[PagingData]的状态流
 *
 * 构建的状态流可以被重复收集，但同时只能被一个收集器收集，
 * 在被首次收集时，才会开始收集它的上游，直到[scope]被取消。
 */
private fun <T : Any> Flow<PagingData<T>>.stateIn(
    scope: CoroutineScope
): Flow<PagingData<T>> {
    var flow: CancellableFlow<PagingEvent<T>>? = null
    val upstream: Flow<PagingData<T>> = map { data ->
        flow?.cancel()
        flow = CancellableFlow(scope, data.flow)
        data.modifyFlow(flow!!)
    }
    return PagingDataStateFlow(scope, upstream)
}

/**
 * [PagingData]状态流
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