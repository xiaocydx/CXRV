package com.xiaocydx.sample.paging.complex

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.PagingPrefetch.ItemCount
import com.xiaocydx.cxrv.paging.appendPrefetch
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamViewModel(handle: SavedStateHandle) : ViewModel() {
    private val shared = VideoStream.receiver(handle, viewModelScope)
    private val videoList = MutableStateList<VideoStreamItem>()
    private val _selectedPosition = MutableStateFlow(0)
    val selectedPosition = _selectedPosition.asStateFlow()
    val selectedTitle = selectedPosition.map { videoList.getOrNull(it)?.title ?: "" }

    init {
        // 先同步初始状态，后收集videoPagingFlow，发射的分页事件会完成状态的同步
        val initialState = shared.consumeReceiverState()
        initialState?.list?.let(videoList::submit)
        initialState?.position?.let(::selectVideo)
    }

    /**
     * item铺满全屏，转换末尾加载的预取策略，提前3个item预取分页数据
     */
    val videoPagingFlow = shared.receiverFlow
        .appendPrefetch(ItemCount(3))
        .storeIn(videoList, viewModelScope)

    fun selectVideo(position: Int) {
        _selectedPosition.value = position
    }

    fun syncSenderId() {
        val item = videoList.getOrNull(selectedPosition.value)
        item?.id?.let(shared::syncSenderId)
    }
}