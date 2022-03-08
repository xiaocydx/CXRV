package com.xiaocydx.sample.paging

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.recycler.list.ListState
import com.xiaocydx.recycler.list.addItem
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.paging.storeIn
import com.xiaocydx.recycler.paging.transformItem

/**
 * @author xcc
 * @date 2022/2/17
 */
class PagingViewModel(
    private val repository: FooRepository
) : ViewModel() {
    private val listState = ListState<Foo>()
    val flow = repository.flow
        .transformItem { item ->
            item.copy(name = "${item.name} transform")
        }.storeIn(listState, viewModelScope)

    val rvId = ViewCompat.generateViewId()

    fun refresh() {
        repository.refresh()
    }

    fun insertItem() {
        var lastNum = listState.currentList.lastOrNull()?.num ?: 0
        val item = createFoo(num = ++lastNum, tag = "Pager")
        listState.addItem(0, item)
    }

    fun deleteItem() {
        listState.removeItemAt(0)
    }

    fun createFoo(num: Int, tag: String): Foo {
        return repository.createFoo(num, tag)
    }

    fun enableMultiTypeFoo() {
        repository.multiTypeFoo = true
    }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass === PagingViewModel::class.java) {
                val repository = FooRepository(
                    pageSize = 10, initKey = 1, maxKey = 5,
                    resultType = ResultType.Normal
                )
                return PagingViewModel(repository) as T
            }
            throw IllegalArgumentException()
        }
    }
}