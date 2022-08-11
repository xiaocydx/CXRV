@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.cxrv.list

import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.ExperimentalFeature

/**
 * 设置[LinearLayoutManagerCompat]，可用于链式调用场景
 */
inline fun <T : RecyclerView> T.linear(
    @Orientation orientation: Int = VERTICAL,
    block: LinearLayoutManagerCompat.() -> Unit = {}
): T = layout(LinearLayoutManagerCompat(context, orientation, false).apply(block))

/**
 * 设置[GridLayoutManagerCompat]，可用于链式调用场景
 */
inline fun <T : RecyclerView> T.grid(
    spanCount: Int,
    @Orientation orientation: Int = VERTICAL,
    block: GridLayoutManagerCompat.() -> Unit = {}
): T = layout(GridLayoutManagerCompat(context, spanCount, orientation, false).apply(block))

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