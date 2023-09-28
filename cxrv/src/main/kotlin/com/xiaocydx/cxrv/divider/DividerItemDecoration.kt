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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LayoutManagerCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.warn
import com.xiaocydx.cxrv.R
import java.lang.ref.WeakReference

/**
 * 通用分割线
 *
 * @author xcc
 * @date 2021/9/29
 */
@Suppress("DEPRECATION")
class DividerItemDecoration private constructor(config: Config) : ItemDecoration() {
    private var outRect: Rect = emptyRect
    private val divider: Drawable? = config.drawable
    private var state: State = emptyState
    private var layoutRef: WeakReference<LayoutManager>? = null
    private val isSpacing = when (divider) {
        null -> true
        !is ColorDrawable -> false
        else -> divider.color == Color.TRANSPARENT
    }

    internal val drawEdge = DrawEdge()
    internal val leftEdge = config.leftEdge
    internal val topEdge = config.topEdge
    internal val rightEdge = config.rightEdge
    internal val bottomEdge = config.bottomEdge
    @Px internal val leftMargin = config.leftMargin
    @Px internal val topMargin = config.topMargin
    @Px internal val rightMargin = config.rightMargin
    @Px internal val bottomMargin = config.bottomMargin
    @Px internal val width = config.width
    @Px internal val height = config.height

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        warn(parent.layoutManager)
        if (state.isPreLayout) {
            // preLayout阶段不重新计算间距，确保preLayout的布局结果不影响realLayout
            view.getItemOffsets()?.let(outRect::set)
            return
        }
        resetThen(state, outRect) {
            DividerStrategy.get(parent).getItemOffsets(view, parent, this)
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        if (isSpacing) return
        resetThen(state) {
            DividerStrategy.get(parent).onDraw(canvas, parent, this)
        }
    }

    private fun warn(layout: LayoutManager?) {
        if (layout == null) {
            layoutRef = null
            return
        }
        if (layoutRef?.get() === layout) return
        layoutRef = WeakReference(layout)
        LayoutManagerCompat.warn(layout) { "确保DividerItemDecoration的绘制结果正常" }
    }

    private inline fun resetThen(
        state: State,
        outRect: Rect = emptyRect,
        block: () -> Unit
    ) {
        this.state = state
        this.outRect = outRect
        drawEdge.reset()
        block()
        this.state = emptyState
        this.outRect = emptyRect
    }

    internal inline fun withSave(
        canvas: Canvas,
        block: DividerItemDecoration.() -> Unit
    ) {
        val checkpoint = canvas.save()
        try {
            this@DividerItemDecoration.block()
        } finally {
            canvas.restoreToCount(checkpoint)
        }
    }

    internal fun View.getItemOffsets(): Rect? {
        return getTag(R.id.tag_divider_offsets_rect) as? Rect
    }

    internal fun View.setItemOffsets(left: Int, top: Int, right: Int, bottom: Int) {
        var offsetRect = getTag(R.id.tag_divider_offsets_rect) as? Rect
        if (offsetRect == null) {
            offsetRect = Rect()
            setTag(R.id.tag_divider_offsets_rect, offsetRect)
        }
        offsetRect.set(left, top, right, bottom)
        outRect.set(left, top, right, bottom)
    }

    internal fun Canvas.drawDivider(
        left: Int, top: Int, right: Int, bottom: Int,
        @FloatRange(from = 0.0, to = 1.0) alpha: Float
    ) {
        divider?.also {
            it.alpha = (alpha * 255).toInt()
                .coerceAtLeast(0)
                .coerceAtMost(255)
            it.setBounds(left, top, right, bottom)
            it.draw(this)
        }
    }

    private companion object {
        val emptyRect = Rect()
        val emptyState = State()
    }

    class Config @PublishedApi internal constructor(private val context: Context) {
        private var edge = Edge()

        /**
         * 水平方向的分割线size
         */
        @Deprecated("调整setter的返回值类型", ReplaceWith("width(value)"))
        @Px var width = 0

        /**
         * 垂直方向的分割线size
         */
        @Deprecated("调整setter的返回值类型", ReplaceWith("height(value)"))
        @Px var height = 0

        /**
         * 分割线颜色值，搭配[width]和[height]使用
         */
        @ColorInt
        @Deprecated("调整setter的返回值类型", ReplaceWith("color(value)"))
        var color = -1
            set(value) = run { color(value) }

        /**
         * 分割线颜色资源Id，搭配[width]和[height]使用
         */
        @ColorRes
        @Deprecated("调整setter的返回值类型", ReplaceWith("colorRes(resId)"))
        var colorRes = -1
            set(value) = run { colorRes(value) }

        /**
         * 分割线Drawable资源Id
         */
        @DrawableRes
        @Deprecated("调整setter的返回值类型", ReplaceWith("drawableRes(resId)"))
        var drawableRes = -1
            set(value) = run { drawableRes(value) }

        /**
         * 分割线Drawable
         */
        @Deprecated("调整setter的返回值类型", ReplaceWith("drawable(value)"))
        var drawable: Drawable? = null

        /**
         * 是否启用左部边缘分割线
         */
        @Deprecated("合并边缘分割线属性", ReplaceWith("edge(Edge.left())"))
        var leftEdge: Boolean = false

        /**
         * 是否启用顶部边缘分割线
         */
        @Deprecated("合并边缘分割线属性", ReplaceWith("edge(Edge.top())"))
        var topEdge = false

        /**
         * 是否启用右部边缘分割线
         */
        @Deprecated("合并边缘分割线属性", ReplaceWith("edge(Edge.right())"))
        var rightEdge = false

        /**
         * 是否启用底部边缘分割线
         */
        @Deprecated("合并边缘分割线属性", ReplaceWith("edge(Edge.bottom())"))
        var bottomEdge = false

        /**
         * 是否启用顶部和底部边缘分割线
         */
        @Deprecated("合并边缘分割线属性", ReplaceWith("edge(Edge.vertical())"))
        var verticalEdge: Boolean
            get() = topEdge && bottomEdge
            set(value) = run { topEdge = value }.run { bottomEdge = value }

        /**
         * 是否启用左部和右部边缘分割线
         */
        @Deprecated("合并边缘分割线属性", ReplaceWith("edge(Edge.horizontal())"))
        var horizontalEdge: Boolean
            get() = leftEdge && rightEdge
            set(value) = run { leftEdge = value }.run { rightEdge = value }

        /**
         * 左部分割线间隔
         */
        @Deprecated("合并分割线间距属性", ReplaceWith("margin(left)"))
        @Px var leftMargin = 0

        /**
         * 顶部分割线间隔
         */
        @Deprecated("合并分割线间距属性", ReplaceWith("margin(top)"))
        @Px var topMargin = 0

        /**
         * 右部分割线间隔
         */
        @Deprecated("合并分割线间距属性", ReplaceWith("margin(right)"))
        @Px var rightMargin = 0

        /**
         * 底部分割线间隔
         */
        @Deprecated("合并分割线间距属性", ReplaceWith("margin(bottom)"))
        @Px var bottomMargin = 0

        /**
         * 垂直方向的分割线间隔
         */
        @get:Px
        @setparam:Px
        @Deprecated("合并分割线间距属性", ReplaceWith("margin(top, bottom)"))
        var verticalMargin: Int
            get() = if (topMargin == bottomMargin) topMargin else 0
            set(value) = run { margin(top = value, bottom = value) }

        /**
         * 水平方向的分割线间隔
         */
        @get:Px
        @setparam:Px
        @Deprecated("合并分割线间距属性", ReplaceWith("margin(left, right)"))
        var horizontalMargin: Int
            get() = if (leftMargin == rightMargin) leftMargin else 0
            set(value) = run { margin(left = value, right = value) }

        /**
         * 水平方向的分割线size
         */
        fun width(@Px value: Int) = apply { width = value }

        /**
         * 垂直方向的分割线size
         */
        fun height(@Px value: Int) = apply { height = value }

        /**
         * 分割线颜色值，搭配[width]和[height]使用
         */
        fun color(@ColorInt value: Int) = drawable(ColorDrawable(value))

        /**
         * 分割线颜色资源Id，搭配[width]和[height]使用
         */
        fun colorRes(@ColorRes resId: Int) = color(ContextCompat.getColor(context, resId))

        /**
         * 分割线Drawable资源Id
         */
        fun drawableRes(@DrawableRes resId: Int) = drawable(
            requireNotNull(ContextCompat.getDrawable(context, resId))
        )

        /**
         * 分割线Drawable
         */
        fun drawable(value: Drawable) = apply { drawable = value }

        /**
         * 边缘分割线，方向不会因为反转布局而改变
         *
         * 以启用顶部和底部边缘分割线为例：
         * ```
         * // RecyclerView顶部绘制分割线
         * *******
         * |     |
         * |     |
         * |     |
         * *******
         * // RecyclerView底部绘制分割线
         * ```
         */
        fun edge(value: Edge) = apply { edge = value }

        /**
         * 分割线间隔，方向会因为反转布局而改变
         *
         * @param left   左部分割线间隔
         * @param top    顶部分割线间隔
         * @param right  右部分割线间隔
         * @param bottom 底部分割线间隔
         */
        fun margin(
            @Px left: Int = leftMargin,
            @Px top: Int = topMargin,
            @Px right: Int = rightMargin,
            @Px bottom: Int = bottomMargin,
        ) = apply {
            leftMargin = left
            topMargin = top
            rightMargin = right
            bottomMargin = bottom
        }

        @PublishedApi
        internal fun build(): DividerItemDecoration {
            width = when {
                drawable == null -> width
                drawable!!.intrinsicWidth != -1 -> drawable!!.intrinsicWidth
                else -> width
            }.coerceAtLeast(0)

            height = when {
                drawable == null -> height
                drawable!!.intrinsicHeight != -1 -> drawable!!.intrinsicHeight
                else -> height
            }.coerceAtLeast(0)

            leftEdge = leftEdge or edge.leftEdge()
            topEdge = topEdge or edge.topEdge()
            rightEdge = rightEdge or edge.rightEdge()
            bottomEdge = bottomEdge or edge.bottomEdge()

            leftMargin = leftMargin.coerceAtLeast(0)
            topMargin = topMargin.coerceAtLeast(0)
            rightMargin = rightMargin.coerceAtLeast(0)
            bottomMargin = bottomMargin.coerceAtLeast(0)
            return DividerItemDecoration(this)
        }
    }
}

/**
 * 边缘分割线，方向不会因为反转布局而改变
 *
 * 以启用顶部和底部边缘分割线为例：
 * ```
 * // RecyclerView顶部绘制分割线
 * *******
 * |     |
 * |     |
 * |     |
 * *******
 * // RecyclerView底部绘制分割线
 */
@JvmInline
value class Edge internal constructor(private val value: Int = 0) {
    /**
     * 启用左部边缘分割线
     */
    fun left() = Edge(value or LEFT)

    /**
     * 启用顶部边缘分割线
     */
    fun top() = Edge(value or TOP)

    /**
     * 启用右部边缘分割线
     */
    fun right() = Edge(value or RIGHT)

    /**
     * 启用底部边缘分割线
     */
    fun bottom() = Edge(value or BOTTOM)

    /**
     * 启用顶部和底部边缘分割线
     */
    fun vertical() = Edge(value or TOP or BOTTOM)

    /**
     * 启用左部和右部边缘分割线
     */
    fun horizontal() = Edge(value or LEFT or RIGHT)

    internal fun leftEdge() = contains(LEFT)

    internal fun topEdge() = contains(TOP)

    internal fun rightEdge() = contains(RIGHT)

    internal fun bottomEdge() = contains(BOTTOM)

    private fun contains(edge: Int) = value and edge == edge

    companion object {
        private const val LEFT = 1
        private const val TOP = 1 shl 1
        private const val RIGHT = 1 shl 2
        private const val BOTTOM = 1 shl 3

        /**
         * 启用左部边缘分割线
         */
        fun left() = Edge(LEFT)

        /**
         * 启用顶部边缘分割线
         */
        fun top() = Edge(TOP)

        /**
         * 启用右部边缘分割线
         */
        fun right() = Edge(RIGHT)

        /**
         * 启用底部边缘分割线
         */
        fun bottom() = Edge(BOTTOM)

        /**
         * 启用顶部和底部边缘分割线
         */
        fun vertical() = Edge(TOP or BOTTOM)

        /**
         * 启用左部和右部边缘分割线
         */
        fun horizontal() = Edge(LEFT or RIGHT)

        /**
         * 启用全部方向的边缘分割线
         */
        fun all() = Edge(LEFT or TOP or RIGHT or BOTTOM)
    }
}