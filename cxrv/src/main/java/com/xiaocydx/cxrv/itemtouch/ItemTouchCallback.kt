package com.xiaocydx.cxrv.itemtouch

import android.graphics.Canvas
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView.*

/**
 * [ItemTouchCallback]是[ItemTouchHelper.Callback]的简化类，
 * 仅定义了业务场景中常用的函数，用于简化模板代码，例如[getDragFlags]、[onDrag]等等，
 * 这些常用函数对应[ItemTouchHelper.Callback]中的同名函数，通过[onIntercept]完成回调分发。
 *
 * [onSelected]、[onDraw]、[clearView]参考自[ItemTouchUIUtil]的命名，可以看作是触摸开始、触摸中、触摸结束的回调。
 *
 * @author xcc
 * @date 2022/4/14
 */
abstract class ItemTouchCallback {
    var touchHelper: ItemTouchHelper? = null
        private set
    var recyclerView: RecyclerView? = null
        private set
    val LayoutManager.isVertical: Boolean
        get() = if (this is LinearLayoutManager) orientation == VERTICAL else false

    /**
     * 默认移动标志
     */
    val defaultFlags = -1

    /**
     * 禁止移动标志
     */
    val disallowFlags = 0

    /**
     * 对应[Callback.isLongPressDragEnabled]
     */
    open var isLongPressDragEnabled = true

    /**
     * 对应[Callback.isItemViewSwipeEnabled]
     */
    open var isItemViewSwipeEnabled = true

    /**
     * 拦截触摸回调
     *
     * 例如bindingAdapter相同时才拦截触摸回调：
     * ```
     * override fun onIntercept(holder: ViewHolder): Boolean {
     *     return holder.bindingAdapter == adapter
     * }
     * ```
     */
    abstract fun onIntercept(holder: ViewHolder): Boolean

    /**
     * 对应[Callback.getMovementFlags]，当[onIntercept]返回`true`时才被调用
     */
    internal fun getMovementFlags(holder: ViewHolder): Int {
        val dragFlags = getDragFlags(holder)
            .takeIf { it != defaultFlags } ?: getDefaultDragFlags()
        val swipeFlags = getSwipeFlags(holder)
            .takeIf { it != defaultFlags } ?: getDefaultSwipeFlags()
        return Callback.makeMovementFlags(dragFlags, swipeFlags)
    }

    /**
     * 获取拖动的移动标志，当[onIntercept]返回`true`时才被调用
     *
     * 返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止拖动，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     *
     * **注意**：若需要重写[getDragFlags]，可以参考[getDefaultDragFlags]的匹配逻辑。
     */
    open fun getDragFlags(holder: ViewHolder): Int = disallowFlags

    /**
     * 获取侧滑的移动标志，当[onIntercept]返回`true`时才被调用
     *
     * 返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止侧滑，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     *
     * **注意**：若需要重写[getSwipeFlags]，可以参考[getDefaultSwipeFlags]的匹配逻辑。
     */
    open fun getSwipeFlags(holder: ViewHolder): Int = disallowFlags

    /**
     * 对应[Callback.onMove]，当[onIntercept]返回`true`时才被调用
     */
    open fun onDrag(from: ViewHolder, to: ViewHolder): Boolean = false

    /**
     * 对应[Callback.onSwiped]，当[onIntercept]返回`true`时才被调用
     */
    open fun onSwipe(holder: ViewHolder, direction: Int) = Unit

    /**
     * 选中回调
     *
     * 对应[Callback.onSelectedChanged]，可以看作是触摸开始，当[onIntercept]返回`true`时才被调用。
     */
    open fun onSelected(holder: ViewHolder) = ItemTouchUIUtilAdapter.onSelected(holder)

    /**
     * 绘制回调，在itemView之下绘制内容
     *
     * 对应[Callback.onChildDraw]，可以看作是触摸中，当[onIntercept]返回`true`时才被调用。
     */
    open fun onDraw(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) = ItemTouchUIUtilAdapter.onDraw(canvas, holder, dX, dY, actionState, isCurrentlyActive)

    /**
     * 绘制回调，在itemView之上绘制内容
     *
     * 对应[Callback.onChildDrawOver]，可以看作是触摸中，当[onIntercept]返回`true`时才被调用。
     */
    open fun onDrawOver(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) = ItemTouchUIUtilAdapter.onDrawOver(canvas, holder, dX, dY, actionState, isCurrentlyActive)

    /**
     * 恢复在[onSelected]、[onDraw]、[onDrawOver]修改的视图状态
     *
     * 对应[Callback.clearView]，可以看作是触摸结束，当[onIntercept]返回`true`时才被调用。
     */
    open fun clearView(holder: ViewHolder) = ItemTouchUIUtilAdapter.clearView(holder)

    internal fun attach(
        touchHelper: ItemTouchHelper,
        recyclerView: RecyclerView
    ) {
        this.touchHelper = touchHelper
        this.recyclerView = recyclerView
    }

    private fun getDefaultDragFlags(): Int {
        return when (val lm: LayoutManager? = recyclerView?.layoutManager) {
            is GridLayoutManager -> UP or DOWN or LEFT or RIGHT
            is LinearLayoutManager -> if (lm.isVertical) UP or DOWN else LEFT or RIGHT
            else -> 0
        }
    }

    private fun getDefaultSwipeFlags(): Int {
        return when (val lm: LayoutManager? = recyclerView?.layoutManager) {
            is GridLayoutManager -> when {
                lm.spanCount != 1 -> 0
                lm.isVertical -> LEFT or RIGHT
                else -> UP or DOWN
            }
            is LinearLayoutManager -> if (lm.isVertical) LEFT or RIGHT else UP or DOWN
            else -> 0
        }
    }
}