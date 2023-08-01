package com.xiaocydx.sample.paging.complex

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel(repository: ComplexRepository = ComplexRepository()) : ViewModel() {
    private var pendingId = ""
    private val helper = PagingSyncHelper<Int, ComplexItem>()
    private val state = ListState<ComplexItem>()
    private val pager = repository.getComplexPager(
        initKey = 1,
        config = PagingConfig(pageSize = 16),
        interceptor = helper
    )
    private val _scrollEvent = MutableSharedFlow<Int>(
        extraBufferCapacity = 1,
        onBufferOverflow = DROP_OLDEST
    )

    val rvId = ViewCompat.generateViewId()
    val flow = pager.flow.storeIn(state, viewModelScope)
    val scrollEvent = _scrollEvent.asSharedFlow()

    fun sync(event: VideoStreamEvent) {
        when (event) {
            is VideoStreamEvent.Refresh -> helper.refresh(pager, event.data, event.nextKey)
            is VideoStreamEvent.Append -> helper.append(pager, event.data, event.nextKey)
            is VideoStreamEvent.Select -> state.currentList.indexOfFirst { it.id == event.id }
                .takeIf { it != -1 }?.let(_scrollEvent::tryEmit)
        }
    }

    fun setPendingId(id: String) {
        pendingId = id
    }

    fun consumeParams(): VideoStreamParams {
        val data = state.currentList.filter { it.type == ComplexItem.TYPE_VIDEO }
        val position = data.indexOfFirst { it.id === pendingId }
        pendingId = ""
        return VideoStreamParams(data, helper.nextKey, position)
    }
}