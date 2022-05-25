package com.xiaocydx.cxrv.list

/**
 * 列表更新操作
 *
 * @author xcc
 * @date 2021/11/2
 */
sealed class UpdateOp<out T : Any> {

    data class SubmitList<T : Any>(val newList: List<T>) : UpdateOp<T>()

    data class SetItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    data class AddItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    data class AddItems<T : Any>(val position: Int, val items: List<T>) : UpdateOp<T>()

    data class RemoveItemAt<T : Any>(val position: Int) : UpdateOp<T>()

    data class SwapItem<T : Any>(val fromPosition: Int, val toPosition: Int) : UpdateOp<T>()
}