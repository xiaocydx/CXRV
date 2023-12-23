package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.accompanist.transition.transform.asPosition
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.storeIn

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel(handle: SavedStateHandle) : ViewModel() {
    private val shared = VideoStream.sender(ComplexSource::class, handle, viewModelScope)
    private val complexList = MutableStateList<ComplexItem>()
    val rvId = ViewCompat.generateViewId()
    val complexPagingFlow = shared.senderFlow.storeIn(complexList, viewModelScope)
    val complexPosition = shared.senderId.asPosition(ComplexItem::id, ::complexList)

    fun setReceiverState(item: ComplexItem): Bundle? {
        val isVideo = item.type == ComplexItem.TYPE_VIDEO
        shared.setReceiverState(item.id, complexList.takeIf { isVideo })
        return shared.takeIf { isVideo }?.toBundle()
    }
}