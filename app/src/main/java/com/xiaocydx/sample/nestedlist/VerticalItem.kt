package com.xiaocydx.sample.nestedlist

/**
 * @author xcc
 * @date 2022/4/6
 */
data class VerticalItem(
    val id: String,
    val title: String,
    val data: List<HorizontalItem>
)

data class HorizontalItem(
    val id: String,
    val title: String
)