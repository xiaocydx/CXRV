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
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.recyclerview.widget.isViewHolderRemoved
import com.xiaocydx.cxrv.internal.childEach
import com.xiaocydx.cxrv.list.isFirstChildBindingAdapterPosition
import com.xiaocydx.cxrv.list.isHeaderOrFooter
import com.xiaocydx.cxrv.list.isLastChildBindingAdapterPosition

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
            if (parent.isHeaderOrFooter(child)) {
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
        val bounds = drawEdge.getBounds(child, withAnim = true)
        val alpha = drawEdge.getAlpha(child, withAnim = true)
        var left = bounds.left
        var right = bounds.right
        // drawLeft
        if (offsets.left > 0) {
            left -= offsets.left
            canvas.drawDivider(left, bounds.top, bounds.left, bounds.bottom, alpha)
        }
        // drawRight
        if (offsets.right > 0) {
            right += offsets.right
            canvas.drawDivider(bounds.right, bounds.top, right, bounds.bottom, alpha)
        }

        left += leftMargin
        right -= rightMargin
        // drawTop
        if (offsets.top > 0) {
            canvas.drawDivider(left, bounds.top - offsets.top, right, bounds.top, alpha)
        }
        // drawBottom
        if (offsets.bottom > 0) {
            canvas.drawDivider(left, bounds.bottom, right, bounds.bottom + offsets.bottom, alpha)
        }
    }

    private fun DividerItemDecoration.drawHorizontalDivider(canvas: Canvas, child: View, offset: Rect) {
        val bounds = drawEdge.getBounds(child, withAnim = true)
        val alpha = drawEdge.getAlpha(child, withAnim = true)
        var top = bounds.top
        var bottom = bounds.bottom
        // drawTop
        if (offset.top > 0) {
            top -= offset.top
            canvas.drawDivider(bounds.left, top, bounds.right, bounds.top, alpha)
        }
        // drawBottom
        if (offset.bottom > 0) {
            bottom += offset.bottom
            canvas.drawDivider(bounds.left, bounds.bottom, bounds.right, bottom, alpha)
        }

        top += topMargin
        bottom -= bottomMargin
        // drawLeft
        if (offset.left > 0) {
            canvas.drawDivider(bounds.left - offset.left, top, bounds.left, bottom, alpha)
        }
        // drawRight
        if (offset.right > 0) {
            canvas.drawDivider(bounds.right, top, bounds.right + offset.right, bottom, alpha)
        }
    }
}