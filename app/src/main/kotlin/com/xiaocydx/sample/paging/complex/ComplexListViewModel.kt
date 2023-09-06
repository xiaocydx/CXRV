package com.xiaocydx.sample.paging.complex

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.paging.PagingData
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.paging.complex.transform.TransformSenderKey
import com.xiaocydx.sample.paging.complex.transform.asPosition
import kotlinx.coroutines.flow.Flow

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel(
    complexId: TransformSenderKey<String>,
    complexFlow: Flow<PagingData<ComplexItem>>
) : ViewModel() {
    private val state = ListState<ComplexItem>()
    val rvId = ViewCompat.generateViewId()
    val complexFlow = complexFlow.storeIn(state, viewModelScope)
    val complexPosition = complexId.asPosition(state, ComplexItem::id)
    val complexState: List<ComplexItem>
        get() = state.currentList

    class Factory(private val sharedViewModel: ComplexSharedViewModel) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass === ComplexListViewModel::class.java)
            val complexId = sharedViewModel.senderId
            val complexFlow = sharedViewModel.senderFlow
            return ComplexListViewModel(complexId, complexFlow) as T
        }
    }
}