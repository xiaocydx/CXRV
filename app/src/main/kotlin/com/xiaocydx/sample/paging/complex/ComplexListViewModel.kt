package com.xiaocydx.sample.paging.complex

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.broadcastIn
import com.xiaocydx.cxrv.paging.dataMap
import com.xiaocydx.cxrv.paging.flowMap
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel(repository: ComplexRepository = ComplexRepository()) : ViewModel() {
    private var pendingInitialState: VideoStreamInitialState? = null
    private val _syncSelectId = Channel<String>(CONFLATED)
    private val state = ListState<ComplexItem>()
    private val pager = repository.getComplexPager(PagingConfig(pageSize = 16))
    private val broadcastFlow = pager.flow.broadcastIn(viewModelScope)

    val rvId = ViewCompat.generateViewId()
    val complexFlow = broadcastFlow.storeIn(state, viewModelScope)
    val scrollEvent = _syncSelectId.receiveAsFlow()
        .map(::findPositionForId).filter { it != -1 }

    fun syncSelectId(id: String) {
        _syncSelectId.trySend(id)
    }

    fun setPendingInitialState(currentId: String) {
        val videoList = state.currentList.toViewStreamList()
        val position = videoList.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
        pendingInitialState = VideoStreamInitialState(position, videoList)
    }

    fun consumePendingInitialState() = pendingInitialState?.also { pendingInitialState = null }

    fun videoStreamFlow() = broadcastFlow.flowMap { flow ->
        flow.dataMap { _, data -> data.toViewStreamList() }
    }

    private fun findPositionForId(id: String) = state.currentList.indexOfFirst { it.id == id }
}