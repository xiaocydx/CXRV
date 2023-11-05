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

package com.xiaocydx.cxrv.paging

import com.xiaocydx.cxrv.paging.LoadType.APPEND
import com.xiaocydx.cxrv.paging.LoadType.REFRESH
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 通过[itemMap]或者[dataMap]转换[PagingData.flow]
 *
 * 不允许在[storeIn]之后，调用[flowMap]转换`Flow<PagingData<T>>`，
 * 这会抛出[IllegalArgumentException]，详细原因可以看[storeIn]的注释。
 * ```
 * val flow: Flow<PagingData<T>> = ...
 * flow.flowMap { flow ->
 *     flow.itemMap { loadType, item -> ...}
 * }
 * ```
 *
 * 由于`Flow<PagingData<T>>`是嵌套流结构，
 * 因此基础库中不会将[flowMap]和[itemMap]组合起来，
 * 声明`Flow<PagingData<T>>.itemMap()`扩展简化代码，
 * 因为这类扩展函数会对调用者产生误导，例如下面的代码：
 * ```
 * val flow: Flow<PagingData<T>> = ...
 * // 组合后的扩展函数
 * flow.itemMap { loadType, item ->
 *     // 对调度者产生误导，误以为item的转换过程，
 *     // 是在Dispatchers.Default调度的线程上进行。
 *     ...
 * }.flowOn(Dispatchers.Default) // 此处是对Flow<PagingData<T>>调用操作符
 * ```
 */
inline fun <T : Any, R : Any> Flow<PagingData<T>>.flowMap(
    crossinline transform: suspend (flow: Flow<PagingEvent<T>>) -> Flow<PagingEvent<R>>
): Flow<PagingData<R>> = map { data ->
    data.ensureBeforeStoreInOperator { "Flow<PagingData<T>>.flowMap()" }
    PagingData(transform(data.flow), data.mediator)
}

/**
 * 当事件流的事件为[PagingEvent.LoadDataSuccess]时，调用[transform]转换item
 */
inline fun <T : Any, R : Any> Flow<PagingEvent<T>>.itemMap(
    crossinline transform: suspend (loadType: LoadType, item: T) -> R
): Flow<PagingEvent<R>> = dataMap { loadType, data ->
    data.map { item -> transform(loadType, item) }
}

/**
 * 当事件流的事件为[PagingEvent.LoadDataSuccess]时，调用[transform]转换集合
 */
@Suppress("UNCHECKED_CAST")
inline fun <T : Any, R : Any> Flow<PagingEvent<T>>.dataMap(
    crossinline transform: suspend (loadType: LoadType, data: List<T>) -> List<R>
): Flow<PagingEvent<R>> = map { event ->
    when (event) {
        is PagingEvent.LoadStateUpdate -> event as PagingEvent<R>
        is PagingEvent.LoadDataSuccess -> event.run {
            PagingEvent.LoadDataSuccess(transform(loadType, data), loadType, loadStates)
        }
        is PagingEvent.ListStateUpdate -> {
            throw UnsupportedOperationException("不支持转换PagingEvent.ListStateUpdate")
        }
    }
}

/**
 * 将上游[REFRESH]加载的滚动首位转换为[scrollToFirst]，原始值为[PagingConfig.refreshStartScrollToFirst]
 */
fun <T : Any> Flow<PagingData<T>>.refreshStartScrollToFirst(
    scrollToFirst: Boolean
): Flow<PagingData<T>> = map { data ->
    var mediator = data.mediator
    mediator = when {
        mediator.refreshStartScrollToFirst == scrollToFirst -> return@map data
        mediator is RefreshStartScrollToFirstMediator -> mediator.copy(scrollToFirst = scrollToFirst)
        else -> RefreshStartScrollToFirstMediator(scrollToFirst, mediator)
    }
    PagingData(data.flow, mediator)
}

/**
 * 将上游[APPEND]加载的自动重试转换为[autoToRetry]，原始值为[PagingConfig.appendFailureAutToRetry]
 */
fun <T : Any> Flow<PagingData<T>>.appendFailureAutToRetry(
    autoToRetry: Boolean
): Flow<PagingData<T>> = map { data ->
    var mediator = data.mediator
    mediator = when {
        mediator.appendFailureAutToRetry == autoToRetry -> return@map data
        mediator is AppendFailureAutToRetryMediator -> mediator.copy(autoToRetry = autoToRetry)
        else -> AppendFailureAutToRetryMediator(autoToRetry, mediator)
    }
    PagingData(data.flow, mediator)
}

/**
 * 将上游[APPEND]加载的预取策略转换为[prefetch]，原始值为[PagingConfig.appendPrefetch]
 */
fun <T : Any> Flow<PagingData<T>>.appendPrefetch(
    prefetch: PagingPrefetch
): Flow<PagingData<T>> = map { data ->
    var mediator = data.mediator
    mediator = when {
        mediator.appendPrefetch == prefetch -> return@map data
        mediator is AppendPrefetchMediator -> mediator.copy(prefetch = prefetch)
        else -> AppendPrefetchMediator(prefetch, mediator)
    }
    PagingData(data.flow, mediator)
}

private data class RefreshStartScrollToFirstMediator(
    private val scrollToFirst: Boolean,
    private val mediator: PagingMediator
) : PagingMediator by mediator {
    override val refreshStartScrollToFirst = scrollToFirst
}

private data class AppendFailureAutToRetryMediator(
    private val autoToRetry: Boolean,
    private val mediator: PagingMediator
) : PagingMediator by mediator {
    override val appendFailureAutToRetry = autoToRetry
}

private data class AppendPrefetchMediator(
    private val prefetch: PagingPrefetch,
    private val mediator: PagingMediator
) : PagingMediator by mediator {
    override val appendPrefetch: PagingPrefetch = prefetch
}