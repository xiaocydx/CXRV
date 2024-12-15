package com.xiaocydx.sample.paging.article

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * [玩Android 开放API](https://wanandroid.com/blog/show/2;jsessionid=1685FA404C8DEE92AE71DD5AB7E56D02)
 *
 * @author xcc
 * @date 2022/3/16
 */
interface WanAndroidApi {

    @GET("article/list/{key}/json")
    suspend fun getArticleList(@Path("key") key: Int, @Query("page_size") pageSize: Int): ArticleList
}