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

import android.graphics.Canvas
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.R
import com.xiaocydx.cxrv.list.InlineList
import com.xiaocydx.cxrv.list.accessEach
import com.xiaocydx.cxrv.list.toList

/**
 * [ItemTouchCallback]的分发器
 */
internal val RecyclerView.itemTouchDispatcher: ItemTouchDispatcher
    get() {
        var dispatcher: ItemTouchDispatcher? =
                getTag(R.id.tag_item_touch_dispatcher) as? ItemTouchDispatcher
        if (dispatcher == null) {
            dispatcher = ItemTouchDispatcher(this)
            setTag(R.id.tag_item_touch_dispatcher, dispatcher)
        }
        return dispatcher
    }

/**
 * [ItemTouchCallback]的分发器
 */
internal class ItemTouchDispatcher(
    private val recyclerView: RecyclerView
) : ItemTouchHelper.Callback() {
    private var callbacks = InlineList<ItemTouchCallback>()
    private var intercepting: ItemTouchCallback? = null
    private val touchHelper = ItemTouchHelper(this)
        .apply { attachToRecyclerView(recyclerView) }

    override fun getMovementFlags(rv: RecyclerView, holder: ViewHolder): Int {
        intercepting = callbacks.findIntercepting(holder)
        return intercepting?.getMovementFlags(holder) ?: return 0
    }

    override fun isLongPressDragEnabled(): Boolean {
        return intercepting?.isLongPressDragEnabled ?: true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return intercepting?.isItemViewSwipeEnabled ?: true
    }

    override fun onMove(rv: RecyclerView, holder: ViewHolder, target: ViewHolder): Boolean {
        val callback = intercepting ?: return false
        return if (callback.onIntercept(target)) callback.onDrag(holder, target) else false
    }

    override fun onSwiped(holder: ViewHolder, direction: Int) {
        intercepting?.onSwipe(holder, direction)
    }

    override fun onSelectedChanged(holder: ViewHolder?, actionState: Int) {
        if (holder != null) intercepting?.onSelected(holder)
    }

    override fun onChildDraw(
        canvas: Canvas, rv: RecyclerView, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        intercepting?.onDraw(canvas, holder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onChildDrawOver(
        canvas: Canvas, rv: RecyclerView, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        intercepting?.onDrawOver(canvas, holder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(rv: RecyclerView, holder: ViewHolder) {
        intercepting?.clearView(holder)
    }

    private fun InlineList<ItemTouchCallback>.findIntercepting(holder: ViewHolder): ItemTouchCallback? {
        accessEach { if (it.onIntercept(holder)) return it }
        return null
    }

    @VisibleForTesting
    fun getItemTouchCallbacks() = callbacks.toList()

    fun addItemTouchCallback(callback: ItemTouchCallback) {
        if (!callbacks.contains(callback)) {
            callbacks += callback
            callback.attach(touchHelper, recyclerView)
        }
    }

    fun removeItemTouchCallback(callback: ItemTouchCallback) {
        callbacks -= callback
        if (this.intercepting === callback) {
            this.intercepting = null
        }
    }
}