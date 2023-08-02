package com.xiaocydx.sample.paging.complex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.getItemOrNull
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.paging.PagingData
import com.xiaocydx.cxrv.paging.dataMap
import com.xiaocydx.cxrv.paging.flowMap
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamViewModel(pagingFlow: Flow<PagingData<ComplexItem>>) : ViewModel() {
    private val _selectId = MutableStateFlow("")
    private val state = ListState<ComplexItem>()
    val selectId = _selectId.asStateFlow()
    val videoFlow = pagingFlow.filterVideo().storeIn(state, viewModelScope)

    fun syncState(params: VideoStreamParams) {
        state.submitList(params.videoList)
        selectVideo(params.position)
    }

    fun selectVideo(position: Int) {
        val item = state.getItemOrNull(position) ?: return
        _selectId.value = item.id
    }

    private fun Flow<PagingData<ComplexItem>>.filterVideo() = flowMap { flow ->
        flow.dataMap { _, data -> data.filter { it.type == ComplexItem.TYPE_VIDEO } }
    }

    class Factory(private val pagingFlow: Flow<PagingData<ComplexItem>>) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === VideoStreamViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            return VideoStreamViewModel(pagingFlow) as T
        }
    }
}

data class VideoStreamParams(
    val position: Int,
    val videoList: List<ComplexItem>
)