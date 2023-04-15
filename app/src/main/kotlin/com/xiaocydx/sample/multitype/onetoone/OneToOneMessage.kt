package com.xiaocydx.sample.multitype.onetoone

import androidx.annotation.DrawableRes

/**
 * @author xcc
 * @date 2022/2/17
 */
sealed class OneToOneMessage {

    data class Text(
        val id: Int,
        @DrawableRes
        val avatar: Int,
        val username: String,
        val content: String,
    ) : OneToOneMessage()

    data class Image(
        val id: Int,
        @DrawableRes
        val avatar: Int,
        val username: String,
        val image: Int
    ) : OneToOneMessage()
}