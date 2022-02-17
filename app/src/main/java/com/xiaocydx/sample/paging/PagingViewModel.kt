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
    private val dataSource: FooDataSource
) : ViewModel() {
    private val pager = Pager(
        initKey = 1,
        config = PagingConfig(pageSize = 10)
    ) { params ->
        dataSource.loadResult(params)
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
        return dataSource.createFoo(num, tag)
    }

    fun enableMultiTypeFoo() {
        dataSource.multiTypeFoo = true
    }

    companion object Factory : ViewModelProvider.Factory {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass === PagingViewModel::class.java) {
                val dataSource = FooDataSource(
                    maxKey = 5,
                    resultType = ResultType.Normal
                )
                return PagingViewModel(dataSource) as T
            }
            throw IllegalArgumentException()
        }
    }
}