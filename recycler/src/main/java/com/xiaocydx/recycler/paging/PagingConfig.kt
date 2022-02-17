package com.xiaocydx.recycler.paging

/**
 * 分页配置，用于从[PagingSource]加载结果
 *
 * @author xcc
 * @date 2021/9/13
 */
data class PagingConfig(
    /**
     * [PagingSource]一次加载的页面大小
     */
    val pageSize: Int,

    /**
     * [PagingSource]初始化加载的页面大小，用于首次加载大于[pageSize]的场景。
     */
    val initPageSize: Int = pageSize
) {

    companion object {
        const val Invalid = -1
        val InvalidPageSize = PagingConfig(pageSize = Invalid)
    }
}