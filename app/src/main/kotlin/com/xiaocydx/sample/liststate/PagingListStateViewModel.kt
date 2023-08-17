package com.xiaocydx.sample.liststate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.addItem
import com.xiaocydx.cxrv.list.clear
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.list.submitTransform
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.foo.Foo
import com.xiaocydx.sample.foo.FooRepository
import com.xiaocydx.sample.foo.FooSource
import com.xiaocydx.sample.foo.ResultType

/**
 * @author xcc
 * @date 2023/8/17
 */
class PagingListStateViewModel(
    private val repository: FooRepository = FooRepository(
        FooSource(maxKey = 5, resultType = ResultType.Normal)
    )
) : ViewModel() {
    private val state = ListState<Foo>()
    private val pager = repository.getFooPager(initKey = 1, PagingConfig(pageSize = 10))
    val flow = pager.flow.storeIn(state, viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun insertItem() {
        val item = repository.createFoo(
            tag = javaClass.simpleName,
            num = state.currentList.size
        )
        state.addItem(0, item)
    }

    fun deleteItem(position: Int = 0) {
        state.removeItemAt(position)
    }

    fun clearOddItem() {
        state.submitTransform { filter { it.num % 2 == 0 } }
    }

    fun clearEvenItem() {
        state.submitTransform { filter { it.num % 2 != 0 } }
    }

    fun clearAllItem() {
        state.clear()
    }
}