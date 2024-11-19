package com.xiaocydx.sample.common

import com.xiaocydx.cxrv.paging.LoadParams
import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingData
import com.xiaocydx.cxrv.paging.PagingSource
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

val urls = arrayOf(
    "https://cdn.pixabay.com/photo/2019/10/31/06/24/lake-4591082_1280.jpg",
    "https://cdn.pixabay.com/photo/2024/01/31/19/25/sunset-8544672_1280.jpg",
    "https://cdn.pixabay.com/photo/2016/11/08/05/18/hot-air-balloons-1807521_960_720.jpg",
    "https://cdn.pixabay.com/photo/2021/02/22/10/08/night-sky-6039591_1280.jpg",
    "https://cdn.pixabay.com/photo/2019/07/16/11/14/lake-4341560_1280.jpg",
    "https://cdn.pixabay.com/photo/2013/07/25/01/33/boat-166738_1280.jpg",
    "https://cdn.pixabay.com/photo/2019/11/18/18/29/mountain-4635428_1280.jpg",
    "https://cdn.pixabay.com/photo/2020/07/11/22/13/tree-5395485_1280.jpg",
    "https://cdn.pixabay.com/photo/2020/10/20/13/24/sunrise-5670440_1280.jpg",
    "https://cdn.pixabay.com/photo/2021/03/16/01/19/sunset-6098406_960_720.jpg",
)

val gifUrls = arrayOf(
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

private val fooUrls = gifUrls

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

    fun createFoo(num: Int, tag: String = javaClass.simpleName): Foo {
        val type = when {
            !multiTypeFoo -> FooType.TYPE1
            num % 2 != 0 -> FooType.TYPE1
            else -> FooType.TYPE2
        }
        val url = fooUrls[num % fooUrls.size]
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