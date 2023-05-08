/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.cxrv.list

import androidx.annotation.MainThread
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.internal.reverseAccessEach
import com.xiaocydx.cxrv.internal.swap
import com.xiaocydx.cxrv.internal.toUnmodifiableList
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import java.util.*
import kotlin.coroutines.*

/**
 * 列表更新帮助类，计算两个列表的差异，并根据计算结果更新列表
 *
 * ### 更新操作
 * 调用[updateList]传入[UpdateOp]更新列表，返回更新结果[UpdateResult]，
 * 若传入的[UpdateOp]是[UpdateOp.SubmitList]，则取消正在执行和挂起中的[UpdateOp]，
 * 若传入的[UpdateOp]不是[UpdateOp.SubmitList]，则立即被执行或者进入更新队列等待被执行。
 *
 * ### 更新取消
 * 调用[CoroutineListDiffer.cancel]能取消挂起中的[UpdateOp]，不能停止正在执行的[UpdateOp.SubmitList]，
 * 原因是[DiffUtil.calculateDiff]的计算逻辑无法响应取消，虽然不能停止计算，但是能在计算完成后不更新列表。
 *
 * ### 更新回调
 * 调用[addListChangedListener]，可以添加列表已更改的[ListChangedListener]，
 * 当[ListChangedListener.onListChanged]被调用时，表示列表数据修改完成、[UpdateOp]执行完成。
 * 调用[addListExecuteListener]，可以添加执行[UpdateOp]的[ListExecuteListener]，
 * 当[ListExecuteListener.onExecute]被调用时，表示开始执行[UpdateOp]，该监听用于构建双向通信。
 *
 * @author xcc
 * @date 2021/12/9
 */
class CoroutineListDiffer<T : Any>(
    private val diffCallback: DiffUtil.ItemCallback<T>,
    private val updateCallback: ListUpdateCallback,
    private var diffDispatcher: CoroutineDispatcher = Dispatchers.Default,
    val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) {
    private val runner = SingleRunner()
    private val scope = CoroutineScope(SupervisorJob() + mainDispatcher.immediate)
    private var sourceList: ArrayList<T> = arrayListOf()
    private var executeListeners: ArrayList<ListExecuteListener<T>>? = null
    private var changedListeners: ArrayList<ListChangedListener<T>>? = null
    @Volatile var currentList: List<T> = sourceList.toUnmodifiableList(); private set

    constructor(
        diffCallback: DiffUtil.ItemCallback<T>,
        adapter: RecyclerView.Adapter<*>,
        diffDispatcher: CoroutineDispatcher = Dispatchers.Default,
        mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
    ) : this(diffCallback, AdapterListUpdateCallback(adapter), diffDispatcher, mainDispatcher)

    /**
     * 设置差异计算的工作线程调度器
     *
     * 若[dispatcher]等于[mainDispatcher]，则在主线程执行差异计算，这种做法的实际意义：
     * 当调度器调度较慢时，会导致差异计算较慢执行（工作线程）、更新列表较慢执行（主线程），
     * 在列表数据量或修改量不大的情况下，可以选择在主线程执行差异计算，不进行任何的调度。
     */
    fun setDiffDispatcher(dispatcher: CoroutineDispatcher) {
        assertMainThread()
        if (diffDispatcher === dispatcher) return
        cancel()
        diffDispatcher = dispatcher
    }

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
     *         // op可能被取消
     *    }
     * }
     * ```
     * @param dispatch 是否将[op]分发给[ListExecuteListener]
     */
    @Deprecated(
        message = "合并更新列表函数，去除complete参数，提供UpdateResult",
        replaceWith = ReplaceWith("updateList(op, dispatch).await()")
    )
    fun updateList(
        op: UpdateOp<T>,
        dispatch: Boolean = true,
        complete: ((exception: Throwable?) -> Unit)? = null
    ) {
        updateList(op, dispatch).let { it as? CompleteCompat }
            ?.invokeOnCompletion(complete) ?: complete?.invoke(null)
    }

    /**
     * 更新列表并等待完成
     *
     * **注意**：该函数会响应调用处协程的取消，抛出[CancellationException]，但不会取消[op]，
     * 调用[CoroutineListDiffer.cancel]能取消挂起中的[UpdateOp]，不能停止正在执行的[UpdateOp.SubmitList]，
     * 原因是[DiffUtil.calculateDiff]的计算逻辑无法响应取消，虽然不能停止计算，但是能在计算完成后不更新列表。
     *
     * ```
     * val differ: CoroutineListDiffer<Any> = ...
     * val op = UpdateOp.SubmitList(newList)
     * scope.launch {
     *    differ.awaitUpdateList(op)
     *    // 此时列表已更新、数据已修改
     * }
     * ```
     * @param dispatch 是否将[op]分发给[ListExecuteListener]
     */
    @Deprecated(
        message = "合并更新列表函数，去除complete参数，提供UpdateResult",
        replaceWith = ReplaceWith("updateList(op, dispatch).await()")
    )
    suspend fun awaitUpdateList(op: UpdateOp<T>, dispatch: Boolean = true) {
        updateList(op, dispatch).await()
    }

    /**
     * 更新列表
     *
     * **注意**：[UpdateResult.await]会响应调用处协程的取消，抛出[CancellationException]，但不会取消[op]，
     * 调用[CoroutineListDiffer.cancel]能取消挂起中的[UpdateOp]，不能停止正在执行的[UpdateOp.SubmitList]，
     * 原因是[DiffUtil.calculateDiff]的计算逻辑无法响应取消，虽然不能停止计算，但是能在计算完成后不更新列表。
     *
     * ```
     * val differ: CoroutineListDiffer<Any> = ...
     * val op = UpdateOp.SubmitList(newList)
     * scope.launch {
     *    differ.updateList(op).await()
     *    // 此时列表已更新、数据已修改
     * }
     * ```
     * @param dispatch 是否将[op]分发给[ListExecuteListener]
     */
    fun updateList(op: UpdateOp<T>, dispatch: Boolean = true): UpdateResult {
        assertMainThread()
        return if (isLaunchNeeded(op)) {
            scope.launch {
                runner.afterPrevious { execute(op, dispatch) }
            }.let(::DeferredResult)
        } else {
            // 该分支下调用execute()不会产生挂起，
            // 因此直接执行execute()的状态机逻辑。
            @Suppress("UNCHECKED_CAST")
            execute(this, op as UpdateOp<Any>, dispatch, NopSymbol)
            CompleteResult
        }
    }

    /**
     * 添加列表已更改的监听
     *
     * [ListChangedListener.onListChanged]中可以调用[removeListChangedListener]。
     */
    fun addListChangedListener(listener: ListChangedListener<T>) {
        assertMainThread()
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
    fun removeListChangedListener(listener: ListChangedListener<T>) {
        assertMainThread()
        changedListeners?.remove(listener)
    }

    /**
     * 添加执行[UpdateOp]的监听
     *
     * * [ListExecuteListener.onExecute]中可以调用[removeListExecuteListener]。
     */
    fun addListExecuteListener(listener: ListExecuteListener<T>) {
        assertMainThread()
        if (executeListeners == null) {
            executeListeners = arrayListOf()
        }
        if (!executeListeners!!.contains(listener)) {
            executeListeners!!.add(listener)
        }
    }

    /**
     * 移除执行[UpdateOp]的监听
     */
    fun removeListExecuteListener(listener: ListExecuteListener<T>) {
        assertMainThread()
        executeListeners?.remove(listener)
    }

    fun cancel() {
        assertMainThread()
        // 允许调用处多次cancel
        runner.cancel()
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
    private fun isLaunchNeeded(op: UpdateOp<T>): Boolean {
        if (runner.isRunning) {
            if (op is UpdateOp.SubmitList) cancel()
            return true
        }
        if (op !is UpdateOp.SubmitList) {
            return false
        }
        // 对应submitList()的快路径
        val oldList = sourceList
        val newList = op.newList
        return when {
            oldList === newList -> false
            oldList.isEmpty() || newList.isEmpty() -> false
            else -> isDiffDispatchNeeded()
        }
    }

    @MainThread
    private suspend fun submitList(newList: List<T>): Boolean {
        val oldList = sourceList
        when {
            oldList === newList -> return false
            oldList.isEmpty() && newList.isEmpty() -> return true
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
                val result: DiffUtil.DiffResult = withDiffDispatcher {
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
        var changed = true
        val payload: Any? = when {
            // oldItem和newItem为同一个对象，确保payload为null的更新
            oldItem === newItem -> null
            !diffCallback.areItemsTheSame(oldItem, newItem) -> {
                changed = false
                null
            }
            !diffCallback.areContentsTheSame(oldItem, newItem) -> {
                diffCallback.getChangePayload(oldItem, newItem)
            }
            else -> NopSymbol
        }
        if (payload !== NopSymbol) {
            updateForSetItem(position, 1, changed, payload)
        }
        return true
    }

    @MainThread
    private fun setItems(position: Int, newItems: List<T>): Boolean {
        if (position !in sourceList.indices) return false
        val end = (position + newItems.size - 1).coerceAtMost(sourceList.lastIndex)
        var index = position
        var start = index
        var count = 0
        var changed = true
        var payload: Any? = NopSymbol
        while (index <= end) {
            val oldItem = sourceList[index]
            val newItem = newItems[index - position]
            sourceList[index] = newItem

            var newChanged = true
            val newPayload: Any? = when {
                // oldItem和newItem为同一个对象，确保payload为null的更新
                oldItem === newItem -> null
                !diffCallback.areItemsTheSame(oldItem, newItem) -> {
                    newChanged = false
                    null
                }
                !diffCallback.areContentsTheSame(oldItem, newItem) -> {
                    diffCallback.getChangePayload(oldItem, newItem)
                }
                else -> NopSymbol
            }

            if (payload !== newPayload || changed != newChanged) {
                if (count > 0) {
                    // 将累积的更新合并为一次更新
                    updateForSetItem(start, count, changed, payload)
                    count = 0
                }
                start = index
            }

            index++
            payload = newPayload
            changed = newChanged
            if (payload !== NopSymbol) count++
        }

        if (count > 0) {
            // 该分支处理两种情况：
            // 1. oldItems和newItems的payload都一致，做一次完整更新
            // 2. oldItems和newItems的payload不一致，做最后一次更新
            updateForSetItem(start, count, changed, payload)
        }
        return position <= end
    }

    @MainThread
    private fun addItem(position: Int, item: T): Boolean {
        if (position !in 0..sourceList.size) return false
        if (position == sourceList.size) {
            // ArrayList.add()相比于ArrayList.add(position, item)，
            // position等于size时，不会调用System.arraycopy()。
            sourceList.add(item)
        } else {
            sourceList.add(position, item)
        }
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
            // ArrayList.remove()相比于ArrayList.removeRange()，
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
        // 先remove再add可能会造成两次较大范围的元素搬运
        // val item = sourceList.removeAt(fromPosition)
        // sourceList.add(toPosition, item)
        if (fromPosition < toPosition) {
            // 从小到大，fromPosition两两交换至toPosition
            for (i in fromPosition until toPosition) sourceList.swap(i, i + 1)
        } else {
            // 从大到小，fromPosition两两交换至toPosition
            for (i in fromPosition downTo toPosition + 1) sourceList.swap(i, i - 1)
        }
        updateCallback.onMoved(fromPosition, toPosition)
        return true
    }

    @MainThread
    private fun updateForSetItem(position: Int, count: Int, changed: Boolean, payload: Any?) {
        if (changed) {
            updateCallback.onChanged(position, count, payload)
        } else {
            // 跟差异计算一致的更新方式
            updateCallback.onRemoved(position, count)
            updateCallback.onInserted(position, count)
        }
    }

    private fun List<T>.ensureSafeMutable(): ArrayList<T> {
        return if (this is SafeMutableList) this else ArrayList(this)
    }

    /**
     * 若[diffDispatcher]是`HandlerContext`，则进行`HandlerContext.equals()`对比，
     */
    private fun isDiffDispatchNeeded() = diffDispatcher != mainDispatcher

    private suspend inline fun <R> withDiffDispatcher(crossinline block: () -> R): R {
        return if (isDiffDispatchNeeded()) withContext(diffDispatcher) { block() } else block()
    }

    private fun assertMainThread() {
        val dispatcher = mainDispatcher.immediate
        assert(!dispatcher.isDispatchNeeded(EmptyCoroutineContext)) { "只能在主线程中调用当前函数" }
    }

    /**
     * 实际场景不需要使用[Mutex]支持多线程调用[cancel]，因此实现简易的挂起队列即可
     */
    @MainThread
    private class SingleRunner {
        private var current: Job? = null
        private var queue: LinkedList<CancellableContinuation<Unit>>? = null
        val isRunning: Boolean
            get() = current != null

        suspend fun <T> afterPrevious(block: suspend () -> T): T {
            if (current != null) {
                suspendCancellableCoroutine(::addToLast)
            }
            current = coroutineContext.job
            return try {
                block()
            } finally {
                // 协程被取消，block抛出CancellationException
                current = null
                removeFirst()?.resume(Unit)
            }
        }

        fun cancel() {
            // 先清空queue，再处理current，
            // 避免current被cancel后取下一个。
            queue?.forEach { it.cancel() }
            queue?.clear()
            current?.cancel()
            current = null
        }

        private fun addToLast(cont: CancellableContinuation<Unit>) {
            val queue = queue ?: LinkedList<CancellableContinuation<Unit>>().also { queue = it }
            queue.add(cont)
        }

        private fun removeFirst(): Continuation<Unit>? {
            val queue = queue ?: return null
            // 当queue为空时，调用removeFirst()会抛出NoSuchElementException
            return if (queue.isNotEmpty()) queue.removeFirst() else null
        }
    }

    private companion object NopSymbol : Continuation<Any?> {
        @Suppress("UNCHECKED_CAST")
        private val execute = CoroutineListDiffer<Any>::execute
                as Function4<Any, UpdateOp<Any>, Boolean, Continuation<Unit>, Any?>
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Any?>) = Unit
    }
}