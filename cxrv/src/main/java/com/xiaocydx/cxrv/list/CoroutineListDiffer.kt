package com.xiaocydx.cxrv.list

import androidx.annotation.MainThread
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.internal.reverseAccessEach
import com.xiaocydx.cxrv.internal.toUnmodifiableList
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.coroutines.*

/**
 * 列表更新帮助类，计算两个列表的差异，并根据计算结果更新列表
 *
 * ### 更新操作
 * 调用[updateList]或[awaitUpdateList]，传入更新操作更新列表，
 * 若正在执行[UpdateOp.SubmitList]，则执行完之后才处理传入的更新操作，
 * [awaitUpdateList]会响应调用处协程的取消，抛出[CancellationException]，但不会取消更新操作。
 *
 * ### 更新取消
 * 调用[CoroutineListDiffer.cancel]会取消挂起中的更新操作，
 * 不会停止正在执行的[UpdateOp.SubmitList]，但在执行完之后不会更新列表。
 *
 * ### 更新回调
 * 1.调用[addListChangedListener]，可以添加列表已更改的[ListChangedListener]，
 * 当[ListChangedListener.onListChanged]被调用时，表示列表数据修改完成、列表更新操作执行完成。
 * 2.调用[addListExecuteListener]，可以添加执行列表更新操作的[ListExecuteListener]，
 * 当[ListExecuteListener.onExecute]被调用时，表示开始执行列表更新操作，该监听用于构建双向通信。
 *
 * @author xcc
 * @date 2021/12/9
 */
class CoroutineListDiffer<T : Any>(
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val updateCallback: ListUpdateCallback,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher.immediate)
    private var sourceList: ArrayList<T> = arrayListOf()
    private var executeListeners: ArrayList<ListExecuteListener<T>>? = null
    private var changedListeners: ArrayList<ListChangedListener<T>>? = null
    @Volatile var currentList: List<T> = sourceList.toUnmodifiableList(); private set

    constructor(
        diffCallback: DiffUtil.ItemCallback<T>,
        adapter: RecyclerView.Adapter<*>,
        workDispatcher: CoroutineDispatcher = Dispatchers.Default,
        mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
    ) : this(diffCallback, AdapterListUpdateCallback(adapter), workDispatcher, mainDispatcher)

    /**
     * 更新列表
     *
     * ```
     * val differ: CoroutineListDiffer<Any> = ...
     * val op = UpdateOp.SubmitList(newList)
     * differ.updateList(op) { exception ->
     *    if(exception == null) {
     *         // 此时列表已更新、数据已修改
     *    } else {
     *         // 更新操作可能被取消
     *    }
     * }
     * ```
     * @param dispatch 是否将更新操作分发给[ListExecuteListener]
     */
    fun updateList(
        op: UpdateOp<T>,
        dispatch: Boolean = true,
        complete: ((exception: Throwable?) -> Unit)? = null
    ) = runOnMainThread {
        if (isLockNeeded(op)) {
            val job = scope.launch {
                mutex.withLock { execute(op, dispatch) }
            }
            complete?.let(job::invokeOnCompletion)
        } else {
            // 该分支下调用execute()不会产生挂起，
            // 因此直接执行execute()的状态机逻辑。
            @Suppress("UNCHECKED_CAST")
            execute(this, op as UpdateOp<Any>, dispatch, NopSymbol)
            complete?.invoke(null)
        }
    }

    /**
     * 更新列表并等待完成
     *
     * **注意**：该函数会响应调用处协程的取消，抛出[CancellationException]，
     * 但不会取消更新操作，调用[CoroutineListDiffer.cancel]，会取消挂起中的更新操作，
     * 不会停止正在执行的[UpdateOp.SubmitList]，但在执行完之后不会更新列表。
     *
     * ```
     * val differ: CoroutineListDiffer<Any> = ...
     * val op = UpdateOp.SubmitList(newList)
     * scope.launch {
     *    differ.awaitUpdateList(op)
     *    // 此时列表已更新、数据已修改
     * }
     * ```
     * @param dispatch 是否将更新操作分发给[ListExecuteListener]
     */
    suspend fun awaitUpdateList(
        op: UpdateOp<T>,
        dispatch: Boolean = true
    ) = withMainDispatcher {
        if (isLockNeeded(op)) {
            scope.launch {
                mutex.withLock { execute(op, dispatch) }
            }.join()
        } else {
            // 该分支下调用execute()不会产生挂起，
            // 因此直接执行execute()的状态机逻辑。
            execute(op, dispatch)
        }
    }

    @MainThread
    private suspend fun execute(op: UpdateOp<T>, dispatch: Boolean) {
        if (dispatch) {
            executeListeners?.reverseAccessEach { it.onExecute(op) }
        }
        when (op) {
            is UpdateOp.SubmitList -> submitList(op.newList)
            is UpdateOp.SetItem -> setItem(op.position, op.item)
            is UpdateOp.SetItems -> setItems(op.position, op.items)
            is UpdateOp.AddItem -> addItem(op.position, op.item)
            is UpdateOp.AddItems -> addItems(op.position, op.items)
            is UpdateOp.RemoveItems -> removeItems(op.position, op.itemCount)
            is UpdateOp.MoveItem -> moveItem(op.fromPosition, op.toPosition)
        }
        changedListeners?.reverseAccessEach { it.onListChanged(currentList) }
    }

    @MainThread
    private fun isLockNeeded(op: UpdateOp<T>): Boolean {
        if (mutex.isLocked) {
            return true
        }
        if (op !is UpdateOp.SubmitList) {
            return false
        }
        // 对应submitList()的快路径
        val oldList = sourceList
        val newList = op.newList
        if (oldList === newList) {
            return false
        }
        if (oldList.isEmpty() || newList.isEmpty()) {
            return false
        }
        return true
    }

    @MainThread
    private suspend fun submitList(newList: List<T>): Boolean {
        val oldList = sourceList
        when {
            oldList === newList -> return false
            oldList.isEmpty() && newList.isEmpty() -> return false
            oldList.isNotEmpty() && newList.isEmpty() -> {
                val count = oldList.size
                sourceList.clear()
                updateCallback.onRemoved(0, count)
            }
            oldList.isEmpty() && newList.isNotEmpty() -> {
                sourceList.addAll(newList)
                updateCallback.onInserted(0, newList.size)
            }
            else -> {
                // 若无法确保newList是安全的，则提前对其进行copy
                val safeList = newList.ensureSafeMutable()
                val result: DiffUtil.DiffResult = withContext(workDispatcher) {
                    oldList.calculateDiff(safeList, diffCallback)
                }
                sourceList = safeList
                currentList = sourceList.toUnmodifiableList()
                result.dispatchUpdatesTo(updateCallback)
            }
        }
        return true
    }

    @MainThread
    private fun setItem(position: Int, newItem: T): Boolean {
        val oldItem = sourceList.getOrNull(position) ?: return false
        sourceList[position] = newItem
        val payload = getChangePayload(oldItem, newItem)
        if (payload !== NopSymbol) {
            updateCallback.onChanged(position, 1, payload)
        }
        return true
    }

    @MainThread
    @Suppress("SuspiciousEqualsCombination")
    private fun setItems(position: Int, newItems: List<T>): Boolean {
        if (position !in sourceList.indices) return false
        val end = (position + newItems.size - 1).coerceAtMost(sourceList.lastIndex)
        var index = position
        var start = index
        var count = 0
        var payload: Any? = NopSymbol
        while (index <= end) {
            val oldItem = sourceList[index]
            val newItem = newItems[index - position]
            sourceList[index] = newItem

            val newPayload = getChangePayload(oldItem, newItem)
            if (newPayload === NopSymbol) {
                index++
                continue
            }

            if (payload !== NopSymbol && payload != newPayload) {
                // payload不等于初始值和newPayload，将之前累积的更新合并为一次更新
                updateCallback.onChanged(start, count, payload)
                count = 0
                start = index
            }

            count++
            index++
            payload = newPayload
        }

        if (payload !== NopSymbol) {
            // 该分支处理两种情况：
            // 1. oldItems和newItems的payload都一致，做一次完整更新
            // 2. oldItems和newItems的payload不一致，做最后一次更新
            updateCallback.onChanged(start, count, payload)
            return true
        }
        return position <= end
    }

    @MainThread
    private fun addItem(position: Int, item: T): Boolean {
        if (position !in 0..sourceList.size) return false
        sourceList.add(position, item)
        updateCallback.onInserted(position, 1)
        return true
    }

    @MainThread
    private fun addItems(position: Int, items: List<T>): Boolean {
        if (position !in 0..sourceList.size) return false
        sourceList.addAll(position, items)
        updateCallback.onInserted(position, items.size)
        return true
    }

    @MainThread
    @Suppress("UnnecessaryVariable")
    private fun removeItems(position: Int, itemCount: Int): Boolean {
        if (position !in sourceList.indices || itemCount <= 0) return false
        if (itemCount == 1) {
            // ArrayList.removeAt()相比于ArrayList.removeRange()，
            // 移除的是最后一位元素时，不会调用System.arraycopy()。
            sourceList.removeAt(position)
            updateCallback.onRemoved(position, 1)
            return true
        }
        val fromIndex = position
        val toIndex = (fromIndex + itemCount).coerceAtMost(sourceList.size)
        // 调用链SubList.clear() -> ArrayList.removeRange()
        sourceList.subList(fromIndex, toIndex).clear()
        updateCallback.onRemoved(position, toIndex - fromIndex)
        return true
    }

    @MainThread
    private fun moveItem(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition !in sourceList.indices
                || toPosition !in sourceList.indices) {
            return false
        }
        val item = sourceList.removeAt(fromPosition)
        sourceList.add(toPosition, item)
        updateCallback.onMoved(fromPosition, toPosition)
        return true
    }

    @MainThread
    private fun getChangePayload(oldItem: T, newItem: T): Any? {
        if (oldItem !== newItem && diffCallback.areItemsTheSame(oldItem, newItem)) {
            if (diffCallback.areContentsTheSame(oldItem, newItem)) return NopSymbol
            return diffCallback.getChangePayload(oldItem, newItem)
        }
        // oldItem和newItem为同一个对象，返回null确保更新
        return null
    }

    private fun List<T>.ensureSafeMutable(): ArrayList<T> {
        return if (this is SafeMutableList) this else ArrayList(this)
    }

    /**
     * 添加列表已更改的监听
     *
     * [ListChangedListener.onListChanged]中可以调用[removeListChangedListener]。
     */
    fun addListChangedListener(
        listener: ListChangedListener<T>
    ) = runOnMainThread {
        if (changedListeners == null) {
            changedListeners = arrayListOf()
        }
        if (!changedListeners!!.contains(listener)) {
            changedListeners!!.add(listener)
        }
    }

    /**
     * 移除列表已更改的监听
     */
    fun removeListChangedListener(
        listener: ListChangedListener<T>
    ) = runOnMainThread {
        changedListeners?.remove(listener)
    }

    /**
     * 添加执行列表更新操作的监听
     *
     * * [ListExecuteListener.onExecute]中可以调用[removeListExecuteListener]。
     */
    fun addListExecuteListener(
        listener: ListExecuteListener<T>
    ) = runOnMainThread {
        if (executeListeners == null) {
            executeListeners = arrayListOf()
        }
        if (!executeListeners!!.contains(listener)) {
            executeListeners!!.add(listener)
        }
    }

    /**
     * 移除执行列表更新操作的监听
     */
    fun removeListExecuteListener(
        listener: ListExecuteListener<T>
    ) = runOnMainThread {
        executeListeners?.remove(listener)
    }

    fun cancel() {
        // 允许调用处多次cancel
        scope.coroutineContext.cancelChildren()
    }

    private inline fun runOnMainThread(crossinline block: () -> Unit) {
        val dispatcher = mainDispatcher.immediate
        if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            dispatcher.dispatch(EmptyCoroutineContext) { block() }
        } else {
            block()
        }
    }

    private suspend inline fun withMainDispatcher(crossinline block: suspend () -> Unit) {
        val dispatcher = mainDispatcher.immediate
        if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            withContext(dispatcher) { block() }
        } else {
            block()
        }
    }

    private companion object NopSymbol : Continuation<Any?> {
        @Suppress("UNCHECKED_CAST")
        private val execute =
                CoroutineListDiffer<Any>::execute as Function4<Any, UpdateOp<Any>, Boolean, Continuation<Unit>, Any?>
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Any?>) = Unit
    }
}