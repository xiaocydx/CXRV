package com.xiaocydx.recycler.list

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.flowOnMain
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.*

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
 * 列表状态，提供[ListData]数据流和[ListOwner]
 *
 * ```
 * // 在ViewModel下创建`listState`，对外提供`flow`
 * class FooViewModel : ViewModel() {
 *     private val listState = ListState(viewModelScope)
 *     val flow = listState.flow
 * }
 *
 * // 在视图控制器下收集`viewModel.flow`
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          lifecycleScope.launch {
 *              adapter.emitAll(viewModel.flow)
 *          }
 *          // 或者仅在视图控制器活跃期间内收集`viewModel.flow`
 *          lifecycleScope.launch {
 *              repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                  adapter.emitAll(viewModel.flow)
 *              }
 *          }
 *     }
 * }
 * ```
 */
class ListState<T : Any>(
    scope: CoroutineScope,
    initList: List<T>? = null
) : ListOwner<T> {
    private val mediator = ListMediatorImpl(initList)
    override val currentList: List<T>
        get() = mediator.currentList
    val flow: Flow<ListData<T>> = ListDataStateFlow(scope, mediator.flow, mediator)

    override fun updateList(op: UpdateOp<T>) {
        mediator.updateList(op, dispatch = true)
    }

    private inner class ListMediatorImpl(
        initList: List<T>?
    ) : ListMediator<T> {
        private val updater: ListUpdater<T> =
                ListUpdater(sourceList = initList?.ensureMutable() ?: mutableListOf())
        private val channel: Channel<UpdateOp<T>> = Channel(Channel.UNLIMITED)
        override var updateVersion = 0
            private set
        override val currentList: List<T>
            get() = updater.currentList

        val flow: Flow<UpdateOp<T>> =
                channel.receiveAsFlow()
                    .onEach { updateVersion++ }
                    .flowOnMain()

        init {
            updater.setUpdatedListener {
                channel.trySend(it)
            }
        }

        override fun execute(op: UpdateOp<T>) {
            updateList(op, dispatch = false)
        }

        fun updateList(op: UpdateOp<T>, dispatch: Boolean) {
            updater.updateList(op, dispatch)
        }
    }
}

/**
 * 列表数据收集器，负责收集指定流的[ListData]
 */
class ListCollector<T : Any> internal constructor(
    private val adapter: ListAdapter<T, *>
) : FlowCollector<ListData<T>> {
    private var updateVersion = 0
    private var resumeJob: Job? = null
    private var mediator: ListMediator<T>? = null

    init {
        adapter.addListExecuteListener { op ->
            mediator?.execute(op)
        }
    }

    override suspend fun emit(
        value: ListData<T>
    ): Unit = withContext(Dispatchers.Main.immediate) {
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

    private suspend fun handleUpdateOp(op: UpdateOp<T>) {
        adapter.awaitUpdateList(op, dispatch = false)
        updateVersion = mediator?.updateVersion ?: 0
    }
}

/**
 * 列表数据的容器
 */
data class ListData<T : Any> internal constructor(
    internal val flow: Flow<UpdateOp<T>>,
    internal val mediator: ListMediator<T>
)

/**
 * 提供列表相关的访问属性、执行函数
 */
internal interface ListMediator<T : Any> {

    /**
     * 列表更新版本号
     */
    val updateVersion: Int

    /**
     * 当前列表
     */
    val currentList: List<T>

    /**
     * 执行列表更新操作
     */
    fun execute(op: UpdateOp<T>)
}

/**
 * 列表数据状态流
 */
private class ListDataStateFlow<T : Any>(
    scope: CoroutineScope,
    flow: Flow<UpdateOp<T>>,
    mediator: ListMediator<T>
) : CancellableFlow<ListData<T>>(scope) {
    private val state: ListData<T> = ListData(
        flow = CancellableFlow(scope, flow),
        mediator = mediator
    )

    override suspend fun onActive(channel: SendChannel<ListData<T>>) {
        channel.send(state)
    }
}