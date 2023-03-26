package com.xiaocydx.sample.nested

/**
 * @author xcc
 * @date 2022/4/6
 */
data class OuterItem(
    val id: String,
    val title: String,
    val data: List<InnerItem>
)

data class InnerItem(
    val id: String,
    val title: String
)