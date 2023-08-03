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

/**
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamViewModel(videoFlow: Flow<PagingData<VideoStreamItem>>) : ViewModel() {
    private val state = ListState<VideoStreamItem>()
    private val _selectId = MutableStateFlow("")

    /**
     * 视频流页面的item铺满全屏，转换末尾加载的预期策略，提前指定item个数预取分页数据
     */
    val videoFlow = videoFlow
        .appendPrefetch(ItemCount(3))
        .storeIn(state, viewModelScope)
    val selectId = _selectId.asStateFlow()

    fun syncInitialState(initialState: VideoStreamInitialState) {
        state.submitList(initialState.videoList)
        selectVideo(initialState.position)
    }

    fun selectVideo(position: Int) {
        val item = state.getItemOrNull(position) ?: return
        _selectId.value = item.id
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