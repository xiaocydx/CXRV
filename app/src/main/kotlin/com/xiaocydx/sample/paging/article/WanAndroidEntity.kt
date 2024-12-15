package com.xiaocydx.sample.paging.article

import com.xiaocydx.accompanist.retrofit.ResponseContract

data class BaseResponse<T>(
    val data: T? = null,
    val errorCode: Int = -1,
    val errorMsg: String? = null
) : ResponseContract {

    override fun getOrNull(): Any? = data

    override fun exceptionOrNull(): Throwable? = when (errorCode) {
        SUCCEED_CODE -> null
        else -> CustomHttpException(errorCode, errorMsg)
    }

    private companion object {
        const val SUCCEED_CODE = 0
    }
}

data class ArticleList(
    val datas: List<ArticleData>,
    val over: Boolean = false,
    val pageCount: Int = 0,
    val size: Int = 0,
    val total: Int = 0
)

data class ArticleData(
    val id: Int = 0,
    val title: String? = null,
    val link: String? = null,
    val author: String? = null,
    val shareUser: String? = null,
    val chapterName: String? = null,
    val superChapterName: String? = null
)