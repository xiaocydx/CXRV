package com.xiaocydx.sample.multitype.onetomany

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding

/**
 * @author xcc
 * @date 2021/10/10
 */
class MessageTextLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MessageLayout(context, attrs, defStyleAttr) {

    val tvContent: TextView = AppCompatTextView(context).apply {
        setPadding(12.dp)
        setTextColor(0xFFFFFFFF.toInt())
        setTextSize(TypedValue.COMPLEX_UNIT_PX, 15.sp.toFloat())
        setBackgroundColor(0xFF5998FF.toInt())
        includeFontPadding = false
    }.also(::setContentView)
}