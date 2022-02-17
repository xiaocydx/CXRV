package com.xiaocydx.recycler.divider

import android.graphics.Canvas
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.xiaocydx.recycler.extension.*

/**
 * 线性分割线策略
 *
 * 执行item动画时，分割线附带view的X/Y偏移量，确保分割线正常显示。
 *
 * @author xcc
 * @date 2021/10/20
 */
internal object LinearDividerStrategy : DividerStrategy {

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
        when (parent.orientation) {
            VERTICAL -> setVerticalItemOffsets(view, parent, decoration)
            HORIZONTAL -> setHorizontalItemOffsets(view, parent, decoration)
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
            if (parent.isHeaderOrFooterOrRemoved(child)) {
                return@childEach
            }
            val offsets = child.getItemOffsets() ?: return@childEach
            when (orientation) {
                VERTICAL -> drawVerticalDivider(canvas, child, offsets)
                HORIZONTAL -> drawHorizontalDivider(canvas, child, offsets)
            }
        }
    }

    private fun setVerticalItemOffsets(
        view: View,
        parent: RecyclerView,
        decoration: DividerItemDecoration
    ) = with(decoration) {
        val left = if (leftEdge) width else 0
        val right = if (rightEdge) width else 0
        var top = when {
            topEdge && parent.isFirstChildBindingAdapterPosition(view) -> height
            else -> 0
        }
        var bottom = when {
            bottomEdge || !parent.isLastChildBindingAdapterPosition(view) -> height
            else -> 0
        }
        if (parent.reverseLayout) {
            top = bottom.also { bottom = top }
        }
        view.setItemOffsets(left, top, right, bottom)
    }

    private fun setHorizontalItemOffsets(
        view: View,
        parent: RecyclerView,
        decoration: DividerItemDecoration
    ) = with(decoration) {
        val top = if (topEdge) height else 0
        val bottom = if (bottomEdge) height else 0
        var left = when {
            leftEdge && parent.isFirstChildBindingAdapterPosition(view) -> width
            else -> 0
        }
        var right = when {
            rightEdge || !parent.isLastChildBindingAdapterPosition(view) -> width
            else -> 0
        }
        if (parent.reverseLayout) {
            left = right.also { right = left }
        }
        view.setItemOffsets(left, top, right, bottom)
    }

    private fun DividerItemDecoration.drawVerticalDivider(canvas: Canvas, child: View, offsets: Rect) {
        val bounds = drawEdge.getBounds(child, withAnimOffset = true)
        var left = bounds.left
        var right = bounds.right
        // drawLeft
        if (offsets.left > 0) {
            left -= offsets.left
            canvas.drawDivider(left, bounds.top, bounds.left, bounds.bottom)
        }
        // drawRight
        if (offsets.right > 0) {
            right += offsets.right
            canvas.drawDivider(bounds.right, bounds.top, right, bounds.bottom)
        }

        left += leftMargin
        right -= rightMargin
        // drawTop
        if (offsets.top > 0) {
            canvas.drawDivider(left, bounds.top - offsets.top, right, bounds.top)
        }
        // drawBottom
        if (offsets.bottom > 0) {
            canvas.drawDivider(left, bounds.bottom, right, bounds.bottom + offsets.bottom)
        }
    }

    private fun DividerItemDecoration.drawHorizontalDivider(canvas: Canvas, child: View, offset: Rect) {
        val bounds = drawEdge.getBounds(child, withAnimOffset = true)
        var top = bounds.top
        var bottom = bounds.bottom
        // drawTop
        if (offset.top > 0) {
            top -= offset.top
            canvas.drawDivider(bounds.left, top, bounds.right, bounds.top)
        }
        // drawBottom
        if (offset.bottom > 0) {
            bottom += offset.bottom
            canvas.drawDivider(bounds.left, bounds.bottom, bounds.right, bottom)
        }

        top += topMargin
        bottom -= bottomMargin
        // drawLeft
        if (offset.left > 0) {
            canvas.drawDivider(bounds.left - offset.left, top, bounds.left, bottom)
        }
        // drawRight
        if (offset.right > 0) {
            canvas.drawDivider(bounds.right, top, bounds.right + offset.right, bottom)
        }
    }
}