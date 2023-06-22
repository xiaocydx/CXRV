package com.xiaocydx.sample.viewpager2.shared

import android.content.Context
import android.graphics.Outline
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.Px
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import com.xiaocydx.sample.CustomLayout
import com.xiaocydx.sample.withLayoutParams
import com.xiaocydx.sample.wrapContent

/**
 * @author xcc
 * @date 2022/8/6
 */
class FooItemView(
    context: Context,
    attrs: AttributeSet? = null
) : CustomLayout(context, attrs) {
    val imageView: ImageView = AppCompatImageView(context).apply {
        addView(this, 80.dp, 80.dp) {
            leftMargin = 12.dp
            verticalMargin = 12.dp
        }
    }

    val textView: TextView = AppCompatTextView(context).apply {
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
        typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 15.sp.toFloat())
        addView(this, wrapContent, wrapContent) { horizontalMargin = 15.dp }
    }

    init {
        clipToOutline = true
        outlineProvider = RoundRectOutlineProvider(4.dp.toFloat())
        setBackgroundColor(0xFFBFD3FF.toInt())
    }

    init {
        withLayoutParams(matchParent, wrapContent)
    }

    override fun onMeasureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        imageView.measureWithDefault()
        textView.measureWith(
            textView.maxWidthMeasureSpec(
                size = measuredWidth
                        - imageView.measuredWidthWithMargins
                        - textView.horizontalMargin
            ),
            textView.defaultHeightMeasureSpec()
        )
        setMeasuredDimension(measuredWidth, imageView.measuredHeightWithMargins)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        imageView.let { it.layout(x = it.marginLeft, y = it.marginTop) }
        textView.let { it.layout(x = imageView.right + it.marginLeft, y = it.alignCenterY(imageView)) }
    }

    private class RoundRectOutlineProvider(@Px private val corners: Float) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, corners)
        }
    }
}