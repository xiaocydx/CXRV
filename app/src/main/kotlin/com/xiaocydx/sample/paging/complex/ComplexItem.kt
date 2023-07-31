package com.xiaocydx.sample.paging.complex

import androidx.annotation.ColorInt

/**
 * @author xcc
 * @date 2023/7/30
 */
data class ComplexItem(
    val id: String,
    val linkUrl: String,
    val coverUrl: String,
    val title: String,
    val type: String,
    @ColorInt val typeColor: Int = typeColor(type)
) {
    companion object {
        const val TYPE_VIDEO = "视频"
        const val TYPE_AD = "广告"

        private fun typeColor(type: String) = when (type) {
            TYPE_VIDEO -> 0xFFAA5458.toInt()
            TYPE_AD -> 0xFF79AA91.toInt()
            else -> throw IllegalArgumentException()
        }
    }
}