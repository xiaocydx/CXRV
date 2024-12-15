package com.xiaocydx.sample.paging.article

import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingConfig

/**
 * @author xcc
 * @date 2022/3/17
 */
open class ArticleRepository(private val api: WanAndroidApi) {

    open fun getArticlePager(
        initKey: Int,
        config: PagingConfig
    ): Pager<Int, ArticleData> = Pager(initKey, config) { params ->
        val list = api.getArticleList(params.key, params.pageSize)
        val nextKey = if (list.over) null else params.key + 1
        LoadResult.Success(list.datas, nextKey)
    }

    companion object Instance : ArticleRepository(RetrofitInstance.create())
}