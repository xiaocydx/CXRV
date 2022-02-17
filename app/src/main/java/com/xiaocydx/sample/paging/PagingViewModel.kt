package com.xiaocydx.sample.paging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.recycler.list.addItem
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.paging.Pager
import com.xiaocydx.recycler.paging.PagingConfig
import com.xiaocydx.recycler.paging.cacheIn

/**
 * @author xcc
 * @date 2022/2/17
 */
class PagingViewModel(
    private val repository: FooRepository
) : ViewModel() {
    private val pager = Pager(
        initKey = 1,
        config = PagingConfig(pageSize = 10)
    ) { params ->
        repository.loadResult(params)
    }
    val flow = pager.flow.cacheIn(viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun insertItem() {
        var lastNum = pager.currentList.lastOrNull()?.num ?: 0
        val item = createFoo(num = ++lastNum, tag = "Pager")
        pager.addItem(0, item)
    }

    fun deleteItem() {
        pager.removeItemAt(0)
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
                    maxKey = 5,
                    resultType = ResultType.Normal
                )
                return PagingViewModel(repository) as T
            }
            throw IllegalArgumentException()
        }
    }
}