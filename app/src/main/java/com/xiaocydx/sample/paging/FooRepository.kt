package com.xiaocydx.sample.paging

import com.xiaocydx.cxrv.paging.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
        source.enableMultiTypeFoo()
    }

    fun createFoo(num: Int, tag: String): Foo {
        return source.createFoo(num, tag)
    }
}

private val urls = arrayOf(
    "https://cdn.pixabay.com/photo/2022/07/01/14/29/wheat-7295718_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/10/18/21/allium-7313550_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/04/23/52/beach-7302072_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/05/11/06/mountains-7302806_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/12/11/30/morning-view-7317119_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/10/21/25/sky-7313775_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/13/07/10/branch-7318716_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/09/17/20/mushroom-7311371_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/07/06/12/11/spaceship-7304985_960_720.jpg",
    "https://cdn.pixabay.com/photo/2022/06/19/07/12/mount-kilimanjaro-7271184_960_720.jpg",
)
private val gifUrls = arrayOf(
    "https://alifei05.cfp.cn/creative/vcg/800/new/VCG211168385804.gif",
    "https://tenfei03.cfp.cn/creative/vcg/800/new/VCG211280897919.gif",
    "https://alifei03.cfp.cn/creative/vcg/800/new/VCG211280814110.gif",
    "https://tenfei01.cfp.cn/creative/vcg/800/new/VCG211166346104.gif",
    "https://alifei02.cfp.cn/creative/vcg/800/new/VCG211151553894.gif",
    "https://tenfei01.cfp.cn/creative/vcg/800/new/VCG211268665808.gif",
    "https://tenfei03.cfp.cn/creative/vcg/800/new/VCG211308028815.gif",
    "https://alifei05.cfp.cn/creative/vcg/800/new/VCG211267313448.gif",
    "https://alifei02.cfp.cn/creative/vcg/800/new/VCG211332127505.gif",
    "https://alifei05.cfp.cn/creative/vcg/800/new/VCG211267317119.gif"
)

class FooSource(
    private val maxKey: Int,
    private val resultType: ResultType,
    private val duration: Duration = 400L.milliseconds,
) : PagingSource<Int, Foo> {
    private var multiTypeFoo = false
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
        val url = urls[num % urls.size]
        return Foo(id = "$tag-$num", name = "Foo-$num", num, url, type)
    }

    fun enableMultiTypeFoo() {
        multiTypeFoo = true
    }

    private suspend fun normalResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        delay(duration)
        val start = params.pageSize * (params.key - 1) + 1
        val end = start + params.pageSize - 1
        val data = (start..end).map { createFoo(num = it) }
        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    private suspend fun emptyResult(params: LoadParams<Int>): LoadResult<Int, Foo> {
        delay(duration)
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
        delay(duration)
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