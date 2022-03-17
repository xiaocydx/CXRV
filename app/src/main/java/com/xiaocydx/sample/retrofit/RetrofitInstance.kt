package com.xiaocydx.sample.retrofit

import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * @author xcc
 * @date 2022/3/16
 */
object RetrofitInstance {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.wanandroid.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .addCallAdapterFactory(TransformCallAdapterFactory(
            defaultResponseClass = BaseResponse::class,
            exceptionTransform = ::exceptionTransform
        ))
        .build()

    inline fun <reified T : Any> create(): T {
        return create(T::class.java)
    }

    fun <T : Any> create(clazz: Class<T>): T {
        return retrofit.create(clazz)
    }

    private fun exceptionTransform(
        exception: Throwable
    ): Throwable = when (exception) {
        is CustomHttpException -> exception
        is HttpException -> exception.run {
            CustomHttpException(code(), message(), this)
        }
        else -> CustomHttpException(-1, exception.message, exception)
    }
}

class CustomHttpException(
    val code: Int,
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)