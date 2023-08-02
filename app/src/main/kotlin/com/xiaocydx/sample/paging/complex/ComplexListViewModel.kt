package com.xiaocydx.sample.paging.complex

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingPrefetch
import com.xiaocydx.cxrv.paging.multiple
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel(repository: ComplexRepository = ComplexRepository()) : ViewModel() {
    private var pendingParams: VideoStreamParams? = null
    private val _scrollEvent = Channel<Int>(CONFLATED)
    private val state = ListState<ComplexItem>()
    private val pager = repository.getComplexPager(
        initKey = 1,
        config = PagingConfig(
            pageSize = 16,
            appendPrefetch = PagingPrefetch.ItemCount(3)
        )
    )

    val rvId = ViewCompat.generateViewId()
    val pagingFlow = pager.flow.multiple(viewModelScope)
    val complexFlow = pagingFlow.storeIn(state, viewModelScope)
    val scrollEvent = _scrollEvent.receiveAsFlow()

    fun syncSelect(id: String) {
        state.currentList.indexOfFirst { it.id == id }
            .takeIf { it != -1 }?.let(_scrollEvent::trySend)
    }

    fun setPendingParams(currentId: String): Boolean {
        if (pendingParams != null) return false
        val videoList = state.currentList.filter { it.type == ComplexItem.TYPE_VIDEO }
        val position = videoList.indexOfFirst { it.id === currentId }
        pendingParams = VideoStreamParams(position, videoList)
        return true
    }

    fun consumePendingParams() = pendingParams?.also { pendingParams = null }
}