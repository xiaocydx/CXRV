package com.xiaocydx.sample.paging.complex

import com.xiaocydx.cxrv.paging.LoadParams
import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.sample.foo.urls
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexSource(
    config: PagingConfig,
    initKey: Int = 1,
    private val adKeyRange: Boolean = false,
    private val maxKey: Int = Int.MAX_VALUE,
    private val duration: Duration = 400L.milliseconds
) : SharedSource<Int, ComplexItem>(initKey, config) {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComplexItem> {
        delay(duration)
        val data = (1..params.pageSize).map { num ->
            val id = "${params.key}-$num"
            val url = urls[num % urls.size]
            val type = when {
                adKeyRange && params.key in 2..4 -> ComplexItem.TYPE_AD
                num % 4 == 0 -> ComplexItem.TYPE_AD
                else -> ComplexItem.TYPE_VIDEO
            }
            ComplexItem(id, url, url, id, type)
        }
        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    override fun toVideoStreamList(data: List<ComplexItem>) = data.toViewStreamList()
}