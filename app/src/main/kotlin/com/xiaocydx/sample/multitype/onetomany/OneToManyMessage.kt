package com.xiaocydx.sample.multitype.onetomany

import androidx.annotation.DrawableRes

/**
 * @author xcc
 * @date 2022/2/17
 */
data class OneToManyMessage(
    val id: Int,
    @DrawableRes
    val avatar: Int,
    val username: String,
    val type: String,
    val content: String = "",
    @DrawableRes
    val image: Int = 0
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_IMAGE = "image"
        const val TYPE_UNKNOWN = "unknown"
    }
}