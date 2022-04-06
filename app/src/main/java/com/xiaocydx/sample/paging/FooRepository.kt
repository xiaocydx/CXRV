package com.xiaocydx.sample.paging

import com.xiaocydx.recycler.paging.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

/**
 * @author xcc
 * @date 2022/2/17
 */
class FooRepository(private val source: FooSource) {

    fun getFooFlow(initKey: Int, config: PagingConfig): Flow<PagingData<Foo>> {
        return getFooPager(initKey, config).flow
    }

    fun getFooPager(initKey: Int, config: PagingConfig): Pager<Int, Foo> {
        return Pager(initKey, config, source)
    }

    fun enableMultiTypeFoo() {
        source.multiTypeFoo = true
    }

    fun createFoo(num: Int, tag: String): Foo {
        return source.createFoo(num, tag)
    }
}

class FooSource(
    private val maxKey: Int,
    private val resultType: ResultType,
    var multiTypeFoo: Boolean = false
) : PagingSource<Int, Foo> {
    private var retryCount: Int = when (resultType) {
        is ResultType.RefreshEmpty -> resultType.retryCount
        is ResultType.AppendEmpty -> resultType.retryCount
        is ResultType.RefreshFailure -> resultType.retryCount
        is ResultType.AppendFailure -> resultType.retryCount
        else -> 0
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, Foo> = when (resultType) {
        ResultType.Normal -> normalResult(params)
        ResultType.Empty,
        is ResultType.RefreshEmpty,
        is ResultType.AppendEmpty -> emptyResult(params)
        is ResultType.RefreshFailure,
        is ResultType.AppendFailure -> failureResult(params)
    }

    fun createFoo(
        num: Int,
        tag: String = javaClass.simpleName
    ): Foo {
        val type = when {
            !multiTypeFoo -> FooType.TYPE1
            num % 2 != 0 -> FooType.TYPE1
            else -> FooType.TYPE2
        }
        return Foo(id = "$tag-$num", name = "Foo-$num", num, type)
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
}

sealed class ResultType {
    object Normal : ResultType()
    object Empty : ResultType()
    class RefreshEmpty(val retryCount: Int) : ResultType()
    class AppendEmpty(val retryCount: Int) : ResultType()
    class RefreshFailure(val retryCount: Int) : ResultType()
    class AppendFailure(val retryCount: Int) : ResultType()
}