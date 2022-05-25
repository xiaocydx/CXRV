package com.xiaocydx.cxrv.itemtouch

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.R
import com.xiaocydx.cxrv.internal.accessEach

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
    private var callbacks: ArrayList<ItemTouchCallback>? = null
    private var intercepting: ItemTouchCallback? = null
    private val touchHelper = ItemTouchHelper(this)
        .apply { attachToRecyclerView(recyclerView) }

    override fun getMovementFlags(rv: RecyclerView, holder: ViewHolder): Int {
        intercepting = callbacks?.findIntercepting(holder)
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
        super.onSelectedChanged(holder, actionState)
        if (holder != null) {
            intercepting?.onSelected(holder)
        }
    }

    override fun onChildDraw(
        canvas: Canvas, rv: RecyclerView, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(canvas, rv, holder, dX, dY, actionState, isCurrentlyActive)
        intercepting?.onDraw(canvas, holder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onChildDrawOver(
        canvas: Canvas, rv: RecyclerView, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        super.onChildDrawOver(canvas, rv, holder, dX, dY, actionState, isCurrentlyActive)
        intercepting?.onDrawOver(canvas, holder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(rv: RecyclerView, holder: ViewHolder) {
        super.clearView(rv, holder)
        intercepting?.clearView(holder)
    }

    private fun ArrayList<ItemTouchCallback>.findIntercepting(holder: ViewHolder): ItemTouchCallback? {
        accessEach { if (it.onIntercept(holder)) return it }
        return null
    }

    fun addItemTouchCallback(callback: ItemTouchCallback) {
        if (callbacks == null) {
            callbacks = ArrayList(2)
        }
        if (!callbacks!!.contains(callback)) {
            callbacks!!.add(callback)
            callback.attach(touchHelper, recyclerView)
        }
    }

    fun removeItemTouchCallback(callback: ItemTouchCallback) {
        callbacks?.remove(callback)
        if (this.intercepting == callback) {
            this.intercepting = null
        }
    }
}