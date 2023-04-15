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
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.holder
import com.xiaocydx.cxrv.internal.RV_HIDE_MARKER
import com.xiaocydx.cxrv.internal.RvDslMarker
import com.xiaocydx.cxrv.internal.isTouched

/**
 * 比[ItemTouchCallback]更进一步的简化类，用于简化触摸回调的配置流程，
 * 提供了[disallowDrag]、[disallowSwipe]、[startDragView]等简化函数。
 *
 * @author xcc
 * @date 2022/4/14
 */
@RvDslMarker
@Suppress("UNCHECKED_CAST", "NEWER_VERSION_IN_SINCE_KOTLIN")
class ItemTouchScope<AdapterT : Adapter<out VH>, VH : ViewHolder>
@PublishedApi internal constructor(
    private val adapter: AdapterT,
    private val rv: RecyclerView
) : ItemTouchCallback() {
    private var handler: StartDragHandler? = null
    private var dragFlags: (AdapterT.(holder: VH) -> Int)? = null
    private var swipeFlags: (AdapterT.(holder: VH) -> Int)? = null
    private var onMove: (AdapterT.(from: Int, to: Int) -> Boolean)? = null
    private var onSwiped: (AdapterT.(position: Int, direction: Int) -> Unit)? = null
    private var onSelected: (AdapterT.(holder: VH) -> Unit)? = null
    private var onDraw: OnDraw<AdapterT, VH>? = null
    private var onDrawOver: OnDraw<AdapterT, VH>? = null
    private var clearView: (AdapterT.(holder: VH) -> Unit)? = null

    /**
     * 获取拖动的移动标志
     *
     * [block]的返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止拖动，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     */
    fun dragFlags(block: AdapterT.(holder: VH) -> Int) {
        dragFlags = block
    }

    /**
     * 获取侧滑的移动标志
     *
     * [block]的返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止侧滑，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     */
    fun swipeFlags(block: AdapterT.(holder: VH) -> Int) {
        swipeFlags = block
    }

    /**
     * 触发开始拖动的View
     *
     * [ACTION_DOWN]触摸到View就开始拖动，不用通过长按itemView触发拖动，
     * [withLongPress]为`true`表示继续启用长按拖动，`false`表示禁用长按拖动。
     */
    fun startDragView(withLongPress: Boolean = false, block: AdapterT.(holder: VH) -> View) {
        isLongPressDragEnabled = withLongPress
        handler?.let(rv::removeOnItemTouchListener)
        handler = StartDragHandler(block).also(rv::addOnItemTouchListener)
    }

    /**
     * 对应[Callback.onMove]
     *
     * 将[ViewHolder]形参简化为[ViewHolder.getBindingAdapterPosition]。
     */
    fun onDrag(block: AdapterT.(from: Int, to: Int) -> Boolean) {
        onMove = block
    }

    /**
     * 对应[Callback.onSwiped]
     *
     * 将[ViewHolder]形参简化为[ViewHolder.getBindingAdapterPosition]。
     */
    fun onSwipe(block: AdapterT.(position: Int, direction: Int) -> Unit) {
        onSwiped = block
    }

    /**
     * 选中回调
     *
     * 对应[Callback.onSelectedChanged]，可以看作是触摸开始。
     */
    fun onSelected(block: AdapterT.(holder: VH) -> Unit) {
        onSelected = block
    }

    /**
     * 绘制回调，在itemView之下绘制内容
     *
     * 对应[Callback.onChildDraw]，可以看作是触摸中。
     */
    fun onDraw(block: OnDraw<AdapterT, VH>) {
        onDraw = block
    }

    /**
     * 绘制回调，在itemView之上绘制内容
     *
     * 对应[Callback.onChildDrawOver]，可以看作是触摸中。
     */
    fun onDrawOver(block: OnDraw<AdapterT, VH>) {
        onDrawOver = block
    }

    /**
     * 恢复在[onSelected]、[onDraw]、[onDrawOver]修改的视图状态
     *
     * 对应[Callback.clearView]，可以看作是触摸结束。
     */
    fun clearView(block: AdapterT.(holder: VH) -> Unit) {
        clearView = block
    }

    /**
     * [block]返回`true`表示禁止拖动
     */
    inline fun disallowDrag(crossinline block: AdapterT.(holder: VH) -> Boolean) {
        dragFlags { holder ->
            if (block(this, holder)) disallowFlags else defaultFlags
        }
    }

    /**
     * [block]返回`true`表示禁止侧滑
     */
    inline fun disallowSwipe(crossinline block: AdapterT.(holder: VH) -> Boolean) {
        swipeFlags { holder ->
            if (block(this, holder)) disallowFlags else defaultFlags
        }
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onIntercept(holder: ViewHolder): Boolean {
        return holder.bindingAdapter == adapter
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun getDragFlags(holder: ViewHolder): Int {
        if (onMove == null) return disallowFlags
        return dragFlags?.invoke(adapter, holder as VH) ?: defaultFlags
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun getSwipeFlags(holder: ViewHolder): Int {
        if (onSwiped == null) return disallowFlags
        return swipeFlags?.invoke(adapter, holder as VH) ?: defaultFlags
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onDrag(from: ViewHolder, to: ViewHolder): Boolean {
        val fromPosition = from.bindingAdapterPosition
        val toPosition = to.bindingAdapterPosition
        return onMove?.invoke(adapter, fromPosition, toPosition) ?: false
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onSwipe(holder: ViewHolder, direction: Int) {
        onSwiped?.invoke(adapter, holder.bindingAdapterPosition, direction)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onSelected(holder: ViewHolder) {
        super.onSelected(holder)
        onSelected?.invoke(adapter, holder as VH)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onDraw(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        super.onDraw(canvas, holder, dX, dY, actionState, isCurrentlyActive)
        onDraw?.invoke(adapter, canvas, holder as VH, dX, dY, actionState, isCurrentlyActive)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onDrawOver(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        super.onDrawOver(canvas, holder, dX, dY, actionState, isCurrentlyActive)
        onDrawOver?.invoke(adapter, canvas, holder as VH, dX, dY, actionState, isCurrentlyActive)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun clearView(holder: ViewHolder) {
        super.clearView(holder)
        clearView?.invoke(adapter, holder as VH)
    }

    private inner class StartDragHandler(
        private val block: AdapterT.(holder: VH) -> View
    ) : SimpleOnItemTouchListener() {

        override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
            if (touchHelper == null || event.actionMasked != ACTION_DOWN) {
                return false
            }
            val holder = rv.findChildViewUnder(event.x, event.y)
                ?.holder?.takeIf { it.bindingAdapter == adapter } ?: return false
            val view = block(adapter, holder as VH)
            if (view.isTouched(event.rawX, event.rawY)) {
                touchHelper?.startDrag(holder)
            }
            return false
        }
    }
}

typealias OnDraw<T, VH> = T.(canvas: Canvas, holder: VH, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) -> Unit