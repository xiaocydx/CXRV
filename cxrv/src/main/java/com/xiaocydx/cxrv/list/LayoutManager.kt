package com.xiaocydx.cxrv.list

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.ExperimentalFeature

/**
 * 设置[LinearLayoutManager]，可用于链式调用场景
 */
inline fun <T : RecyclerView> T.linear(
    @Orientation orientation: Int = VERTICAL,
    block: LinearLayoutManager.() -> Unit = {}
): T = layout(LinearLayoutManager(context, orientation, false).apply(block))

/**
 * 设置[GridLayoutManager]，可用于链式调用场景
 */
inline fun <T : RecyclerView> T.grid(
    spanCount: Int,
    @Orientation orientation: Int = VERTICAL,
    block: GridLayoutManager.() -> Unit = {}
): T = layout(GridLayoutManager(context, spanCount, orientation, false).apply(block))

/**
 * 设置[StaggeredGridLayoutManagerCompat]，可用于链式调用场景
 */
inline fun <T : RecyclerView> T.staggered(
    spanCount: Int,
    @Orientation orientation: Int = VERTICAL,
    block: StaggeredGridLayoutManagerCompat.() -> Unit = {}
): T = layout(StaggeredGridLayoutManagerCompat(spanCount, orientation).apply(block))

/**
 * 设置[LayoutManager]，可用于链式调用场景
 */
fun <T : RecyclerView> T.layout(layout: LayoutManager): T {
    layoutManager = layout
    return this
}

/**
 * 设置`setHasFixedSize(true)`，可用于链式调用场景
 */
fun <T : RecyclerView> T.fixedSize(): T {
    setHasFixedSize(true)
    return this
}

/**
 * 启用[ViewBoundsCheck]兼容
 *
 * ### 兼容效果
 * 让`layoutManager.findXXXVisibleItemPosition()`这类查找函数，不去除`recyclerView.padding`区域。
 *
 * ### 兼容场景
 * 若`recyclerView.clipToPadding = false`，并且itemView绘制在`recyclerView.padding`区域，
 * 则`layoutManager.findXXXVisibleItemPosition()`这类查找函数会受`recyclerView.padding`影响。
 * 例如垂直方向的[LinearLayoutManager]，最后一个可视itemView绘制在`recyclerView.paddingBottom`区域，
 * [LinearLayoutManager.findLastVisibleItemPosition]会去除`recyclerView.paddingBottom`区域进行计算，
 * 导致函数返回结果不是实际的最后一个可视itemView的position。
 */
@ExperimentalFeature
fun LayoutManager.enableBoundCheckCompat() {
    isBoundCheckCompatEnabled = true
}

/**
 * 提供一些兼容属性的[StaggeredGridLayoutManager]
 */
open class StaggeredGridLayoutManagerCompat : StaggeredGridLayoutManager {
    private var pendingSavedState: Parcelable? = null

    /**
     * 是否在[onDetachedFromWindow]被调用时保存状态
     *
     * 由于`super.onDetachedFromWindow(view, recycler)`会执行清除逻辑，
     * 因此需要在其之前保存状态，确保`ViewPager2`嵌套[RecyclerView]这些场景，
     * [RecyclerView]的滚动位置能够被正确恢复。
     */
    var isSaveStateOnDetach = false

    constructor(
        spanCount: Int,
        @RecyclerView.Orientation orientation: Int
    ) : super(spanCount, orientation)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    override fun onAttachedToWindow(view: RecyclerView) {
        pendingSavedState?.let(::onRestoreInstanceState)
        pendingSavedState = null
        super.onAttachedToWindow(view)
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: RecyclerView.Recycler) {
        pendingSavedState = if (isSaveStateOnDetach) onSaveInstanceState() else null
        // 这是一种取巧的做法，对StaggeredGridLayoutManager的mPendingSavedState赋值，
        // 确保Fragment销毁时能保存状态，Fragment重建时恢复RecyclerView的滚动位置。
        pendingSavedState?.let(::onRestoreInstanceState)
        super.onDetachedFromWindow(view, recycler)
    }
}