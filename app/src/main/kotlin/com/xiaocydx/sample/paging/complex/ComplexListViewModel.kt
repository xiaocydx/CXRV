package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.paging.complex.VideoStream.KEY_SHARED_TOKEN
import com.xiaocydx.sample.transition.transform.asPosition

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel : ViewModel() {
    private val list = MutableStateList<ComplexItem>()
    private val shared = VideoStream.makeShared(
        scope = viewModelScope,
        source = ComplexSource(PagingConfig(pageSize = 16))
    )

    val rvId = ViewCompat.generateViewId()
    val complexFlow = shared.senderFlow.storeIn(list, viewModelScope)
    val complexPosition = shared.senderId.asPosition({ list }, ComplexItem::id)

    fun setReceiverState(item: ComplexItem): Bundle? {
        val isVideo = item.type == ComplexItem.TYPE_VIDEO
        shared.setReceiverState(item.id, list.takeIf { isVideo })
        if (!isVideo) return null
        return Bundle(1).apply { putString(KEY_SHARED_TOKEN, shared.token) }
    }
}