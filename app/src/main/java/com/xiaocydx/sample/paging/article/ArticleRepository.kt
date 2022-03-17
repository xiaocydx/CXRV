package com.xiaocydx.sample.paging.article

import com.xiaocydx.recycler.paging.LoadResult
import com.xiaocydx.recycler.paging.Pager
import com.xiaocydx.recycler.paging.PagingConfig
import com.xiaocydx.sample.retrofit.WanAndroidApi

/**
 * @author xcc
 * @date 2022/3/17
 */
class ArticleRepository(private val api: WanAndroidApi) {
    private val pager = Pager(
        initKey = 0,
        config = PagingConfig(pageSize = 15, initPageSize = 30)
    ) { params ->
        val list = api.getArticleList(params.key, params.pageSize)
        val nextKey = if (list.over) null else params.key + 1
        LoadResult.Success(list.datas, nextKey)
    }

    val flow = pager.flow

    fun refresh() {
        pager.refresh()
    }
}