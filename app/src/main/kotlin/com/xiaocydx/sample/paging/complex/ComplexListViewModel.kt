package com.xiaocydx.sample.paging.complex

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.getItemOrNull
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.broadcastIn
import com.xiaocydx.cxrv.paging.dataMap
import com.xiaocydx.cxrv.paging.flowMap
import com.xiaocydx.cxrv.paging.storeIn
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel(repository: ComplexRepository = ComplexRepository()) : ViewModel() {
    private val state = ListState<ComplexItem>()
    private val pager = repository.getComplexPager(PagingConfig(pageSize = 16))
    private val broadcastFlow = pager.flow.broadcastIn(viewModelScope)
    private val syncEvent = Channel<String>(CONFLATED)
    private var syncId = ""
    private var pendingState: VideoStreamInitialState? = null

    val rvId = ViewCompat.generateViewId()
    val complexFlow = broadcastFlow.storeIn(state, viewModelScope)
    val scrollEvent = syncEvent.receiveAsFlow()
        .onStart { if (syncId.isNotEmpty()) emit(syncId) }
        .map(::findPositionForId).filter { it != -1 }

    fun syncSelect(id: String) {
        if (syncId == id) return
        syncId = id
        syncEvent.trySend(syncId)
    }

    fun setPendingState(id: String) {
        val item = findPositionForId(id).let(state::getItemOrNull)
        syncId = item?.id ?: ""
        pendingState = null
        if (item?.type == ComplexItem.TYPE_VIDEO) {
            val videoList = state.currentList.toViewStreamList()
            val position = videoList.indexOfFirst { it.id == syncId }.coerceAtLeast(0)
            pendingState = VideoStreamInitialState(position, videoList)
        }
    }

    fun consumePendingState() = pendingState?.also { pendingState = null }

    fun consumeSyncPosition() = syncId.also { syncId = "" }.let(::findPositionForId)

    fun videoStreamFlow() = broadcastFlow.flowMap { flow ->
        flow.dataMap { _, data -> data.toViewStreamList() }
    }

    private fun findPositionForId(id: String) = state.currentList.indexOfFirst { it.id == id }
}