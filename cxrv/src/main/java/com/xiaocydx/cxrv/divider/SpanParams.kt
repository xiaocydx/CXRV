package com.xiaocydx.cxrv.divider

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.list.getChildBindingAdapterItemCount
import com.xiaocydx.cxrv.list.getChildBindingAdapterPosition
import com.xiaocydx.cxrv.list.getChildLastBindingAdapterPosition

/**
 * 跨度空间参数
 *
 * @author xcc
 * @date 2021/10/29
 */
internal class SpanParams {
    var spanCount = -1
        private set
    var spanSize = -1
        private set
    var spanIndex = -1
        private set
    var spanGroupIndex = -1
        private set
    var isFirstSpan = false
        private set
    var isLastSpan = false
        private set
    var isFirstGroup = false
        private set
    var isLastGroup = false
        private set

    fun calculate(child: View, parent: RecyclerView) {
        spanCount = parent.spanCount
        spanSize = parent.getSpanSize(child)
        // item的跨度空间起始index
        spanIndex = parent.getSpanIndex(child)
        // item的跨度空间所属组起始index
        spanGroupIndex = parent.getSpanGroupIndex(child)
        isFirstSpan = spanIndex == 0
        isLastSpan = parent.isLastSpan(child, spanIndex)
        isFirstGroup = parent.isFirstSpanGroup(child, spanGroupIndex)
        isLastGroup = parent.isLastSpanGroup(child, spanGroupIndex)
    }

    private val RecyclerView.spanCount: Int
        get() = when (val lm: RecyclerView.LayoutManager? = layoutManager) {
            is GridLayoutManager -> lm.spanCount
            is StaggeredGridLayoutManager -> lm.spanCount
            is LinearLayoutManager -> 1
            else -> 0
        }

    private fun RecyclerView.getSpanSize(
        child: View
    ): Int = when (val lm: RecyclerView.LayoutManager? = layoutManager) {
        is GridLayoutManager -> {
            lm.spanSizeLookup.getSpanSize(getChildAdapterPosition(child))
        }
        else -> 1
    }

    private fun RecyclerView.getSpanIndex(
        child: View
    ): Int = when (val lm: RecyclerView.LayoutManager? = layoutManager) {
        is GridLayoutManager -> {
            lm.spanSizeLookup.getSpanIndex(getChildAdapterPosition(child), lm.spanCount)
        }
        is StaggeredGridLayoutManager -> {
            (child.layoutParams as StaggeredGridLayoutManager.LayoutParams).spanIndex
        }
        else -> 0
    }

    private fun RecyclerView.getSpanGroupIndex(child: View): Int =
            getSpanGroupIndex(getChildAdapterPosition(child))

    private fun RecyclerView.getSpanGroupIndex(
        globalPosition: Int
    ): Int = when (val lm: RecyclerView.LayoutManager? = layoutManager) {
        is GridLayoutManager -> {
            lm.spanSizeLookup.getSpanGroupIndex(globalPosition, lm.spanCount)
        }
        else -> 0
    }

    /**
     * 是否为末尾跨度空间
     */
    private fun RecyclerView.isLastSpan(child: View, spanIndex: Int): Boolean {
        return spanIndex + getSpanSize(child) == spanCount
    }

    /**
     * 是否为排除Header后的起始跨度空间所属组
     */
    private fun RecyclerView.isFirstSpanGroup(child: View, spanGroupIndex: Int): Boolean {
        val localPosition = getChildBindingAdapterPosition(child)
        return when (val lm: RecyclerView.LayoutManager? = layoutManager) {
            is GridLayoutManager -> {
                val globalPosition = getChildAdapterPosition(child)
                // 排除Header的起始position
                val firstPosition = globalPosition - localPosition
                spanGroupIndex == getSpanGroupIndex(firstPosition)
            }
            is StaggeredGridLayoutManager -> localPosition < lm.spanCount
            else -> false
        }
    }

    /**
     * 是否为排除Footer后的末尾跨度空间所属组
     */
    private fun RecyclerView.isLastSpanGroup(child: View, spanGroupIndex: Int): Boolean {
        val localPosition = getChildBindingAdapterPosition(child)
        return when (val lm: RecyclerView.LayoutManager? = layoutManager) {
            is GridLayoutManager -> {
                val globalPosition = getChildAdapterPosition(child)
                val lastLocalPosition = getChildLastBindingAdapterPosition(child)
                // 排除Footer的末尾position
                val lastPosition = globalPosition + lastLocalPosition - localPosition
                spanGroupIndex == getSpanGroupIndex(lastPosition)
            }
            is StaggeredGridLayoutManager -> {
                val itemCount = getChildBindingAdapterItemCount(child)
                localPosition >= itemCount - lm.spanCount
            }
            else -> false
        }
    }
}