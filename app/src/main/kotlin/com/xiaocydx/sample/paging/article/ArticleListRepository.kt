package com.xiaocydx.sample.paging.article

import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.sample.retrofit.ArticleInfo
import com.xiaocydx.sample.retrofit.RetrofitInstance
import com.xiaocydx.sample.retrofit.WanAndroidApi

/**
 * @author xcc
 * @date 2022/3/17
 */
open class ArticleListRepository(private val api: WanAndroidApi) {

    open fun getArticlePager(
        initKey: Int,
        config: PagingConfig
    ): Pager<Int, ArticleInfo> = Pager(initKey, config) { params ->
        val list = api.getArticleList(params.key, params.pageSize)
        val nextKey = if (list.over) null else params.key + 1
        LoadResult.Success(list.datas, nextKey)
    }

    companion object Instance : ArticleListRepository(RetrofitInstance.create())
}