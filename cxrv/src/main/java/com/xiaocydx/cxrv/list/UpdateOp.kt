package com.xiaocydx.cxrv.list

/**
 * 列表更新操作
 *
 * @author xcc
 * @date 2021/11/2
 */
sealed class UpdateOp<in T : Any> {
    internal data class SubmitList<T : Any>(val newList: List<T>) : UpdateOp<T>()

    internal data class SetItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    internal data class SetItems<T : Any>(val position: Int, val items: List<T>) : UpdateOp<T>()

    internal data class AddItem<T : Any>(val position: Int, val item: T) : UpdateOp<T>()

    internal data class AddItems<T : Any>(val position: Int, val items: List<T>) : UpdateOp<T>()

    internal data class RemoveItems<T : Any>(val position: Int, val itemCount: Int) : UpdateOp<T>()

    internal data class SwapItem<T : Any>(val fromPosition: Int, val toPosition: Int) : UpdateOp<T>()
}