package com.xiaocydx.sample.multitype.onetomany

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.setPadding
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2021/10/10
 */
class MessageTextLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MessageLayout(context, attrs, defStyleAttr) {

    val tvContent: TextView = AppCompatTextView(context).apply {
        setPadding(12.dp)
        setTextSizeDp(15f)
        setTextColor(0xFFFFFFFF.toInt())
        setBackgroundColor(0xFF5998FF.toInt())
        includeFontPadding = false
    }.also(container::addView)
}