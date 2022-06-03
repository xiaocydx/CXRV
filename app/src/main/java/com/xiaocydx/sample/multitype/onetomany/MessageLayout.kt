package com.xiaocydx.sample.multitype.onetomany

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import androidx.core.view.setMargins
import com.xiaocydx.sample.CustomLayout
import kotlin.math.max

/**
 * @author xcc
 * @date 2022/2/17
 */
abstract class MessageLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CustomLayout(context, attrs, defStyleAttr) {
    private var contentView: View? = null
    private val maxContentWidth = 254.dp

    val ivAvatar: ImageView = AppCompatImageView(context).apply {
        addView(this, 48.dp, 48.dp)
        scaleType = ImageView.ScaleType.CENTER_CROP
    }

    val tvUsername: TextView = AppCompatTextView(context).apply {
        addView(this, wrapContent, wrapContent) { leftMargin = 12.dp }
        setTextColor(0xFF000000.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 14.sp.toFloat())
        includeFontPadding = false
    }

    init {
        layoutParams = LayoutParams(matchParent, wrapContent)
            .apply { setMargins(16.dp) }
    }

    fun setContentView(view: View) {
        contentView?.let(::removeView)
        contentView = view
        val lp = view.layoutParams
        val width = lp?.width ?: wrapContent
        val height = lp?.height ?: wrapContent
        addView(view, width, height) { topMargin = 12.dp }
    }

    override fun onMeasureChildren(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        ivAvatar.measureWithDefault()
        tvUsername.measureWithDefault()
        contentView?.measureWith(
            contentView!!.maxWidthMeasureSpec(maxContentWidth),
            contentView!!.defaultHeightMeasureSpec()
        )
        val measuredHeight = max(
            ivAvatar.measuredHeight,
            tvUsername.measuredHeight + (contentView?.measuredHeightWithMargins ?: 0)
        )
        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        ivAvatar.layout(x = 0, y = 0)
        tvUsername.layout(x = ivAvatar.right + tvUsername.marginStart, y = ivAvatar.top)
        contentView?.layout(x = tvUsername.left, y = tvUsername.bottom + contentView!!.marginTop)
    }
}