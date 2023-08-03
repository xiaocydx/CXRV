package com.xiaocydx.sample.paging.complex

/**
 * @author xcc
 * @date 2023/8/3
 */
data class VideoStreamItem(
    val id: String,
    val videoUrl: String,
    val coverUrl: String,
    val title: String
)

fun List<ComplexItem>.toViewStreamList() = mapNotNull {
    if (it.type != ComplexItem.TYPE_VIDEO) return@mapNotNull null
    VideoStreamItem(it.id, it.linkUrl, it.coverUrl, it.title)
}