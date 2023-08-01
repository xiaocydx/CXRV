package com.xiaocydx.sample.paging.complex

import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.getItemOrNull
import com.xiaocydx.cxrv.paging.LoadParams
import com.xiaocydx.cxrv.paging.LoadResult
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingPrefetch
import com.xiaocydx.cxrv.paging.PagingSource
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.CoroutineScope

/**
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamStateHolder(repository: ComplexRepository = ComplexRepository()) {
    private var sendEvent: ((VideoStreamEvent) -> Unit)? = null
    private var position = NO_POSITION
    private val state = ListState<ComplexItem>()
    private val helper = PagingSyncHelper<Int, ComplexItem>()
    private val pager = repository.getComplexPager(
        initKey = 1,
        config = PagingConfig(
            pageSize = 16,
            appendPrefetch = PagingPrefetch.ItemCount(5)
        ),
        interceptor = VideoStreamInterceptor()
    )

    fun flow(scope: CoroutineScope) = pager.flow.storeIn(state, scope)

    fun initState(params: VideoStreamParams) {
        position = params.position
        helper.refresh(pager, params.data, params.nextKey)
    }

    fun sendEvent(block: (VideoStreamEvent) -> Unit) {
        sendEvent = block
    }

    fun selectVideo(position: Int) {
        if (this.position == position) return
        val item = state.getItemOrNull(position) ?: return
        this.position = position
        sendEvent?.invoke(VideoStreamEvent.Select(item.id))
    }

    private inner class VideoStreamInterceptor : PagingInterceptor<Int, ComplexItem> {
        override fun intercept(source: PagingSource<Int, ComplexItem>): PagingSource<Int, ComplexItem> {
            return VideoStreamTransformer(helper.intercept(source))
        }
    }

    private inner class VideoStreamTransformer(
        private val source: PagingSource<Int, ComplexItem>
    ) : PagingSource<Int, ComplexItem> {
        private var isFirstRefresh = true

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ComplexItem> {
            val result = source.load(params)
            result.takeIf { !isFirstRefresh }
                ?.toEvent(params)?.let { sendEvent?.invoke(it) }
            if (params is LoadParams.Refresh) isFirstRefresh = false
            return result.filterData()
        }

        private fun LoadResult<Int, ComplexItem>.toEvent(
            params: LoadParams<Int>
        ): VideoStreamEvent? = when {
            this !is LoadResult.Success -> null
            params is LoadParams.Refresh -> VideoStreamEvent.Refresh(data, nextKey)
            params is LoadParams.Append -> VideoStreamEvent.Append(data, nextKey)
            else -> null
        }

        private fun LoadResult<Int, ComplexItem>.filterData() = when (this) {
            !is LoadResult.Success -> this
            else -> copy(data = data.filter { it.type == ComplexItem.TYPE_VIDEO })
        }
    }
}