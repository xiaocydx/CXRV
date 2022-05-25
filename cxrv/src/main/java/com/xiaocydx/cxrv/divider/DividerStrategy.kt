package com.xiaocydx.cxrv.divider

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager

/**
 * 分割线策略
 *
 * @author xcc
 * @date 2021/10/8
 */
internal interface DividerStrategy {

    /**
     * 详细描述[ItemDecoration.getItemOffsets]
     */
    fun getItemOffsets(view: View, parent: RecyclerView, decoration: DividerItemDecoration)

    /**
     * 详细描述[ItemDecoration.onDraw]
     */
    fun onDraw(canvas: Canvas, parent: RecyclerView, decoration: DividerItemDecoration)

    val RecyclerView.orientation: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.orientation
            is StaggeredGridLayoutManager -> lm.orientation
            else -> -1
        }

    val RecyclerView.reverseLayout: Boolean
        get() = when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.reverseLayout
            is StaggeredGridLayoutManager -> lm.reverseLayout
            else -> false
        }

    fun Canvas.clipPadding(parent: RecyclerView): Boolean = when {
        parent.clipToPadding -> clipRect(
            parent.paddingLeft,
            parent.paddingTop,
            parent.width - parent.paddingRight,
            parent.height - parent.paddingBottom
        )
        else -> false
    }

    companion object {

        fun get(
            parent: RecyclerView
        ): DividerStrategy = when (val lm: LayoutManager? = parent.layoutManager) {
            is GridLayoutManager -> when (lm.spanCount) {
                1 -> LinearDividerStrategy
                else -> SpanDividerStrategy
            }
            is StaggeredGridLayoutManager -> when (lm.spanCount) {
                1 -> LinearDividerStrategy
                else -> SpanDividerStrategy
            }
            is LinearLayoutManager -> LinearDividerStrategy
            else -> NonDividerStrategy
        }
    }
}