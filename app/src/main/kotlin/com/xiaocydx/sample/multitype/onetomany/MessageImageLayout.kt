package com.xiaocydx.sample.multitype.onetomany

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import com.xiaocydx.sample.layoutParams

/**
 * @author xcc
 * @date 2021/10/10
 */
class MessageImageLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : MessageLayout(context, attrs, defStyleAttr) {

    val ivContent: ImageView = AppCompatImageView(context).apply {
        layoutParams(150.dp, 150.dp)
        scaleType = ImageView.ScaleType.CENTER_CROP
    }.also(::setContentView)
}