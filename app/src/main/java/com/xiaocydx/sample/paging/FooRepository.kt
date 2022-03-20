package com.xiaocydx.sample.paging

import com.xiaocydx.recycler.paging.LoadParams
import com.xiaocydx.recycler.paging.LoadResult
import com.xiaocydx.recycler.paging.Pager
import com.xiaocydx.recycler.paging.PagingConfig
import kotlinx.coroutines.delay

/**
 * @author xcc
 * @date 2022/2/17
 */
class FooRepository(
    pageSize: Int,
    initKey: Int,
    private val maxKey: Int,
    private val resultType: ResultType,
) {
    private var retryCount: Int = when (resultType) {
        is ResultType.RefreshEmpty -> resultType.retryCount
        is ResultType.AppendEmpty -> resultType.retryCount
        is ResultType.RefreshFailure -> resultType.retryCount
        is ResultType.AppendFailure -> resultType.retryCount
        else -> 0
    }

    private val pager = Pager(
        initKey = initKey,
        config = PagingConfig(pageSize)
    ) { params ->
        when (resultType) {
            ResultType.Normal -> normalResult(params)
            ResultType.Empty,
            is ResultType.RefreshEmpty,
            is ResultType.AppendEmpty -> emptyResult(params)
            is ResultType.RefreshFailure,
            is ResultType.AppendFailure -> failureResult(params)
        }
    }

    var multiTypeFoo = false
    val flow = pager.flow

    fun refresh() {
        pager.refresh()
    }

    private suspend fun normalResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        delay(600)
        val start = params.pageSize * (params.key - 1) + 1
        val end = start + params.pageSize - 1
        val data = (start..end).map { createFoo(num = it) }
        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    private suspend fun emptyResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        delay(600)
        if (resultType is ResultType.Empty) {
            return LoadResult.Success(listOf(), nextKey = null)
        }

        val isEmptyNeeded = when {
            retryCount <= 0 -> false
            resultType is ResultType.RefreshEmpty
                    && params is LoadParams.Refresh -> true
            resultType is ResultType.AppendEmpty
                    && params is LoadParams.Append -> true
            else -> false
        }

        val data = if (isEmptyNeeded) {
            retryCount--
            emptyList()
        } else {
            val start = params.pageSize * (params.key - 1) + 1
            val end = start + params.pageSize - 1
            (start..end).map { createFoo(num = it) }
        }

        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    private suspend fun failureResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        delay(600)
        val start = params.pageSize * (params.key - 1) + 1
        val end = start + params.pageSize - 1
        val range = start..end

        val isRetryNeeded = when {
            retryCount <= 0 -> false
            resultType is ResultType.RefreshFailure
                    && params is LoadParams.Refresh -> true
            resultType is ResultType.AppendFailure
                    && params is LoadParams.Append -> true
            else -> false
        }
        if (isRetryNeeded) {
            retryCount--
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
    class RefreshEmpty(val retryCount: Int) : ResultType()
    class AppendEmpty(val retryCount: Int) : ResultType()
    class RefreshFailure(val retryCount: Int) : ResultType()
    class AppendFailure(val retryCount: Int) : ResultType()
}