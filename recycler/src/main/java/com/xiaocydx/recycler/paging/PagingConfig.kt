package com.xiaocydx.recycler.paging

import com.xiaocydx.recycler.paging.LoadType.APPEND

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
    val initPageSize: Int = pageSize,

    /**
     * 当[LoadResult.Success]的`data`为空且`nextKey`不为空时，
     * 若该属性为`true`，则保持加载状态和加载类型，按`nextKey`获取下一页，
     * 若该属性为`false`，则认为这是一种异常情况，按[LoadResult.Failure]处理。
     */
    val loadResultEmptyFetchNext: Boolean = true,

    /**
     * 当[APPEND]加载失败时，若该属性为`true`，则会自动重试，
     * 自动重试的含义是不需要点击重试，时机是再次满足[APPEND]加载的条件，
     * 例如滑动列表，若最后一个item再次可视，则自动重试。
     */
    val appendFailureAutToRetry: Boolean = true
) {

    companion object {
        const val Invalid = -1
        val InvalidPageSize = PagingConfig(pageSize = Invalid)
    }
}