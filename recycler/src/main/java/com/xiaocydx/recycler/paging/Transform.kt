package com.xiaocydx.recycler.paging

import androidx.annotation.CheckResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 通过[transformItem]或者[transformData]转换[PagingData]中的事件流
 *
 * **注意**：若对`Flow<PagingData<T>>`先调用[storeIn]，后调用[transformEventFlow]，
 * 则会抛出[IllegalArgumentException]异常，详细原因可以看[storeIn]的注释。
 * ```
 * val flow: Flow<PagingData<T>> = ...
 * flow.transformEventFlow { eventFlow ->
 *     eventFlow.transformItem { loadType, item ->
 *         ...
 *     }
 * }
 * ```
 *
 * 由于`Flow<PagingData<T>>`是嵌套流的结构，
 * 因此基础库中不会将[transformEventFlow]和[transformItem]组合起来，
 * 声明`Flow<PagingData<T>>.transformItem()`扩展函数简化代码，
 * 因为这类扩展函数会对调用者产生误导，例如下面的代码：
 * ```
 * val flow: Flow<PagingData<T>> = ...
 * // 组合后的扩展函数
 * flow.transformItem { loadType, item ->
 *     // 对调度者产生误导，误以为item的转换过程，
 *     // 是在Dispatchers.Default调度的线程上进行。
 *     ...
 * }.flowOn(Dispatchers.Default) // 此处是对Flow<PagingData<T>>调用操作符
 * ```
 */
inline fun <T : Any, R : Any> Flow<PagingData<T>>.transformEventFlow(
    crossinline transform: suspend (flow: Flow<PagingEvent<T>>) -> Flow<PagingEvent<R>>
): Flow<PagingData<R>> = map { data ->
    data.ensureAllowTransform()
    PagingData(transform(data.flow), data.mediator)
}

/**
 * 当事件流的事件为[PagingEvent.LoadDataSuccess]时，调用[transform]转换item
 */
inline fun <T : Any, R : Any> Flow<PagingEvent<T>>.transformItem(
    crossinline transform: suspend (loadType: LoadType, item: T) -> R
): Flow<PagingEvent<R>> = transformData { loadType, data ->
    data.map { item -> transform(loadType, item) }
}

/**
 * 当事件流的事件为[PagingEvent.LoadDataSuccess]时，调用[transform]转换集合
 */
@Suppress("UNCHECKED_CAST")
inline fun <T : Any, R : Any> Flow<PagingEvent<T>>.transformData(
    crossinline transform: suspend (loadType: LoadType, data: List<T>) -> List<R>
): Flow<PagingEvent<R>> = map { event ->
    when (event) {
        is PagingEvent.LoadStateUpdate -> event as PagingEvent<R>
        is PagingEvent.LoadDataSuccess -> event.run {
            PagingEvent.LoadDataSuccess(transform(loadType, data), loadType, loadStates)
        }
        is PagingEvent.ListStateUpdate -> {
            throw UnsupportedOperationException("不支持对PagingEvent.ListStateUpdate的转换。")
        }
    }
}

/**
 * 确保允许转换`PagingData<T>`，若不允许，则抛出[IllegalArgumentException]异常
 */
@PublishedApi
internal fun PagingData<*>.ensureAllowTransform() {
    require(mediator.asListMediator<Any>() == null) {
        "transformEventFlow()必须在storeIn()之前调用。"
    }
}

@CheckResult
internal fun <T : Any> PagingData<T>.modifyFlow(
    flow: Flow<PagingEvent<T>>
): PagingData<T> = PagingData(flow, mediator)