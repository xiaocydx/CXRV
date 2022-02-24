package com.xiaocydx.sample.multitype.onetomany

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import com.xiaocydx.sample.CustomLayout
import com.xiaocydx.sample.dp
import kotlin.math.max

/**
 * @author xcc
 * @date 2022/2/17
 */
abstract class MessageLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CustomLayout(context, attrs, defStyleAttr) {
    val ivAvatar: ImageView = AppCompatImageView(context).apply {
        withLayoutParams(48.dp, 48.dp)
        scaleType = ImageView.ScaleType.CENTER_CROP
    }.also(::addView)

    val tvUsername: TextView = AppCompatTextView(context).apply {
        withLayoutParams().apply {
            leftMargin = 12.dp
        }
        setTextSizeDp(14f)
        setTextColor(0xFF000000.toInt())
        includeFontPadding = false
    }.also(::addView)

    val container: FrameLayout = FrameLayout(context).apply {
        withLayoutParams().apply {
            leftMargin = 12.dp
            topMargin = 12.dp
        }
    }.also(::addView)

    init {
        setPadding(16.dp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        ivAvatar.autoMeasure()
        tvUsername.autoMeasure()
        container.also {
            it.measure(254.dp.toAtMostSpec(), it.defaultHeightSpec)
        }
        val maxHeight = max(
            ivAvatar.measuredHeight,
            tvUsername.measureHeightWithMargins + container.measureHeightWithMargins
        )
        setMeasuredDimension(measuredWidth, maxHeight + verticalPadding)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        ivAvatar.layout(paddingTop, paddingStart)
        tvUsername.layout(ivAvatar.right + tvUsername.marginStart, ivAvatar.top)
        container.layout(
            ivAvatar.right + container.marginStart,
            tvUsername.bottom + container.marginTop
        )
    }
}