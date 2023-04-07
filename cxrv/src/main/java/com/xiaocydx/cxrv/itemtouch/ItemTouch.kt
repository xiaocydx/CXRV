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

package com.xiaocydx.cxrv.itemtouch

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.*

/**
 * 设置item触摸回调
 *
 * **注意**：[ItemTouchCallback]是[ItemTouchHelper.Callback]的简化类，
 * 仅用于简化一般场景的模板代码，若对item触摸效果有更精细的要求，
 * 则自行创建[ItemTouchHelper]，完成[ItemTouchHelper.Callback]的配置。
 */
fun RecyclerView.addItemTouchCallback(callback: ItemTouchCallback) {
    itemTouchDispatcher.addItemTouchCallback(callback)
}

/**
 * 移除item触摸回调
 */
fun RecyclerView.removeItemTouchCallback(callback: ItemTouchCallback) {
    itemTouchDispatcher.removeItemTouchCallback(callback)
}

/**
 * item触摸，详细的属性及函数描述[ItemTouchScope]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     // 拖动时移动item
 *     onDrag { from, to ->
 *         moveItem(from, to)
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
inline fun <AdapterT : Adapter<out VH>, VH : ViewHolder> RecyclerView.itemTouch(
    adapter: AdapterT,
    block: ItemTouchScope<AdapterT, VH>.() -> Unit
): Disposable = ItemTouchDisposable().attach(
    rv = this,
    callback = ItemTouchScope(adapter, this).apply(block)
)

/**
 * item触摸，详细的属性及函数描述[ItemTouchScope]
 *
 * ```
 * adapter.itemTouch {
 *     // 拖动时移动item
 *     onDragMoveItem()
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
inline fun <AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder> AdapterT.itemTouch(
    crossinline block: ItemTouchScope<AdapterT, VH>.() -> Unit
): Disposable = repeatOnAttach { rv -> rv.itemTouch(this, block) }

/**
 * 拖动时移动item
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     onDragMoveItem()
 * }
 * ```
 */
fun <AdapterT, ITEM, VH> ItemTouchScope<AdapterT, VH>.onDragMoveItem()
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    onDrag { from, to ->
        moveItem(from, to)
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
@Deprecated(
    message = "早期理解错误，以为局部更新的move等同于swap",
    replaceWith = ReplaceWith("onDragMoveItem()")
)
fun <AdapterT, ITEM, VH> ItemTouchScope<AdapterT, VH>.onDragSwapItem()
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    onDragMoveItem()
}

@PublishedApi
internal class ItemTouchDisposable : Disposable {
    private var dispatcher: ItemTouchDispatcher? = null
    private var callback: ItemTouchCallback? = null
    override val isDisposed: Boolean
        get() = dispatcher == null && callback == null

    fun attach(
        rv: RecyclerView,
        callback: ItemTouchCallback
    ): Disposable {
        this.dispatcher = rv.itemTouchDispatcher
        this.callback = callback
        rv.itemTouchDispatcher.addItemTouchCallback(callback)
        return this
    }

    override fun dispose() {
        dispatcher?.removeItemTouchCallback(callback!!)
        dispatcher = null
        callback = null
    }
}