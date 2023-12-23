package com.xiaocydx.sample.liststate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.common.Foo
import com.xiaocydx.sample.common.FooRepository
import com.xiaocydx.sample.common.FooSource
import com.xiaocydx.sample.common.ResultType

/**
 * @author xcc
 * @date 2023/8/17
 */
class PagingListStateViewModel(
    private val repository: FooRepository = FooRepository(
        FooSource(maxKey = 5, resultType = ResultType.Normal)
    )
) : ViewModel() {
    /**
     * [ListState]降级为内部实现，[MutableStateList]替代[ListState]
     */
    private val list = MutableStateList<Foo>()
    private val pager = repository.getFooPager(initKey = 1, PagingConfig(pageSize = 10))
    val flow = pager.flow.storeIn(list, viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun insertItem() {
        list.add(0, repository.createFoo(list.size, javaClass.simpleName))
    }

    fun removeItem() {
        list.removeFirstOrNull()
    }

    fun clearOdd() {
        list.filter { it.num % 2 == 0 }.let(list::submit)
    }

    fun clearEven() {
        list.filter { it.num % 2 != 0 }.let(list::submit)
    }

    fun clearAll() {
        list.clear()
    }
}