package com.xiaocydx.cxrv.paging

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 通过[itemMap]或者[dataMap]转换[PagingData.flow]
 *
 * **注意**：若对`Flow<PagingData<T>>`先调用[storeIn]，后调用[flowMap]，
 * 则会抛出[IllegalArgumentException]异常，详细原因可以看[storeIn]的注释。
 * ```
 * val flow: Flow<PagingData<T>> = ...
 * flow.flowMap { eventFlow ->
 *     eventFlow.itemMap { loadType, item ->
 *         ...
 *     }
 * }
 * ```
 *
 * 由于`Flow<PagingData<T>>`是嵌套流结构，
 * 因此基础库中不会将[flowMap]和[itemMap]组合起来，
 * 声明`Flow<PagingData<T>>.itemMap`扩展函数简化代码，
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
    data.ensureAllowTransform()
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
            throw UnsupportedOperationException("不支持对PagingEvent.ListStateUpdate的转换")
        }
    }
}

/**
 * 确保允许转换`PagingData<T>`，若不允许，则抛出[IllegalArgumentException]异常
 */
@PublishedApi
internal fun PagingData<*>.ensureAllowTransform() {
    require(mediator.asListMediator<Any>() == null) {
        "flowMap()必须在storeIn()之前调用"
    }
}