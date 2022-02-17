package com.xiaocydx.sample.paging

import com.xiaocydx.recycler.paging.LoadParams
import com.xiaocydx.recycler.paging.LoadResult
import kotlinx.coroutines.delay

/**
 * @author xcc
 * @date 2022/2/17
 */
class FooDataSource(
    private val maxKey: Int,
    private val resultType: ResultType
) {
    private var retryCount: Int = when (resultType) {
        is ResultType.RefreshFailure -> resultType.retryCount
        is ResultType.AppendFailure -> resultType.retryCount
        else -> 0
    }
    var multiTypeFoo = false

    suspend fun loadResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        return when (resultType) {
            ResultType.Normal -> normalResult(params)
            ResultType.Empty -> emptyResult()
            ResultType.RefreshEmpty -> refreshEmptyResult(params)
            is ResultType.RefreshFailure -> failureResult(params)
            is ResultType.AppendFailure -> failureResult(params)
        }
    }

    private suspend fun normalResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        val range = when (params) {
            is LoadParams.Refresh -> {
                delay(600)
                (1..params.pageSize)
            }
            is LoadParams.Append -> {
                delay(600)
                val start = params.pageSize * (params.key - 1) + 1
                val end = start + params.pageSize - 1
                start..end
            }
        }

        val data = range.map { createFoo(num = it) }
        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    private suspend fun emptyResult(): LoadResult<Int, Foo> {
        delay(600)
        return LoadResult.Success(listOf(), nextKey = null)
    }

    private suspend fun refreshEmptyResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        val data = when (params) {
            is LoadParams.Refresh -> {
                delay(600)
                emptyList()
            }
            is LoadParams.Append -> {
                delay(600)
                val start = params.pageSize * (params.key - 1) + 1
                val end = start + params.pageSize - 1
                (start..end).map { createFoo(num = it) }
            }
        }

        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    private suspend fun failureResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        val range = when (params) {
            is LoadParams.Refresh -> {
                delay(600)
                (1..params.pageSize)
            }
            is LoadParams.Append -> {
                delay(600)
                val start = params.pageSize * (params.key - 1) + 1
                val end = start + params.pageSize - 1
                start..end
            }
        }
        val isRetryNeeded = when {
            retryCount <= 0 -> false
            resultType is ResultType.RefreshFailure
                    && params is LoadParams.Refresh -> true
            resultType is ResultType.AppendFailure
                    && params is LoadParams.Append -> true
            else -> false
        }
        if (isRetryNeeded) {
            --retryCount
            return LoadResult.Failure(IllegalArgumentException())
        }

        val data = range.map { createFoo(num = it) }
        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    fun createFoo(
        num: Int,
        tag: String = this::class.java.simpleName
    ): Foo {
        val type = when {
            !multiTypeFoo -> FooType.TYPE1
            num % 2 != 0 -> FooType.TYPE1
            else -> FooType.TYPE2
        }
        return Foo(id = "$tag-$num", name = "Foo-$num", num, type)
    }
}

sealed class ResultType {
    object Normal : ResultType()
    object Empty : ResultType()
    object RefreshEmpty : ResultType()
    class RefreshFailure(val retryCount: Int) : ResultType()
    class AppendFailure(val retryCount: Int) : ResultType()
}