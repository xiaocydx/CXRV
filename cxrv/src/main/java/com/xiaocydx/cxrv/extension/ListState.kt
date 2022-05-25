package com.xiaocydx.cxrv.extension

import com.xiaocydx.cxrv.list.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * 将[ListState]转换为列表更新数据流
 */
fun <T : Any> ListState<T>.asFlow(): Flow<ListData<T>> = unsafeFlow {
    val mediator = ListMediatorImpl(this@asFlow)
    emit(ListData(mediator.flow, mediator))
}

/**
 * `Flow<ListData<T>>`的值发射给[listCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.onEach { adapter.listCollector.emit(it) }
 *
 * // 简化上面的写法
 * flow.onEach(adapter)
 * ```
 */
fun <T : Any> Flow<ListData<T>>.onEach(
    adapter: ListAdapter<T, *>
): Flow<ListData<T>> = onEach(adapter.listCollector::emit)

/**
 * `Flow<ListData<T>>`的值发射给[listCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.collect { value ->
 *     adapter.listCollector.emit(value)
 * }
 *
 * // 简化上面的写法
 * flow.collect(adapter)
 * ```
 */
suspend fun <T : Any> Flow<ListData<T>>.collect(
    adapter: ListAdapter<T, *>
): Unit = collect(adapter.listCollector)