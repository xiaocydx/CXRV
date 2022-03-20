package com.xiaocydx.recycler.list

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.reverseAccessEach
import com.xiaocydx.recycler.extension.runOnMainThread
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 列表状态，和视图控制器建立基于[ListOwner]的双向通信
 *
 * 1.在ViewModel下创建`listState`，对外提供`flow`
 * ```
 * class FooViewModel : ViewModel() {
 *     private val listState = ListState()
 *     val flow = listState.asFlow(viewModelScope)
 * }
 * ```
 *
 * 2.在视图控制器下收集`viewModel.flow`
 * ```
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          lifecycleScope.launch {
 *              adapter.emitAll(viewModel.flow)
 *          }
 *          // 或者仅在视图控制器活跃期间内收集viewModel.flow
 *          lifecycleScope.launch {
 *              repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                  adapter.emitAll(viewModel.flow)
 *              }
 *          }
 *     }
 * }
 * ```
 */
class ListState<T : Any> : ListOwner<T> {
    private var listeners: ArrayList<(UpdateOp<T>) -> Unit>? = null
    private val sourceList: MutableList<T> = mutableListOf()
    override val currentList: List<T> = sourceList.toReadOnlyList()
    internal var updateVersion: Int = 0
        private set

    override fun updateList(op: UpdateOp<T>) {
        updateList(op, dispatch = true)
    }

    /**
     * 更新列表
     *
     * @param dispatch 是否将更新操作分发给[listeners]
     */
    internal fun updateList(op: UpdateOp<T>, dispatch: Boolean) = runOnMainThread {
        val succeed = when (op) {
            is UpdateOp.SubmitList -> submitList(op.newList)
            is UpdateOp.SetItem -> setItem(op.position, op.item)
            is UpdateOp.AddItem -> addItem(op.position, op.item)
            is UpdateOp.AddItems -> addItems(op.position, op.items)
            is UpdateOp.RemoveItemAt -> removeItemAt(op.position)
            is UpdateOp.SwapItem -> swapItem(op.fromPosition, op.toPosition)
        }
        if (!succeed) {
            return@runOnMainThread
        }
        updateVersion++
        if (dispatch) {
            listeners?.reverseAccessEach { it(op) }
        }
    }

    internal fun addUpdatedListener(
        listener: (UpdateOp<T>) -> Unit
    ) = runOnMainThread {
        if (listeners == null) {
            listeners = arrayListOf()
        }
        if (!listeners!!.contains(listener)) {
            listeners!!.add(listener)
        }
    }

    internal fun removeUpdatedListener(
        listener: (UpdateOp<T>) -> Unit
    ) = runOnMainThread {
        listeners?.remove(listener)
    }

    /**
     * 若[ListState]和[CoroutineListDiffer]建立了双向通信，
     * 则提交新列表，并将更新操作分发给[listeners]时:
     * ### [newList]是[MutableList]类型
     * [ListState]中的sourceList通过[addAll]更新为[newList]，
     * [CoroutineListDiffer]中的sourceList直接赋值替换为[newList]，
     * 整个过程仅[ListState]的[addAll]copy一次数组。
     *
     * ### [newList]不是[MutableList]
     * [ListState]中的sourceList通过[addAll]更新为[newList]，
     * [CoroutineListDiffer]中的sourceList通过创建[MutableList]更新为[newList]，
     * 整个过程[ListState]的[addAll]和[CoroutineListDiffer]创建[MutableList]copy两次数组。
     */
    @MainThread
    private fun submitList(newList: List<T>): Boolean {
        if (newList.isEmpty()) {
            sourceList.clear()
        } else {
            sourceList.clear()
            sourceList.addAll(newList)
        }
        return true
    }

    @MainThread
    private fun setItem(position: Int, item: T): Boolean {
        if (position !in sourceList.indices) {
            return false
        }
        sourceList[position] = item
        return true
    }

    @MainThread
    private fun addItem(position: Int, item: T): Boolean {
        if (position !in 0..sourceList.size) {
            return false
        }
        sourceList.add(position, item)
        return true
    }

    @MainThread
    private fun addItems(position: Int, items: List<T>): Boolean {
        if (position !in 0..sourceList.size) {
            return false
        }
        return sourceList.addAll(position, items)
    }

    @MainThread
    private fun removeItemAt(position: Int): Boolean {
        if (position !in sourceList.indices) {
            return false
        }
        sourceList.removeAt(position)
        return true
    }

    @MainThread
    private fun swapItem(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in sourceList.indices
                || toPosition !in sourceList.indices) {
            return false
        }
        Collections.swap(sourceList, fromPosition, toPosition)
        return true
    }
}

/**
 * 将[ListState]转换为列表更新数据流
 */
fun <T : Any> ListState<T>.asFlow(): Flow<ListData<T>> = flow {
    val mediator = ListMediatorImpl(this@asFlow)
    emit(ListData(mediator.flow, mediator))
}

private class ListMediatorImpl<T : Any>(
    private val listState: ListState<T>
) : ListMediator<T> {
    private val collected = AtomicBoolean()
    override val updateVersion: Int
        get() = listState.updateVersion
    override val currentList: List<T>
        get() = listState.currentList

    val flow: Flow<UpdateOp<T>> = flow {
        check(collected.compareAndSet(false, true)) {
            "列表更新数据流Flow<UpdateOp<*>>只能被收集一次"
        }
        val channel = Channel<UpdateOp<T>>(UNLIMITED)
        val listener: (UpdateOp<T>) -> Unit = { channel.trySend(it) }
        listState.addUpdatedListener(listener)
        try {
            emitAll(channel)
        } finally {
            listState.removeUpdatedListener(listener)
        }
    }

    override fun updateList(op: UpdateOp<T>) {
        listState.updateList(op, dispatch = false)
    }
}