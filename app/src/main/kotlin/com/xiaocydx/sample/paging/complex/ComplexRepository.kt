package com.xiaocydx.sample.paging.complex

import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.sample.foo.urls
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexRepository(
    private val maxKey: Int = Int.MAX_VALUE,
    private val duration: Duration = 400L.milliseconds
) {

    fun getComplexPager(
        initKey: Int,
        config: PagingConfig,
        interceptor: PagingInterceptor<Int, ComplexItem>
    ): Pager<Int, ComplexItem> = Pager(
        initKey = initKey,
        config = config,
        source = interceptor.intercept { params ->
            delay(duration)
            val data = (1..params.pageSize).map { num ->
                val id = "${params.key}-$num"
                val url = urls[num % urls.size]
                val type = when {
                    num % 4 == 0 -> ComplexItem.TYPE_IMAGE
                    num % 7 == 0 -> ComplexItem.TYPE_AD
                    else -> ComplexItem.TYPE_VIDEO
                }
                ComplexItem(id, url, url, id, type)
            }
            val nextKey = if (params.key >= maxKey) null else params.key + 1
            LoadResult.Success(data, nextKey)
        }
    )
}

