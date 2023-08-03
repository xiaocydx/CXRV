package com.xiaocydx.sample.paging.complex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.getItemOrNull
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.paging.PagingData
import com.xiaocydx.cxrv.paging.PagingPrefetch.ItemCount
import com.xiaocydx.cxrv.paging.appendPrefetch
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamViewModel(videoFlow: Flow<PagingData<VideoStreamItem>>) : ViewModel() {
    private val state = ListState<VideoStreamItem>()
    private val _selectPosition = MutableStateFlow(0)

    /**
     * 视频流页面的item铺满全屏，转换末尾加载的预期策略，提前指定item个数预取分页数据
     */
    val videoFlow = videoFlow
        .appendPrefetch(ItemCount(3))
        .storeIn(state, viewModelScope)
    val selectPosition = _selectPosition.asStateFlow()
    val selectVideoId = selectPosition.map { state.getItemOrNull(it)?.id ?: "" }
    val selectVideoTitle = selectPosition.map { state.getItemOrNull(it)?.title ?: "" }

    /**
     * 先同步初始状态，后收集[videoFlow]，收集时发射的分页事件会完成状态的同步
     */
    fun syncInitialState(initialState: VideoStreamInitialState) {
        state.submitList(initialState.videoList)
        selectVideo(initialState.position)
    }

    fun selectVideo(position: Int) {
        _selectPosition.value = position
    }

    class Factory(private val videoFlow: Flow<PagingData<VideoStreamItem>>) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === VideoStreamViewModel::class.java)
            return VideoStreamViewModel(videoFlow) as T
        }
    }
}

data class VideoStreamInitialState(val position: Int, val videoList: List<VideoStreamItem>)