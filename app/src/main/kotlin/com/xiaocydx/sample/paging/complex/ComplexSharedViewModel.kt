package com.xiaocydx.sample.paging.complex

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.broadcastIn
import com.xiaocydx.cxrv.paging.dataMap
import com.xiaocydx.cxrv.paging.flowMap
import com.xiaocydx.sample.transition.transform.SenderKey
import com.xiaocydx.sample.transition.transform.TransformReceiver
import com.xiaocydx.sample.transition.transform.TransformSender

/**
 * [TransformSender]和[TransformReceiver]共享的ViewModel，
 * 该类的属性和函数用`Sender`和`Receiver`前缀演示数据的走向。
 *
 * @author xcc
 * @date 2023/9/6
 */
class ComplexSharedViewModel(repository: ComplexRepository = ComplexRepository()) : ViewModel() {
    private val pager = repository.getComplexPager(PagingConfig(pageSize = 16))
    private var receiverState: VideoStreamInitialState? = null
    private val broadcastFlow = pager.flow.broadcastIn(viewModelScope)
    private val _senderId = SenderKey<String>()

    val senderId = _senderId.asTransform()
    val senderFlow = broadcastFlow
    val receiverFlow = broadcastFlow.flowMap { flow ->
        flow.dataMap { _, data -> data.toViewStreamList() }
    }

    fun syncSenderId(id: String) {
        _senderId.sync(id)
    }

    fun setReceiverState(id: String, list: List<ComplexItem>) {
        _senderId.record(id)
        receiverState = null
        val item = list.firstOrNull { it.id == id }
        if (item?.type == ComplexItem.TYPE_VIDEO) {
            val videoList = list.toViewStreamList()
            val position = videoList.indexOfFirst { it.id == id }.coerceAtLeast(0)
            receiverState = VideoStreamInitialState(position, videoList)
        }
    }

    fun consumeReceiverState() = receiverState?.also { receiverState = null }
}