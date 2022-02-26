package com.xiaocydx.recycler.list

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.runOnMainThread
import java.util.*

/**
 * 列表更新器
 *
 * @author xcc
 * @date 2021/11/3
 */
class ListUpdater<T : Any>(private val sourceList: MutableList<T>) {
    private var listener: ((UpdateOp<T>) -> Unit)? = null
    val currentList: List<T> = sourceList.toReadOnlyList()

    /**
     * 更新列表
     *
     * @param dispatch 是否将更新操作分发给[listener]
     */
    fun updateList(op: UpdateOp<T>, dispatch: Boolean) = runOnMainThread {
        val succeed = when (op) {
            is UpdateOp.SubmitList -> submitList(op.newList)
            is UpdateOp.SetItem -> setItem(op.position, op.item)
            is UpdateOp.AddItem -> addItem(op.position, op.item)
            is UpdateOp.AddItems -> addItems(op.position, op.items)
            is UpdateOp.RemoveItemAt -> removeItemAt(op.position)
            is UpdateOp.SwapItem -> swapItem(op.fromPosition, op.toPosition)
        }
        if (succeed && dispatch) {
            listener?.invoke(op)
        }
    }

    /**
     * 若[ListUpdater]和[CoroutineListDiffer]构建了双向通信，
     * 则提交新列表，并将更新操作分发给[listener]时:
     * ### [newList]是[MutableList]类型
     * [ListUpdater]中的sourceList通过[addAll]更新为[newList]，
     * [CoroutineListDiffer]中的sourceList直接赋值替换为[newList]，
     * 整个过程仅[ListUpdater]的[addAll]copy一次数组。
     *
     * ### [newList]不是[MutableList]
     * [ListUpdater]中的sourceList通过[addAll]更新为[newList]，
     * [CoroutineListDiffer]中的sourceList通过创建[MutableList]更新为[newList]，
     * 整个过程[ListUpdater]的[addAll]和[CoroutineListDiffer]创建[MutableList]copy两次数组。
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

    fun setUpdatedListener(
        listener: ((UpdateOp<T>) -> Unit)?
    ) = runOnMainThread {
        this.listener = listener
    }
}