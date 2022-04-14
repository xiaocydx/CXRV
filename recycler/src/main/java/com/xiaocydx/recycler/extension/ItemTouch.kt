package com.xiaocydx.recycler.extension

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.doOnAttach
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.list.swapItem
import com.xiaocydx.recycler.touch.ItemTouchCallback
import com.xiaocydx.recycler.touch.ItemTouchScope
import com.xiaocydx.recycler.touch.itemTouchDispatcher

/**
 * 设置item触摸回调
 *
 * **注意**：[ItemTouchCallback]是[ItemTouchHelper.Callback]的简化类，
 * 仅用于简化一般业务场景的模板代码，若对item触摸效果有更精细的要求，
 * 则自行创建[ItemTouchHelper]，完成[ItemTouchHelper.Callback]的配置。
 */
fun <T : RecyclerView> T.addItemTouchCallback(callback: ItemTouchCallback): T {
    itemTouchDispatcher.addItemTouchCallback(callback)
    return this
}

/**
 * 移除item触摸回调
 */
fun <T : RecyclerView> T.removeItemTouchCallback(callback: ItemTouchCallback): T {
    itemTouchDispatcher.removeItemTouchCallback(callback)
    return this
}

/**
 * item触摸，详细的属性及函数描述[ItemTouchScope]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     // 拖动时交换item
 *     onDrag { from, to ->
 *         swapItem(from, to)
 *         true
 *     }
 *     // 拖动开始时放大itemView
 *     onSelected { holder ->
 *         holder.itemView.scaleX = 1.1f
 *         holder.itemView.scaleY = 1.1f
 *     ｝
 *     // 拖动结束时恢复itemView
 *     clearView { holder ->
 *         holder.itemView.scaleX = 1.0f
 *         holder.itemView.scaleY = 1.0f
 *     ｝
 * }
 * ```
 */
inline fun <AdapterT, VH, RV> RV.itemTouch(
    adapter: AdapterT,
    block: ItemTouchScope<AdapterT, VH>.() -> Unit
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    return addItemTouchCallback(ItemTouchScope(adapter, this).apply(block))
}

/**
 * item触摸，详细的属性及函数描述[ItemTouchScope]
 *
 * ```
 * adapter.itemTouch {
 *     // 拖动时交换item
 *     onDragSwapItem()
 *     // 拖动开始时放大itemView
 *     onSelected { holder ->
 *         holder.itemView.scaleX = 1.1f
 *         holder.itemView.scaleY = 1.1f
 *     ｝
 *     // 拖动结束时恢复itemView
 *     clearView { holder ->
 *         holder.itemView.scaleX = 1.0f
 *         holder.itemView.scaleY = 1.0f
 *     ｝
 * }
 * ```
 */
inline fun <AdapterT, ITEM, VH> AdapterT.itemTouch(
    crossinline block: ItemTouchScope<AdapterT, VH>.() -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv -> rv.itemTouch(this, block) }
    return this
}

/**
 * 拖动时交换item
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     onDragSwapItem()
 * }
 * ```
 */
fun <AdapterT, ITEM, VH> ItemTouchScope<AdapterT, VH>.onDragSwapItem()
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    onDrag { from, to ->
        swapItem(from, to)
        true
    }
}

/**
 * 侧滑时移除item
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     onSwipeRemoveItem()
 * }
 * ```
 */
fun <AdapterT, ITEM, VH> ItemTouchScope<AdapterT, VH>.onSwipeRemoveItem()
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    onSwipe { position, _ -> removeItemAt(position) }
}