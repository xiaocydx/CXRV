package com.xiaocydx.cxrv.list

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * 设置[LinearLayoutManager]，可用于链式调用场景
 */
fun <T : RecyclerView> T.linear(
    @Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    stackFromEnd: Boolean = false
): T = layout(LinearLayoutManager(context).also {
    it.orientation = orientation
    it.reverseLayout = reverseLayout
    it.stackFromEnd = stackFromEnd
})

/**
 * 设置[GridLayoutManager]，可用于链式调用场景
 */
fun <T : RecyclerView> T.grid(
    spanCount: Int,
    @Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    stackFromEnd: Boolean = false
): T = layout(GridLayoutManager(context, spanCount).also {
    it.orientation = orientation
    it.reverseLayout = reverseLayout
    it.stackFromEnd = stackFromEnd
})

/**
 * 设置[StaggeredGridLayoutManager]，可用于链式调用场景
 */
fun <T : RecyclerView> T.staggered(
    spanCount: Int,
    @Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false
): T = layout(StaggeredGridLayoutManager(spanCount, orientation).also {
    it.reverseLayout = reverseLayout
})

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