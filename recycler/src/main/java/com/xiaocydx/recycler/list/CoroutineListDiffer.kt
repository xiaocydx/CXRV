package com.xiaocydx.recycler.list

import androidx.annotation.MainThread
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.xiaocydx.recycler.extension.reverseAccessEach
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.coroutines.*

/**
 * 列表更新帮助类，可以计算两个列表的差异，并根据计算结果更新列表
 *
 * ### 更新操作
 * 调用[updateList]或[updateListAwait]，传入更新操作更新列表，
 * [updateListAwait]会响应调用处协程的取消恢复，但不会把更新操作从队列中移除。
 * 正在执行的[UpdateOp.SubmitList]不会响应`cancel()`停止计算，但在计算完成后不会更新列表。
 *
 * ### 更新队列
 * 调用[updateList]时，若[CoroutineListDiffer]正在执行[UpdateOp.SubmitList]，
 * 则会将此次更新操作加入队列，在执行完[UpdateOp.SubmitList]后按队列顺序更新列表。
 * 通过[CoroutineListDiffer.cancel]或者[CoroutineListDiffer.cancelChildren]可以清除队列。
 *
 * @author xcc
 * @date 2021/12/9
 */
class CoroutineListDiffer<T : Any>(
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val updateCallback: ListUpdateCallback,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) : Continuation<Any?>, CoroutineScope {
    private val mutex = Mutex()
    private var sourceList: MutableList<T> = mutableListOf()
    private var executeListeners: ArrayList<ListExecuteListener<T>>? = null
    private var changedListeners: ArrayList<ListChangedListener<T>>? = null
    override val context: CoroutineContext = SupervisorJob() + mainDispatcher.immediate
    override val coroutineContext: CoroutineContext = context
    var currentList: List<T> = sourceList.toReadOnlyList()
        private set

    /**
     * 更新列表
     *
     * ```
     * val differ: CoroutineListDiffer<Any> = ...
     * val op = UpdateOp.SubmitList(newList)
     * differ.updateList(op) {
     *    // 此时列表数据已修改、列表更新操作已执行
     * }
     * ```
     * @param dispatch 是否将更新操作分发给[ListExecuteListener]
     */
    @Suppress("UNCHECKED_CAST")
    fun updateList(
        op: UpdateOp<T>,
        dispatch: Boolean = true,
        complete: ((exception: Throwable?) -> Unit)? = null
    ) = runOnMainThread {
        if (isLockNeeded(op)) {
            val job = launch {
                mutex.withLock { execute(op, dispatch) }
            }
            if (complete != null) {
                job.invokeOnCompletion { complete.invoke(it) }
            }
        } else {
            // 该分支下调用execute()不会产生挂起，
            // 因此直接执行execute()的状态机逻辑。
            execute(this, op as UpdateOp<Any>, dispatch, this)
            complete?.invoke(null)
        }
    }

    /**
     * 更新列表并等待完成
     *
     * **注意**：该函数会响应调用处协程的取消恢复，但不会把更新操作从队列中移除，
     * 只能通过[CoroutineListDiffer.cancel]或者[CoroutineListDiffer.cancelChildren]清除队列。
     *
     * ```
     * val differ: CoroutineListDiffer<Any> = ...
     * val op = UpdateOp.SubmitList(newList)
     * scope.launch {
     *    differ.updateListAwait(op)
     *    // 此时列表数据已修改、列表更新操作已执行
     * }
     * ```
     * @param dispatch 是否将更新操作分发给[ListExecuteListener]
     */
    suspend fun updateListAwait(op: UpdateOp<T>, dispatch: Boolean = true) {
        suspendCancellableCoroutine<Unit> { continuation ->
            updateList(op, dispatch) { exception ->
                if (exception == null) {
                    continuation.resume(Unit)
                } else {
                    continuation.resumeWithException(exception)
                }
            }
        }
    }

    override fun resumeWith(result: Result<Any?>) {
        // 不做任何处理
    }

    @MainThread
    private suspend fun execute(op: UpdateOp<T>, dispatch: Boolean) {
        if (dispatch) {
            executeListeners?.reverseAccessEach { it.onExecute(op) }
        }
        when (op) {
            is UpdateOp.SubmitList -> submitList(op.newList)
            is UpdateOp.SetItem -> setItem(op.position, op.item)
            is UpdateOp.AddItem -> addItem(op.position, op.item)
            is UpdateOp.AddItems -> addItems(op.position, op.items)
            is UpdateOp.RemoveItemAt -> removeItemAt(op.position)
            is UpdateOp.SwapItem -> swapItem(op.fromPosition, op.toPosition)
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
        if (newList === oldList) {
            return false
        }
        if (oldList.isEmpty() || newList.isEmpty()) {
            return false
        }
        return true
    }

    /**
     * 当提交新列表，并将更新操作分发给[ListExecuteListener]时:
     * ### [newList]是[MutableList]类型
     * [CoroutineListDiffer]中的sourceList直接赋值替换为[newList]，
     * [ListUpdater]中的sourceList通过[addAll]更新为[newList]，
     * 整个过程仅[ListUpdater]的[addAll]copy了一次数组。
     *
     * ### [newList]不是[MutableList]
     * [CoroutineListDiffer]中的sourceList通过创建[MutableList]更新为[newList]，
     * [ListUpdater]中的sourceList通过[addAll]更新为[newList]，
     * 整个过程[CoroutineListDiffer]创建[MutableList]和[ListUpdater]的[addAll]copy了两次数组。
     */
    @MainThread
    private suspend fun submitList(newList: List<T>) {
        val oldList = sourceList
        if (newList === oldList) {
            return
        }
        when {
            oldList.isNotEmpty() && newList.isEmpty() -> {
                val count = oldList.size
                sourceList.clear()
                updateCallback.onRemoved(0, count)
            }
            oldList.isEmpty() && newList.isNotEmpty() -> {
                sourceList = newList.ensureMutable()
                currentList = sourceList.toReadOnlyList()
                updateCallback.onInserted(0, newList.size)
            }
            else -> {
                val result: DiffUtil.DiffResult = withContext(workDispatcher) {
                    oldList.calculateDiff(newList, diffCallback)
                }
                sourceList = newList.ensureMutable()
                currentList = sourceList.toReadOnlyList()
                result.dispatchUpdatesTo(updateCallback)
            }
        }
    }

    @MainThread
    private fun setItem(position: Int, item: T) {
        val oldItem = sourceList.getOrNull(position) ?: return
        sourceList[position] = item
        var payload: Any? = null
        if (oldItem !== item && diffCallback.areItemsTheSame(oldItem, item)) {
            if (diffCallback.areContentsTheSame(oldItem, item)) {
                return
            } else {
                payload = diffCallback.getChangePayload(oldItem, item)
            }
        }
        updateCallback.onChanged(position, 1, payload)
    }

    @MainThread
    private fun addItem(position: Int, item: T) {
        if (position !in 0..sourceList.size) {
            return
        }
        sourceList.add(position, item)
        updateCallback.onInserted(position, 1)
    }

    @MainThread
    private fun addItems(position: Int, items: List<T>) {
        if (position !in 0..sourceList.size) {
            return
        }
        sourceList.addAll(position, items)
        updateCallback.onInserted(position, items.size)
    }

    @MainThread
    private fun removeItemAt(position: Int) {
        if (position !in sourceList.indices) {
            return
        }
        sourceList.removeAt(position)
        updateCallback.onRemoved(position, 1)
    }

    @MainThread
    private fun swapItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition !in sourceList.indices
                || toPosition !in sourceList.indices) {
            return
        }
        Collections.swap(sourceList, fromPosition, toPosition)
        updateCallback.onMoved(fromPosition, toPosition)
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

    fun cancelChildren() {
        coroutineContext.cancelChildren()
    }

    private inline fun runOnMainThread(crossinline block: () -> Unit) {
        val dispatcher = mainDispatcher.immediate
        if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            dispatcher.dispatch(EmptyCoroutineContext) { block() }
        } else {
            block()
        }
    }

    private companion object {
        @Suppress("UNCHECKED_CAST")
        private val execute =
                CoroutineListDiffer<Any>::execute as Function4<Any, UpdateOp<Any>, Boolean, Continuation<Unit>, Any?>
    }
}