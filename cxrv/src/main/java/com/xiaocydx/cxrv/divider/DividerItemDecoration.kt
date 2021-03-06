package com.xiaocydx.cxrv.divider

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.Px
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.R
import kotlin.math.max

/**
 * 通用分割线
 *
 * @author xcc
 * @date 2021/9/29
 */
class DividerItemDecoration private constructor(config: Config) : ItemDecoration() {
    private var outRect: Rect = emptyRect
    private val divider: Drawable? = config.drawable
    private var state: State = emptyState
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
    internal val isGrid = width > 0 && height > 0
    internal val isSpacing = when (divider) {
        null -> true
        !is ColorDrawable -> false
        else -> divider.color == Color.TRANSPARENT
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        resetThen(state, outRect) {
            DividerStrategy.get(parent).getItemOffsets(view, parent, this)
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        if (isSpacing) {
            return
        }
        resetThen(state) {
            DividerStrategy.get(parent).onDraw(canvas, parent, this)
        }
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
    )  {
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

    /**
     * 执行item动画时，ViewHolder的localPosition为-1，计算的偏移结果是错误的，导致分割线显示异常，
     * 因此在[setItemOffsets]中记录之前计算的偏移量，在item被移除的时候，将记录值作为偏移输出结果。
     */
    internal fun View.setRemovedItemOffsets() {
        getItemOffsets()?.let(outRect::set)
    }

    internal fun Canvas.drawDivider(left: Int, top: Int, right: Int, bottom: Int) {
        divider?.also {
            it.setBounds(left, top, right, bottom)
            it.draw(this)
        }
    }

    private companion object {
        val emptyRect = Rect()
        val emptyState = State()
    }

    class Config
    @PublishedApi
    internal constructor(private val context: Context) {
        /**
         * 水平方向的分割线size
         */
        @Px var width = 0

        /**
         * 垂直方向的分割线size
         */
        @Px var height = 0

        /**
         * 分割线颜色值，搭配[width]和[height]使用
         */
        @setparam:ColorInt
        var color = -1
            set(value) {
                drawable = ColorDrawable(value)
            }

        /**
         * 分割线颜色资源Id，搭配[width]和[height]使用
         */
        @setparam:ColorRes
        var colorRes = -1
            set(value) {
                color = ContextCompat.getColor(context, value)
            }

        /**
         * 分割线Drawable资源Id
         */
        @setparam:DrawableRes
        var drawableRes = -1
            set(value) {
                drawable = requireNotNull(
                    value = ContextCompat.getDrawable(context, value),
                    lazyMessage = { "无法创建drawableRes = ${value}的Drawable" }
                )
            }

        /**
         * 分割线Drawable
         */
        var drawable: Drawable? = null

        /**
         * 是否启用左部边缘分割线
         */
        var leftEdge = false

        /**
         * 是否启用顶部边缘分割线
         */
        var topEdge = false

        /**
         * 是否启用右部边缘分割线
         */
        var rightEdge = false

        /**
         * 是否启用底部边缘分割线
         */
        var bottomEdge = false

        /**
         * 是否启用顶部和底部边缘分割线
         */
        var verticalEdge: Boolean
            get() = topEdge && bottomEdge
            set(value) {
                topEdge = value
                bottomEdge = value
            }

        /**
         * 是否启用左部和右部边缘分割线
         */
        var horizontalEdge: Boolean
            get() = leftEdge && rightEdge
            set(value) {
                leftEdge = value
                rightEdge = value
            }

        /**
         * 左部分割线间隔
         */
        @Px var leftMargin = 0

        /**
         * 顶部分割线间隔
         */
        @Px var topMargin = 0

        /**
         * 右部分割线间隔
         */
        @Px var rightMargin = 0

        /**
         * 底部分割线间隔
         */
        @Px var bottomMargin = 0

        /**
         * 垂直方向的分割线间隔
         */
        @get:Px
        @setparam:Px
        var verticalMargin: Int
            get() = if (topMargin == bottomMargin) topMargin else 0
            set(value) {
                topMargin = value
                bottomMargin = value
            }

        /**
         * 水平方向的分割线间隔
         */
        @get:Px
        @setparam:Px
        var horizontalMargin: Int
            get() = if (leftMargin == rightMargin) leftMargin else 0
            set(value) {
                leftMargin = value
                rightMargin = value
            }

        @PublishedApi
        internal fun build(): DividerItemDecoration {
            width = when {
                drawable == null -> width
                drawable!!.intrinsicWidth != -1 -> drawable!!.intrinsicWidth
                else -> width
            }.let { max(it, 0) }

            height = when {
                drawable == null -> height
                drawable!!.intrinsicHeight != -1 -> drawable!!.intrinsicHeight
                else -> height
            }.let { max(it, 0) }

            leftMargin = max(leftMargin, 0)
            topMargin = max(topMargin, 0)
            rightMargin = max(rightMargin, 0)
            bottomMargin = max(bottomMargin, 0)
            return DividerItemDecoration(this)
        }
    }
}