package com.xiaocydx.recycler.list

/**
 * 列表更新操作
 *
 * @author xcc
 * @date 2021/11/2
 */
sealed class UpdateOp<out T : Any> {

    class SubmitList<T : Any>(val newList: List<T>) : UpdateOp<T>()

    class SetItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    class AddItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    class AddItems<T : Any>(val position: Int, val items: List<T>) : UpdateOp<T>()

    class RemoveItemAt<T : Any>(val position: Int) : UpdateOp<T>()

    class SwapItem<T : Any>(val fromPosition: Int, val toPosition: Int) : UpdateOp<T>()
}