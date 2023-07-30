package com.xiaocydx.sample.paging.complex

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.storeIn

/**
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListViewModel(repository: ComplexRepository = ComplexRepository()) : ViewModel() {
    private val helper = PagingSyncHelper<Int, ComplexItem>()
    private val state = ListState<ComplexItem>()
    private val pager = repository.getComplexPager(
        initKey = 1,
        config = PagingConfig(pageSize = 16),
        interceptor = helper
    )

    val rvId = ViewCompat.generateViewId()
    val flow = pager.flow.storeIn(state, viewModelScope)
    val currentList: List<ComplexItem>
        get() = state.currentList
    val nextKey: Int?
        get() = helper.nextKey

    fun syncRefresh(data: List<ComplexItem>, nextKey: Int?) {
        helper.refresh(pager, data, nextKey)
    }

    fun syncAppend(data: List<ComplexItem>, nextKey: Int?) {
        helper.append(pager, data, nextKey)
    }

    fun findPosition(id: String): Int {
        return state.currentList.indexOfFirst { it.id == id }
    }
}