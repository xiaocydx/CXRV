package com.xiaocydx.sample.retrofit

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
    val datas: List<ArticleInfo>,
    val over: Boolean = false,
    val pageCount: Int = 0,
    val size: Int = 0,
    val total: Int = 0
)

data class ArticleInfo(
    val id: Int = 0,
    val title: String? = null,
    val author: String? = null,
    val link: String? = null
)