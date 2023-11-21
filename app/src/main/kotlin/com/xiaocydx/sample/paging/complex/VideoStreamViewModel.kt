package com.xiaocydx.sample.paging.complex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.MutableStateList
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
class VideoStreamViewModel(
    initialState: VideoStreamInitialState?,
    videoFlow: Flow<PagingData<VideoStreamItem>>
) : ViewModel() {
    private val list = MutableStateList<VideoStreamItem>()
    private val _selectPosition = MutableStateFlow(0)
    val selectPosition = _selectPosition.asStateFlow()
    val selectTitle = selectPosition.map { list.getOrNull(it)?.title ?: "" }
    val selectId: String
        get() = list.getOrNull(selectPosition.value)?.id ?: ""

    init {
        // 先同步初始状态，后收集videoFlow，收集时发射的分页事件会完成状态的同步
        initialState?.videoList?.let(list::submit)
        initialState?.position?.let(::selectVideo)
    }

    /**
     * 视频流页面的item铺满全屏，转换末尾加载的预取策略，提前指定item个数预取分页数据
     */
    val videoFlow = videoFlow
        .appendPrefetch(ItemCount(3))
        .storeIn(list, viewModelScope)

    fun selectVideo(position: Int) {
        _selectPosition.value = position
    }

    class Factory(private val sharedViewModel: ComplexSharedViewModel) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === VideoStreamViewModel::class.java)
            val initialState = sharedViewModel.consumeReceiverState()
            val videoFlow = sharedViewModel.receiverFlow
            return VideoStreamViewModel(initialState, videoFlow) as T
        }
    }
}

data class VideoStreamInitialState(val position: Int, val videoList: List<VideoStreamItem>)