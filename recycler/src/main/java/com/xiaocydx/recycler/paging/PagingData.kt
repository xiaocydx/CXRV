package com.xiaocydx.recycler.paging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 分页数据的容器
 *
 * @author xcc
 * @date 2021/9/14
 */
data class PagingData<T : Any>(
    val flow: Flow<PagingEvent<T>>,
    val mediator: PagingMediator
)

inline fun <T : Any, R : Any> Flow<PagingData<T>>.transformItem(
    crossinline block: (item: T) -> R
): Flow<PagingData<R>> = transformData { it.map(block) }

inline fun <T : Any, R : Any> Flow<PagingData<T>>.transformData(
    crossinline block: (data: List<T>) -> List<R>
): Flow<PagingData<R>> = map { data ->
    PagingData(data.flow.map { it.transformData(block) }, data.mediator)
}

@Suppress("UNCHECKED_CAST")
inline fun <T : Any, R : Any> PagingEvent<T>.transformData(
    crossinline block: (data: List<T>) -> List<R>
): PagingEvent<R> = when (this) {
    is PagingEvent.LoadStateUpdate -> this as PagingEvent<R>
    is PagingEvent.LoadDataSuccess -> PagingEvent.LoadDataSuccess(block(data), loadType, loadStates)
    is PagingEvent.ListStateUpdate -> throw UnsupportedOperationException("ListStateUpdate不支持转换。")
}