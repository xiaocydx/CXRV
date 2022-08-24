package com.xiaocydx.cxrv.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

private const val LIST_COLLECTOR_KEY = "com.xiaocydx.cxrv.list.LIST_COLLECTOR_KEY"

/**
 * 列表数据收集器，负责收集指定流的[ListData]
 */
val <T : Any> ListAdapter<T, *>.listCollector: ListCollector<T>
    get() {
        var collector =
                getTag<ListCollector<T>>(LIST_COLLECTOR_KEY)
        if (collector == null) {
            collector = ListCollector(this)
            setTag(LIST_COLLECTOR_KEY, collector)
        }
        return collector
    }

/**
 * `Flow<ListData<T>>`的值发射给[listCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.onEach { adapter.listCollector.emit(it) }
 *
 * // 简化上面的写法
 * flow.onEach(adapter.listCollector)
 * ```
 */
fun <T : Any> Flow<ListData<T>>.onEach(
    collector: ListCollector<T>
): Flow<ListData<T>> = onEach(collector::emit)

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
@Deprecated(
    message = "虽然简化了代码，但是降低了可读性",
    replaceWith = ReplaceWith(expression = "onEach(adapter.listCollector)")
)
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
@Deprecated(
    message = "虽然简化了代码，但是降低了可读性",
    replaceWith = ReplaceWith(expression = "collect(adapter.listCollector)")
)
suspend fun <T : Any> Flow<ListData<T>>.collect(
    adapter: ListAdapter<T, *>
): Unit = collect(adapter.listCollector)

/**
 * 列表数据收集器，负责收集指定流的[ListData]
 */
class ListCollector<T : Any> internal constructor(
    private val adapter: ListAdapter<T, *>,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : FlowCollector<ListData<T>> {
    private var version = 0
    private var mediator: ListMediator<T>? = null

    init {
        adapter.addListExecuteListener { op ->
            mediator?.updateList(op)
            version = mediator?.version ?: 0
        }
    }

    override suspend fun emit(
        value: ListData<T>
    ): Unit = withContext(mainDispatcher.immediate) {
        if (mediator !== value.mediator) {
            version = 0
            mediator = value.mediator
        }
        value.flow.collect { event ->
            val newVersion = event.version
            if (newVersion <= version) return@collect
            adapter.awaitUpdateList(event.op, dispatch = false)
            // 更新列表完成后才保存版本号
            version = newVersion
        }
    }
}