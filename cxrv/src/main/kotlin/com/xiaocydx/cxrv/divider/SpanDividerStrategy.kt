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

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.recyclerview.widget.isViewHolderRemoved
import com.xiaocydx.cxrv.R
import com.xiaocydx.cxrv.internal.childEach
import com.xiaocydx.cxrv.list.isHeaderOrFooter

/**
 * 跨度空间分割线策略
 *
 * 执行item动画时，分割线不附带view的X/Y偏移量，确保分割线正常显示。
 *
 * @author xcc
 * @date 2021/9/29
 */
@Suppress("KotlinConstantConditions")
internal object SpanDividerStrategy : DividerStrategy {

    override fun getItemOffsets(
        view: View,
        parent: RecyclerView,
        decoration: DividerItemDecoration
    ) = with(decoration) {
        if (parent.isHeaderOrFooter(view)) {
            return
        }
        if (parent.isViewHolderRemoved(view)) {
            view.setRemovedItemOffsets()
            return
        }
        parent.checkSpanCacheEnabled()
        // 保存Span参数，绘制阶段直接获取，不需要再计算
        val span = view.setSpanParams(parent)
        when (parent.orientation) {
            VERTICAL -> setVerticalItemOffsets(view, parent, span)
            HORIZONTAL -> setHorizontalItemOffsets(view, parent, span)
        }
    }

    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        decoration: DividerItemDecoration
    ) = decoration.withSave(canvas) {
        val orientation = parent.orientation
        canvas.clipPadding(parent)
        parent.childEach { child ->
            if (parent.isHeaderOrFooter(child)) {
                return@childEach
            }
            val span = child.getSpanParams() ?: return@childEach
            when (orientation) {
                VERTICAL -> drawVerticalDivider(canvas, child, parent, span)
                HORIZONTAL -> drawHorizontalDivider(canvas, child, parent, span)
            }
        }
    }

    private fun DividerItemDecoration.setVerticalItemOffsets(
        child: View,
        parent: RecyclerView,
        span: SpanParams
    ) {
        var left = 0
        var right = 0
        when {
            leftEdge && !rightEdge -> left = width
            !leftEdge && rightEdge -> right = width
            leftEdge && rightEdge -> span.apply {
                left = width - spanIndex * width / span.spanCount
                right = (spanIndex + spanSize) * width / spanCount
            }
            !leftEdge && !rightEdge -> span.apply {
                left = spanIndex * width / spanCount
                right = width - (spanIndex + spanSize) * width / spanCount
            }
        }

        var top = if (topEdge && span.isFirstGroup) height else 0
        var bottom = if (bottomEdge || !span.isLastGroup) height else 0
        if (parent.reverseLayout) {
            top = bottom.also { bottom = top }
        }
        child.setItemOffsets(left, top, right, bottom)
    }

    private fun DividerItemDecoration.setHorizontalItemOffsets(
        child: View,
        parent: RecyclerView,
        span: SpanParams
    ) {
        var top = 0
        var bottom = 0
        when {
            topEdge && !bottomEdge -> top = height
            !topEdge && bottomEdge -> bottom = height
            topEdge && bottomEdge -> span.apply {
                top = height - spanIndex * height / spanCount
                bottom = (spanIndex + spanSize) * height / spanCount
            }
            !topEdge && !bottomEdge -> span.apply {
                top = spanIndex * height / spanCount
                bottom = height - (spanIndex + spanSize) * height / spanCount
            }
        }

        var left = if (leftEdge && span.isFirstGroup) width else 0
        var right = if (rightEdge || !span.isLastGroup) width else 0
        if (parent.reverseLayout) {
            left = right.also { right = left }
        }
        child.setItemOffsets(left, top, right, bottom)
    }

    /**
     * 分割线的size与[setVerticalItemOffsets]计算出的偏移量并不一致，
     * 计算出的偏移量其作用是让item的宽度相等，并不是实际的分割线size。
     */
    private fun DividerItemDecoration.drawVerticalDivider(
        canvas: Canvas,
        child: View,
        parent: RecyclerView,
        span: SpanParams
    ) {
        setVerticalDrawEdge(parent, span)
        val bounds = drawEdge.getBounds(child, withAnim = false)
        val alpha = drawEdge.getAlpha(child, withAnim = !parent.isStaggered)
        val reverseLayout = parent.reverseLayout
        var left = bounds.left
        var right = bounds.right
        val top = when {
            !reverseLayout && span.isFirstGroup -> bounds.top + topMargin
            reverseLayout && span.isLastGroup -> bounds.top + bottomMargin
            else -> bounds.top
        }
        val bottom = when {
            !reverseLayout && span.isLastGroup -> bounds.bottom - bottomMargin
            reverseLayout && span.isFirstGroup -> bounds.bottom - topMargin
            else -> bounds.bottom
        }

        if (drawEdge.left) {
            left -= width
            canvas.drawDivider(left, top, bounds.left, bottom, alpha)
        }
        if (drawEdge.right) {
            right += width
            canvas.drawDivider(bounds.right, top, right, bottom, alpha)
        }

        left = if (span.isFirstSpan) left + leftMargin else left
        right = if (span.isLastSpan) right - rightMargin else right
        if (drawEdge.top) {
            canvas.drawDivider(left, bounds.top - height, right, bounds.top, alpha)
        }
        if (drawEdge.bottom) {
            canvas.drawDivider(left, bounds.bottom, right, bounds.bottom + height, alpha)
        }
    }

    /**
     * 分割线的size与[setHorizontalItemOffsets]计算出的偏移量并不一致，
     * 计算出的偏移量其作用是让item的高度相等，并不是实际的分割线size。
     */
    private fun DividerItemDecoration.drawHorizontalDivider(
        canvas: Canvas,
        child: View,
        parent: RecyclerView,
        span: SpanParams
    ) {
        setHorizontalDrawEdge(parent, span)
        val bounds = drawEdge.getBounds(child, withAnim = false)
        val alpha = drawEdge.getAlpha(child, withAnim = !parent.isStaggered)
        val reverseLayout = parent.reverseLayout
        var top = bounds.top
        var bottom = bounds.bottom
        val left = when {
            !reverseLayout && span.isFirstGroup -> bounds.left + leftMargin
            reverseLayout && span.isLastGroup -> bounds.left + rightMargin
            else -> bounds.left
        }
        val right = when {
            !reverseLayout && span.isLastGroup -> bounds.right - rightMargin
            reverseLayout && span.isFirstGroup -> bounds.right - leftMargin
            else -> bounds.right
        }

        if (drawEdge.top) {
            top -= height
            canvas.drawDivider(left, top, right, bounds.top, alpha)
        }
        if (drawEdge.bottom) {
            bottom += height
            canvas.drawDivider(left, bounds.bottom, right, bottom, alpha)
        }

        top = if (span.isFirstSpan) top + topMargin else top
        bottom = if (span.isLastSpan) bottom - bottomMargin else bottom
        if (drawEdge.left) {
            canvas.drawDivider(bounds.left - width, top, bounds.left, bottom, alpha)
        }
        if (drawEdge.right) {
            canvas.drawDivider(bounds.right, top, bounds.right + width, bottom, alpha)
        }
    }

    private fun DividerItemDecoration.setVerticalDrawEdge(parent: RecyclerView, span: SpanParams) {
        var left = false
        var right = false
        when {
            leftEdge && rightEdge -> {
                left = span.isFirstSpan
                right = true
            }
            leftEdge && !rightEdge -> left = true
            !leftEdge && rightEdge -> right = true
            !leftEdge && !rightEdge -> right = !span.isLastSpan
        }
        // 填充瀑布流布局最后一组的左部和右部
        if (parent.isStaggered && !left) {
            left = span.isLastGroup
        }
        if (parent.isStaggered && !right) {
            right = span.isLastGroup
        }

        var top = topEdge && span.isFirstGroup
        var bottom = bottomEdge || !span.isLastGroup
        if (parent.reverseLayout) {
            top = bottom.also { bottom = top }
        }
        drawEdge.set(left, top, right, bottom)
    }

    private fun DividerItemDecoration.setHorizontalDrawEdge(parent: RecyclerView, span: SpanParams) {
        var top = false
        var bottom = false
        when {
            topEdge && bottomEdge -> {
                top = span.isFirstSpan
                bottom = true
            }
            topEdge && !bottomEdge -> top = true
            !topEdge && bottomEdge -> bottom = true
            !topEdge && !bottomEdge -> bottom = !span.isLastSpan
        }
        // 填充瀑布流布局最后一组的顶部和底部
        if (parent.isStaggered && !top) {
            top = span.isLastGroup
        }
        if (parent.isStaggered && !bottom) {
            bottom = span.isLastGroup
        }

        var left = leftEdge && span.isFirstGroup
        var right = rightEdge || !span.isLastGroup
        if (parent.reverseLayout) {
            left = right.also { right = left }
        }
        drawEdge.set(left, top, right, bottom)
    }

    private val RecyclerView.isStaggered: Boolean
        get() = layoutManager is StaggeredGridLayoutManager

    private fun View.getSpanParams(): SpanParams? {
        return getTag(R.id.tag_span_params) as? SpanParams
    }

    private fun View.setSpanParams(parent: RecyclerView): SpanParams {
        var params = getTag(R.id.tag_span_params) as? SpanParams
        if (params == null) {
            params = SpanParams()
            setTag(R.id.tag_span_params, params)
        }
        params.calculate(this, parent)
        return params
    }

    private fun RecyclerView.checkSpanCacheEnabled() {
        val lm = layoutManager as? GridLayoutManager ?: return
        val lookup = lm.spanSizeLookup ?: return
        if (!lookup.isSpanIndexCacheEnabled) {
            lookup.isSpanIndexCacheEnabled = true
        }
        if (!lookup.isSpanGroupIndexCacheEnabled) {
            lookup.isSpanGroupIndexCacheEnabled = true
        }
    }
}