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
    @ColorInt val typeColor: Int = typeColor(type),
    val dimensionRatio: String = dimensionRatio(type)
) {
    companion object {
        const val TYPE_VIDEO = "视频"
        const val TYPE_IMAGE = "图片"
        const val TYPE_AD = "广告"

        private fun typeColor(type: String) = when (type) {
            TYPE_VIDEO -> 0xFFAA5458.toInt()
            TYPE_IMAGE -> 0xFF79AA91.toInt()
            TYPE_AD -> 0xFF688CAA.toInt()
            else -> throw IllegalArgumentException()
        }

        private fun dimensionRatio(type: String) = when (type) {
            TYPE_VIDEO -> "3:4"
            TYPE_IMAGE -> "1:1"
            TYPE_AD -> "4:3"
            else -> throw IllegalArgumentException()
        }
    }
}