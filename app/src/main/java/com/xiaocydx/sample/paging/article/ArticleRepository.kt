package com.xiaocydx.sample.paging.article

import com.xiaocydx.recycler.paging.LoadResult
import com.xiaocydx.recycler.paging.Pager
import com.xiaocydx.recycler.paging.PagingConfig
import com.xiaocydx.recycler.paging.PagingData
import com.xiaocydx.sample.retrofit.ArticleInfo
import com.xiaocydx.sample.retrofit.WanAndroidApi
import kotlinx.coroutines.flow.Flow

/**
 * @author xcc
 * @date 2022/3/17
 */
class ArticleRepository(private val api: WanAndroidApi) {

    fun getArticleFlow(
        initKey: Int,
        pageSize: Int
    ): Flow<PagingData<ArticleInfo>> = Pager(
        initKey = initKey,
        config = PagingConfig(pageSize)
    ) { params ->
        val list = api.getArticleList(params.key, params.pageSize)
        val nextKey = if (list.over) null else params.key + 1
        LoadResult.Success(list.datas, nextKey)
    }.flow
}