package com.xiaocydx.sample.retrofit

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * @author xcc
 * @date 2022/3/16
 */
interface WanAndroidApi {

    @GET("article/list/{key}/json")
    suspend fun getArticleList(
        @Path("key") key: Int,
        @Query("page_size") pageSize: Int
    ): ArticleList
}