package com.xiaocydx.cxrv.paging

import androidx.annotation.IntRange

/**
 * 分页数据的预取策略
 *
 * @author xcc
 * @date 2022/11/30
 */
sealed class PagingPrefetch {
    /**
     * 不需要预取分页数据
     */
    object None : PagingPrefetch()

    /**
     * 依靠RecyclerView的预取机制预取分页数据
     *
     * **注意**：对RecyclerView禁用预取机制（默认启用），该策略会无效。
     */
    object Default : PagingPrefetch()

    /**
     * 在[Default]的基础上，提前[value]个item预取分页数据
     *
     * **注意**：对RecyclerView禁用预取机制（默认启用），该策略仍有效。
     */
    class ItemCount(@IntRange(from = 1) val value: Int) : PagingPrefetch()
}