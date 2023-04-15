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
    /**
     * text、image
     */
    val type: String,
    val content: String = "",
    @DrawableRes
    val image: Int = 0
)