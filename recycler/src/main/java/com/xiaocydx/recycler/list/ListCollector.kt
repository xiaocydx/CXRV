package com.xiaocydx.recycler.list

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

private const val LIST_COLLECTOR_KEY = "com.xiaocydx.recycler.list.LIST_COLLECTOR_KEY"

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
 * 收集[flow]的所有值，并将它们发送给[listCollector]，是一种简化写法
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * flow.collect { value ->
 *     adapter.listCollector.emit(value)
 * }
 *
 * // 简化上面的写法
 * adapter.listCollector.emitAll(flow)
 *
 * // 再进行简化
 * adapter.emitAll(flow)
 * ```
 */
@Deprecated(
    message = "其它相同形式的扩展函数，导致该函数调用体验不好，因此废弃",
    replaceWith = ReplaceWith(
        expression = "flow.collect(adapter)",
        imports = ["com.xiaocydx.recycler.extension.collect"]
    )
)
suspend fun <T : Any> ListAdapter<T, *>.emitAll(
    flow: Flow<ListData<T>>
) = listCollector.emitAll(flow)

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
        mediator = value.mediator
        val mediator = value.mediator
        value.flow
            .onStart {
                if (version < mediator.version) {
                    emit(UpdateOp.SubmitList(mediator.currentList))
                }
            }
            .collect { op ->
                val newVersion = mediator.version
                if (version < newVersion) {
                    adapter.awaitUpdateList(op, dispatch = false)
                    // 更新列表完成后才保存版本号
                    version = newVersion
                }
            }
    }
}