package com.xiaocydx.sample.retrofit

import androidx.annotation.CheckResult
import retrofit2.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * 实现[ResponseContract]的类，是跟服务端约定的响应Body，例如：
 * ```
 * data class BaseResponse<T>(
 *     val data: T? = null,
 *     val errorCode: Int = -1,
 *     val errorMsg: String? = null
 * ) : ResponseContract {
 *
 *     override fun getOrNull(): Any? = data
 *
 *     override fun exceptionOrNull(): Throwable? = when (errorCode) {
 *         SUCCEED_CODE -> null
 *         else -> IllegalArgumentException("errorCode = $errorCode, errorMsg = $errorMsg")
 *     }
 * }
 * ```
 */
interface ResponseContract {
    /**
     * 返回数据，若没有则返回`null`
     */
    fun getOrNull(): Any?

    /**
     * 返回异常，若没有则返回`null`
     *
     * 返回的异常会通过[TransformCallAdapterFactory.exceptionTransform]进行转换
     */
    fun exceptionOrNull(): Throwable?
}

/**
 * 将`Call<T>`或者挂起函数的返回值，按[responseClass]进行转换、解析，例如：
 * ```
 * interface ApiService {
 *
 *     @GET("foo/list")
 *     @Transform(BaseResponse::class)
 *     fun getFooList(): Call<List<Foo>>
 *
 *     @GET("foo/list")
 *     @Transform(BaseResponse::class)
 *     suspend fun getFooListSuspend(): List<Foo>
 * }
 * ```
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Transform(val responseClass: KClass<out ResponseContract>)

/**
 * 实现[Transform]注解功能的[CallAdapter]工厂类
 *
 * 1. 若函数没有[Transform]注解，则按[defaultResponseClass]进行转换、解析。
 * 2. [Callback.onFailure]的异常和[ResponseContract.exceptionOrNull]的异常，
 * 会通过[exceptionTransform]进行转换，转换结果可以是自定义HttpException。
 */
class TransformCallAdapterFactory(
    private val defaultResponseClass: KClass<out ResponseContract>? = null,
    private val exceptionTransform: ((exception: Throwable) -> Throwable)? = null
) : CallAdapter.Factory() {

    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (getRawType(returnType) != Call::class.java) {
            return null
        }
        require(returnType is ParameterizedType) {
            "returnType = ${returnType}，不是ParameterizedType"
        }

        val dataType = getParameterUpperBound(0, returnType)
        if (ResponseContract::class.java.isAssignableFrom(getRawType(dataType))) {
            return null
        }

        val transform = annotations
            .firstNotNullOfOrNull { it as? Transform }
        val rawType: Class<out ResponseContract> = when {
            transform != null -> transform.responseClass.java
            else -> defaultResponseClass?.java
        } ?: return null

        return TransformCallAdapter(
            rawType = rawType,
            dataType = dataType,
            next = retrofit.nextCallAdapter(this, returnType, annotations),
            exceptionTransform = exceptionTransform
        )
    }
}

/**
 * 实现[Transform]注解功能的[CallAdapter]
 *
 * [Callback.onFailure]的异常和[ResponseContract.exceptionOrNull]的异常，
 * 会通过[exceptionTransform]进行转换，转换结果可以是自定义HttpException。
 */
private class TransformCallAdapter(
    rawType: Class<out ResponseContract>,
    private val dataType: Type,
    private val next: CallAdapter<*, *>,
    private val exceptionTransform: ((exception: Throwable) -> Throwable)? = null
) : CallAdapter<Any, Call<*>> {
    private val responseType = ParameterizedTypeImpl(rawType, dataType)

    override fun responseType(): Type = responseType

    @Suppress("UNCHECKED_CAST")
    override fun adapt(call: Call<Any>): Call<*> {
        val delegate = (next as CallAdapter<Any, Any>).adapt(call)
        require(delegate is Call<*>) {
            "${next.javaClass.simpleName}.adapt()的返回值类型不是Call，无法做转换处理"
        }
        require(next.responseType() == dataType) {
            "${next.javaClass.simpleName}.responseType()和Call<T>的T不一致，无法做转换处理"
        }
        return CallImpl(delegate, exceptionTransform)
    }

    private data class ParameterizedTypeImpl(
        private val rawType: Type,
        private val responseType: Type
    ) : ParameterizedType {
        override fun getOwnerType(): Type? = null

        override fun getRawType(): Type = rawType

        override fun getActualTypeArguments(): Array<Type> = arrayOf(responseType)
    }

    private class CallImpl<T>(
        private val delegate: Call<T>,
        private val exceptionTransform: ((exception: Throwable) -> Throwable)? = null
    ) : Call<T> by delegate {
        private val receiver: Call<T> = this

        override fun execute(): Response<T> {
            return delegate.execute().transform()
        }

        override fun enqueue(callback: Callback<T>) {
            delegate.enqueue(object : Callback<T> {
                override fun onResponse(call: Call<T>, response: Response<T>) {
                    val exception = response.exceptionOrNull()
                    if (exception != null) {
                        callback.onFailure(receiver, exception.transform())
                    } else {
                        callback.onResponse(receiver, response.transform())
                    }
                }

                override fun onFailure(call: Call<T>, exception: Throwable) {
                    callback.onFailure(receiver, exception.transform())
                }
            })
        }

        override fun clone(): Call<T> {
            return CallImpl(delegate, exceptionTransform)
        }

        @CheckResult
        private fun Throwable.transform(): Throwable {
            return exceptionTransform?.invoke(this) ?: this
        }

        @CheckResult
        @Suppress("UNCHECKED_CAST")
        private fun Response<T>.transform(): Response<T> = when {
            isSuccessful -> {
                val body = body() as? ResponseContract
                        ?: throw AssertionError("转换过程出现断言异常")
                Response.success(body.getOrNull(), raw()) as Response<T>
            }
            else -> this
        }

        private fun Response<T>.exceptionOrNull(): Throwable? {
            if (!isSuccessful) {
                return HttpException(this)
            }
            return when (val body = body()) {
                null -> {
                    // 该分支代码copy自Call.await()
                    val invocation = delegate.request().tag(Invocation::class.java)!!
                    val method = invocation.method()
                    NullPointerException("Response from " +
                            method.declaringClass.name +
                            '.' +
                            method.name +
                            " was null but response body type was declared as non-null")
                }
                !is ResponseContract -> AssertionError("转换过程出现断言异常")
                else -> body.exceptionOrNull()
            }
        }
    }
}