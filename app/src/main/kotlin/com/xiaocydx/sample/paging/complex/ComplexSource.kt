package com.xiaocydx.sample.paging.complex

import androidx.annotation.ColorInt
import com.xiaocydx.accompanist.videostream.VideoStream
import com.xiaocydx.accompanist.videostream.VideoStreamItem
import com.xiaocydx.cxrv.paging.LoadParams
import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.sample.common.urls
import kotlinx.coroutines.delay

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexSource(
    private val adKeyRange: Boolean = false,
    private val maxKey: Int = Int.MAX_VALUE,
    private val timeMillis: Long = 400L,
    override val initKey: Int = 1,
    override val config: PagingConfig = PagingConfig(pageSize = 16)
) : VideoStream.Source<Int, ComplexItem> {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComplexItem> {
        val data = loadData(params)
        val nextKey = if (params.key >= maxKey) null else params.key + 1
        return LoadResult.Success(data, nextKey)
    }

    override fun transform(data: List<ComplexItem>) = data.mapNotNull {
        if (it.type != ComplexItem.TYPE_VIDEO) return@mapNotNull null
        VideoStreamItem(it.id, it.linkUrl, it.coverUrl, it.name)
    }

    private suspend fun loadData(params: LoadParams<Int>): List<ComplexItem> {
        delay(timeMillis)
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
        return data
    }
}

data class ComplexItem(
    val id: String,
    val linkUrl: String,
    val coverUrl: String,
    val name: String,
    val type: String,
    @ColorInt val typeColor: Int = typeColor(type)
) {
    companion object {
        const val TYPE_VIDEO = "视频"
        const val TYPE_AD = "广告"

        private fun typeColor(type: String) = when (type) {
            TYPE_VIDEO -> 0xFFAA5458.toInt()
            TYPE_AD -> 0xFF79AA91.toInt()
            else -> throw IllegalArgumentException()
        }
    }
}