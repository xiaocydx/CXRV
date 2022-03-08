package com.xiaocydx.recycler.list

import androidx.annotation.MainThread
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll

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
    private var updateVersion = 0
    private var resumeJob: Job? = null
    private var mediator: ListMediator<T>? = null

    init {
        adapter.addListExecuteListener { op ->
            mediator?.updateList(op)
        }
    }

    override suspend fun emit(
        value: ListData<T>
    ): Unit = withContext(mainDispatcher.immediate) {
        mediator = value.mediator
        // 此处resumeJob若使用var局部变量，则编译后resumeJob会是ObjectRef对象，
        // 收集操作流期间，value.flow传入的FlowCollector会一直持有ObjectRef对象引用，
        // resumeJob执行完之后置空，ObjectRef对象内的Job可以被GC，但ObjectRef对象本身无法被GC。
        resumeJob = launchResumeJob(value.mediator)
        resumeJob?.invokeOnCompletion { resumeJob = null }
        value.flow.collect { op ->
            // 等待恢复任务执行完毕，才处理后续的更新操作
            resumeJob?.join()
            handleUpdateOp(op)
        }
    }

    @MainThread
    private fun CoroutineScope.launchResumeJob(mediator: ListMediator<T>): Job? {
        if (mediator.updateVersion == updateVersion) {
            return null
        }
        // 无需调度时，协程的启动恢复不是同步执行，而是通过事件循环进行恢复，
        // 因此启动模式设为UNDISPATCHED，确保在收集事件流之前执行恢复任务。
        return launch(start = CoroutineStart.UNDISPATCHED) {
            handleUpdateOp(UpdateOp.SubmitList(mediator.currentList))
        }
    }

    @MainThread
    private suspend fun handleUpdateOp(op: UpdateOp<T>) {
        adapter.awaitUpdateList(op, dispatch = false)
        // 更新列表完成后才保存版本号
        updateVersion = mediator?.updateVersion ?: 0
    }
}