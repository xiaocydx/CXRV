package com.xiaocydx.cxrv.extension

import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.paging.PagingData
import com.xiaocydx.cxrv.paging.pagingCollector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * `Flow<PagingData<T>>`的值发射给[pagingCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.onEach { adapter.pagingCollector.emit(it) }
 *
 * // 简化上面的写法
 * flow.onEach(adapter)
 * ```
 */
fun <T : Any> Flow<PagingData<T>>.onEach(
    adapter: ListAdapter<T, *>
): Flow<PagingData<T>> = onEach(adapter.pagingCollector::emit)

/**
 * `Flow<PagingData<T>>`的值发射给[pagingCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.collect { value ->
 *     adapter.pagingCollector.emit(value)
 * }
 *
 * // 简化上面的写法
 * flow.collect(adapter)
 * ```
 */
suspend fun <T : Any> Flow<PagingData<T>>.collect(
    adapter: ListAdapter<T, *>
): Unit = collect(adapter.pagingCollector)