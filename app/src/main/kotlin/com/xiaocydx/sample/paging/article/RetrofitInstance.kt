/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.sample.paging.article

import com.xiaocydx.accompanist.retrofit.TransformCallAdapterFactory
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
            exceptionTransform = RetrofitInstance::exceptionTransform
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