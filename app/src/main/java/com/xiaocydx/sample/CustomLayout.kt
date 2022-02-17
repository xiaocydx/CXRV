package com.xiaocydx.sample

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.View.MeasureSpec.*
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.annotation.Px
import androidx.core.view.*
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max

/**
 * 提供测量、布局相关扩展的自定义ViewGroup
 *
 * @author xcc
 * @date 2022/2/16
 */
abstract class CustomLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    /**
     * 若[CustomLayout]的`parent`是RecyclerView、ScrollView等滑动控件，
     * 则传入的测量规格的模式可能为[UNSPECIFIED]，这取决于`layoutParams`的宽高值。
     *
     * [onMeasure]的默认实现，若测量规格的模式为[UNSPECIFIED]，则保存`suggestedMinimum`值，
     * 这不符合[CustomLayout]的意图，[CustomLayout]在测量阶段希望给到子View最大可用空间，
     * 因此修改[onMeasure]的默认实现，当`parent`是滑动控件时，保存调整后的默认值。
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = getDefaultSize(
            measureSpec = widthMeasureSpec,
            layoutParamsSize = { layoutParams?.width ?: 0 },
            suggestedMinimumSize = { suggestedMinimumWidth }
        )
        val measuredHeight = getDefaultSize(
            measureSpec = heightMeasureSpec,
            layoutParamsSize = { layoutParams?.height ?: 0 },
            suggestedMinimumSize = { suggestedMinimumHeight }
        )
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    private inline fun getDefaultSize(
        measureSpec: Int,
        layoutParamsSize: () -> Int,
        suggestedMinimumSize: () -> Int
    ): Int {
        val specMode = measureSpec.specMode
        val specSize = measureSpec.specSize
        return when (specMode) {
            UNSPECIFIED -> {
                if ((parent as? View)?.isScrollContainer == true) {
                    val size = layoutParamsSize()
                    if (size >= 0) size else specSize
                } else suggestedMinimumSize()
            }
            AT_MOST, EXACTLY -> specSize
            else -> throw IllegalArgumentException("无效的measureSpec。")
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (parent is RecyclerView) {
            // 若宽高值为WRAP_CONTENT，则调整为MATCH_PARENT，
            // 确保onMeasure()测量规格的specSize不为0。
            val lp = layoutParams ?: return
            lp.width = max(lp.width, MATCH_PARENT)
            lp.height = max(lp.height, MATCH_PARENT)
        }
    }

    //region 间距相关扩展，后续考虑迁出
    @get:Px
    protected inline val View.horizontalMargin: Int
        get() = marginLeft + marginRight

    @get:Px
    protected inline val View.verticalMargin: Int
        get() = marginTop + marginBottom

    @get:Px
    protected inline var View.horizontalPadding: Int
        get() = paddingLeft + paddingRight
        set(value) {
            updatePadding(left = value, right = value)
        }

    @get:Px
    protected inline var View.verticalPadding: Int
        get() = paddingTop + paddingBottom
        set(value) {
            updatePadding(top = value, bottom = value)
        }

    @get:Px
    protected inline val View.measureHeightWithMargins: Int
        get() = measuredHeight + verticalMargin

    @get:Px
    protected inline val View.measureWidthWithMargins: Int
        get() = measuredWidth + horizontalMargin
    //endregion

    //region 测量相关扩展
    protected inline val Int.specSize: Int
        get() = getSize(this)

    protected inline val Int.specMode: Int
        get() = getMode(this)

    protected fun Int.toUnspecifiedMeasureSpec(): Int {
        return makeMeasureSpec(this, UNSPECIFIED)
    }

    protected fun Int.toExactlyMeasureSpec(): Int {
        return makeMeasureSpec(this, EXACTLY)
    }

    protected fun Int.toAtMostMeasureSpec(): Int {
        return makeMeasureSpec(this, AT_MOST)
    }

    protected val View.defaultWidthMeasureSpec: Int
        get() {
            val parent = requireNotNull(
                value = parent as? ViewGroup,
                lazyMessage = { "parent不能为空。" }
            )
            return when (layoutParams.width) {
                MATCH_PARENT -> parent.measuredWidth.toExactlyMeasureSpec()
                WRAP_CONTENT -> parent.measuredWidth.toAtMostMeasureSpec()
                else -> layoutParams.width.toExactlyMeasureSpec()
            }
        }

    protected val View.defaultHeightMeasureSpec: Int
        get() {
            val parent = requireNotNull(
                value = parent as? ViewGroup,
                lazyMessage = { "parent不能为空。" }
            )
            return when (layoutParams.height) {
                MATCH_PARENT -> parent.measuredHeight.toExactlyMeasureSpec()
                WRAP_CONTENT -> parent.measuredHeight.toAtMostMeasureSpec()
                else -> layoutParams.height.toExactlyMeasureSpec()
            }
        }

    protected fun View.autoMeasure() {
        measure(defaultWidthMeasureSpec, defaultHeightMeasureSpec)
    }
    //endregion

    //region 布局相关扩展
    protected fun View.layout(left: Int, top: Int) {
        layout(left, top, left + measuredWidth, top + measuredHeight)
    }
    //endregion

    //region TextView相关扩展，后续考虑迁出
    protected var TextView.isFakeBoldText: Boolean
        get() = paint.isFakeBoldText
        set(value) {
            paint.isFakeBoldText = value
        }

    protected fun TextView.setTextSizeDp(size: Float) {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, size)
    }
    //endregion

    //region MarginLayoutParams相关扩展，后续考虑迁出
    @get:Px
    @setparam:Px
    protected inline var MarginLayoutParams.horizontalMargin: Int
        get() = leftMargin + rightMargin
        set(value) {
            updateMargin(left = value, right = value)
        }

    @get:Px
    @setparam:Px
    protected inline var MarginLayoutParams.verticalMargin: Int
        get() = topMargin + bottomMargin
        set(value) {
            updateMargin(top = value, bottom = value)
        }

    protected fun View.withLayoutParams(
        @Px width: Int = WRAP_CONTENT,
        @Px height: Int = WRAP_CONTENT
    ): MarginLayoutParams {
        return LayoutParams(width, height).also { layoutParams = it }
    }

    protected fun MarginLayoutParams.setMargin(@Px value: Int) {
        updateMargin(value, value, value, value)
    }

    protected fun MarginLayoutParams.updateMargin(
        @Px left: Int = leftMargin,
        @Px top: Int = topMargin,
        @Px right: Int = rightMargin,
        @Px bottom: Int = bottomMargin
    ) {
        leftMargin = left
        topMargin = top
        rightMargin = right
        bottomMargin = bottom
    }

    protected class LayoutParams(width: Int, height: Int) : MarginLayoutParams(width, height)
    //endregion
}