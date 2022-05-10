package com.xiaocydx.recycler.extension

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.FlexboxLayoutManager

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

/**
 * 解决itemView的LayoutParams缺失或者与LayoutManager的LayoutParams类型不一致的问题
 *
 * ```
 * override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
 *     val itemView: View = ...
 *     return ViewHolder(itemView).resolveLayoutParams(parent)
 * }
 * ```
 */
fun <T : ViewHolder> T.resolveLayoutParams(parent: ViewGroup): T {
    require(parent is RecyclerView) { "parent的类型需要是RecyclerView。" }
    val lm = parent.layoutManager ?: return this
    val source = itemView.layoutParams
    itemView.layoutParams = when (source) {
        null -> lm.generateDefaultLayoutParams()
        else -> lm.generateLayoutParamsFix(source)
    }
    return this
}

/**
 * 修复[FlexboxLayoutManager]没有重写[LayoutManager.generateLayoutParams]的问题
 */
private fun LayoutManager.generateLayoutParamsFix(
    source: LayoutParams
): LayoutParams = when (this) {
    !is FlexboxLayoutManager -> generateLayoutParams(source)
    else -> when (source) {
        is FlexboxLayoutManager.LayoutParams -> FlexboxLayoutManager.LayoutParams(source)
        is ViewGroup.MarginLayoutParams -> FlexboxLayoutManager.LayoutParams(source)
        else -> FlexboxLayoutManager.LayoutParams(source)
    }
}