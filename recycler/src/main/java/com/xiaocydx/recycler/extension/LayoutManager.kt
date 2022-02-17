package com.xiaocydx.recycler.extension

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.flexbox.FlexboxLayoutManager

/**
 * 设置[LinearLayoutManager]，可用于链式调用场景
 *
 * ```
 * recyclerView.linear(orientation = VERTICAL)
 * ```
 * **注意**：大部分场景的RecyclerView宽高是[MATCH_PARENT]或者固定值，
 * 因此将[hasFixedSize]默认设为`true`，若宽高是[WRAP_CONTENT]，则[hasFixedSize]传入`false`。
 */
fun <T : RecyclerView> T.linear(
    @Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    stackFromEnd: Boolean = false,
    hasFixedSize: Boolean = true
): T = layout(
    hasFixedSize = hasFixedSize,
    layout = LinearLayoutManager(context).also {
        it.orientation = orientation
        it.reverseLayout = reverseLayout
        it.stackFromEnd = stackFromEnd
    }
)

/**
 * 设置[GridLayoutManager]，可用于链式调用场景
 *
 * ```
 * recyclerView.grid(spanCount = 3)
 * ```
 * **注意**：大部分场景的RecyclerView宽高是[MATCH_PARENT]或者固定值，
 * 因此将[hasFixedSize]默认设为`true`，若宽高是[WRAP_CONTENT]，则[hasFixedSize]传入`false`。
 */
fun <T : RecyclerView> T.grid(
    spanCount: Int,
    @Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    stackFromEnd: Boolean = false,
    hasFixedSize: Boolean = true
): T = layout(
    hasFixedSize = hasFixedSize,
    layout = GridLayoutManager(context, spanCount).also {
        it.orientation = orientation
        it.reverseLayout = reverseLayout
        it.stackFromEnd = stackFromEnd
    }
)

/**
 * 设置[StaggeredGridLayoutManager]，可用于链式调用场景
 *
 * ```
 * recyclerView.staggered(spanCount = 3)
 * ```
 * **注意**：大部分场景的RecyclerView宽高是[MATCH_PARENT]或者固定值，
 * 因此将[hasFixedSize]默认设为`true`，若宽高是[WRAP_CONTENT]，则[hasFixedSize]传入`false`。
 */
fun <T : RecyclerView> T.staggered(
    spanCount: Int,
    @Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    hasFixedSize: Boolean = true
): T = layout(
    hasFixedSize = hasFixedSize,
    layout = StaggeredGridLayoutManager(spanCount, orientation)
        .also { it.reverseLayout = reverseLayout }
)

/**
 * 设置[LayoutManager]，可用于链式调用场景
 *
 * ```
 * val layoutManager: LayoutManager = ...
 * recyclerView.layout(layoutManager)
 * ```
 * **注意**：大部分场景的RecyclerView宽高是[MATCH_PARENT]或者固定值，
 * 因此将[hasFixedSize]默认设为`true`，若宽高是[WRAP_CONTENT]，则[hasFixedSize]传入`false`。
 */
fun <T : RecyclerView> T.layout(
    layout: LayoutManager,
    hasFixedSize: Boolean = true
): T {
    layoutManager = layout
    setHasFixedSize(hasFixedSize)
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