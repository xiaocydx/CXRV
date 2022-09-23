package com.xiaocydx.cxrv.list

import androidx.annotation.MainThread
import com.xiaocydx.cxrv.internal.*
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

/**
 * 列表状态，和视图控制器建立基于[ListOwner]的双向通信
 *
 * 1.在ViewModel下创建`listState`，对外提供`flow`
 * ```
 * class FooViewModel : ViewModel() {
 *     private val listState = ListState<Foo>()
 *     val flow = listState.asFlow()
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
 *          viewModel.flow
 *                 .onEach(adapter.listCollector)
 *                 .launchIn(lifecycleScope)
 *
 *          // 或者仅在视图控制器活跃期间内收集viewModel.flow
 *          lifecycleScope.launch {
 *              repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                  adapter.listCollector.emitAll(viewModel.flow)
 *              }
 *          }
 *     }
 * }
 * ```
 */
class ListState<T : Any> : ListOwner<T> {
    private var listeners: ArrayList<(UpdateOp<T>) -> Unit>? = null
    private val sourceList: ArrayList<T> = arrayListOf()
    override val currentList: List<T> = sourceList.toUnmodifiableList()
    internal var version: Int = 0
        private set

    @MainThread
    override fun updateList(op: UpdateOp<T>) {
        updateList(op, dispatch = true)
    }

    /**
     * 更新列表，该函数必须在主线程调用
     *
     * @param dispatch 是否将更新操作分发给[listeners]
     */
    @MainThread
    internal fun updateList(op: UpdateOp<T>, dispatch: Boolean) {
        assertMainThread()
        val succeed = when (op) {
            is UpdateOp.SubmitList -> submitList(op.newList)
            is UpdateOp.SetItem -> setItem(op.position, op.item)
            is UpdateOp.SetItems -> setItems(op.position, op.items)
            is UpdateOp.AddItem -> addItem(op.position, op.item)
            is UpdateOp.AddItems -> addItems(op.position, op.items)
            is UpdateOp.RemoveItems -> removeItems(op.position, op.itemCount)
            is UpdateOp.SwapItem -> swapItem(op.fromPosition, op.toPosition)
        }
        if (!succeed) return
        version++
        if (dispatch) {
            listeners?.reverseAccessEach { it(op) }
        }
    }

    @MainThread
    internal fun addUpdatedListener(listener: (UpdateOp<T>) -> Unit) {
        assertMainThread()
        if (listeners == null) {
            listeners = arrayListOf()
        }
        if (!listeners!!.contains(listener)) {
            listeners!!.add(listener)
        }
    }

    @MainThread
    internal fun removeUpdatedListener(listener: (UpdateOp<T>) -> Unit) {
        assertMainThread()
        listeners?.remove(listener)
    }

    @MainThread
    private fun submitList(newList: List<T>): Boolean {
        if (sourceList.isNotEmpty()) {
            sourceList.clear()
        }
        if (newList.isEmpty()) return true
        return sourceList.addAll(newList)
    }

    @MainThread
    private fun setItem(position: Int, newItem: T): Boolean {
        if (position !in sourceList.indices) return false
        sourceList[position] = newItem
        return true
    }

    @MainThread
    private fun setItems(position: Int, newItems: List<T>): Boolean {
        if (position !in sourceList.indices) return false
        val end = (position + newItems.size - 1).coerceAtMost(sourceList.lastIndex)
        var index = position
        while (index <= end) {
            sourceList[index] = newItems[index - position]
            index++
        }
        return true
    }

    @MainThread
    private fun addItem(position: Int, item: T): Boolean {
        if (position !in 0..sourceList.size) return false
        sourceList.add(position, item)
        return true
    }

    @MainThread
    private fun addItems(position: Int, items: List<T>): Boolean {
        if (position !in 0..sourceList.size) return false
        return sourceList.addAll(position, items)
    }

    @MainThread
    @Suppress("UnnecessaryVariable")
    private fun removeItems(position: Int, itemCount: Int): Boolean {
        if (position !in sourceList.indices || itemCount <= 0) return false
        if (itemCount == 1) {
            // ArrayList.removeAt()相比于ArrayList.removeRange()，
            // 移除的是最后一位元素时，不会调用System.arraycopy()。
            sourceList.removeAt(position)
            return true
        }
        val fromIndex = position
        val toIndex = (fromIndex + itemCount).coerceAtMost(sourceList.size)
        // 调用链SubList.clear() -> ArrayList.removeRange()
        sourceList.subList(fromIndex, toIndex).clear()
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
 * 将[ListState]转换为列表数据流
 */
fun <T : Any> ListState<T>.asFlow(): Flow<ListData<T>> = unsafeFlow {
    val mediator = ListMediatorImpl(this@asFlow)
    emit(ListData(mediator.flow, mediator))
}

@PublishedApi
internal class ListMediatorImpl<T : Any>(
    private val listState: ListState<T>
) : ListMediator<T> {
    private var collected = false
    override val version: Int
        get() = listState.version
    override val currentList: List<T>
        get() = listState.currentList

    val flow: Flow<ListEvent<T>> = callbackFlow {
        check(!collected) { "列表更新流Flow<ListEvent<*>>只能被收集一次" }
        collected = true
        // 先发射最新的列表数据和版本号，让下游判断是否需要更新
        send(ListEvent(UpdateOp.SubmitList(currentList), version))

        val listener: (UpdateOp<T>) -> Unit = {
            trySend(ListEvent(it, version))
        }
        listState.addUpdatedListener(listener)
        awaitClose { listState.removeUpdatedListener(listener) }
    }.buffer(UNLIMITED).flowOnMain()

    override fun updateList(op: UpdateOp<T>) {
        listState.updateList(op, dispatch = false)
    }
}