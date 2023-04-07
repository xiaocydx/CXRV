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
        isLastGroup = parent.isLastSpanGroup(child, spanIndex, spanGroupIndex)
    }

    private val RecyclerView.spanCount: Int
        get() = when (val lm: RecyclerView.LayoutManager? = layoutManager) {
            is GridLayoutManager -> lm.spanCount
            is StaggeredGridLayoutManager -> lm.spanCount
            is LinearLayoutManager -> 1
            else -> NOT_SUPPORT
        }

    private fun RecyclerView.getSpanSize(child: View): Int = when (layoutManager) {
        is GridLayoutManager -> (child.layoutParams as GridLayoutManager.LayoutParams).spanSize
        else -> 1
    }

    private fun RecyclerView.getSpanIndex(child: View): Int = when (layoutManager) {
        is GridLayoutManager -> (child.layoutParams as GridLayoutManager.LayoutParams).spanIndex
        is StaggeredGridLayoutManager -> (child.layoutParams as StaggeredGridLayoutManager.LayoutParams).spanIndex
        else -> NOT_SUPPORT
    }

    private fun RecyclerView.getSpanGroupIndex(child: View): Int =
            getSpanGroupIndex(getChildAdapterPosition(child))

    private fun RecyclerView.getSpanGroupIndex(
        globalPosition: Int
    ): Int = when (val lm: RecyclerView.LayoutManager? = layoutManager) {
        is GridLayoutManager -> lm.spanSizeLookup.getSpanGroupIndex(globalPosition, lm.spanCount)
        else -> NOT_SUPPORT
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
    private fun RecyclerView.isLastSpanGroup(child: View, spanIndex: Int, spanGroupIndex: Int): Boolean {
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
                // 注意：计算逻辑不考虑isFullSpan = true的情况。
                // 先假设spanCount个item位于末尾spanGroup，计算出预测的spanIndex，
                // 若spanIndex <= predictSpanIndex，则能粗略表示item位于末尾spanGroup。
                val itemCount = getChildBindingAdapterItemCount(child)
                val thresholdPosition = (itemCount - lm.spanCount).coerceAtLeast(0)
                if (localPosition < thresholdPosition) return false
                val predictSpanIndex = localPosition - thresholdPosition
                spanIndex <= predictSpanIndex
            }
            else -> false
        }
    }

    companion object {
        const val NOT_SUPPORT = -1
    }
}