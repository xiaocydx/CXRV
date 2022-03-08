package com.xiaocydx.sample.paging

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.recycler.extension.stateOn
import com.xiaocydx.recycler.list.addItem
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.paging.PagingData
import com.xiaocydx.recycler.paging.PagingEvent
import com.xiaocydx.recycler.paging.PagingListState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * @author xcc
 * @date 2022/2/17
 */
class PagingViewModel(
    private val repository: FooRepository
) : ViewModel() {
    private val listState = PagingListState<Foo>()
    val flow = repository.flow
        .transformItem { item ->
            item.copy(name = "${item.name} transform")
        }.stateOn(listState, viewModelScope)

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

    private inline fun <T : Any> Flow<PagingData<T>>.transformItem(
        crossinline transform: suspend (item: T) -> T
    ): Flow<PagingData<T>> = transformEvent { event ->
        if (event is PagingEvent.LoadDataSuccess) {
            event.copy(data = event.data.map { transform(it) })
        } else event
    }

    private inline fun <T : Any> Flow<PagingData<T>>.transformEvent(
        crossinline transform: suspend (event: PagingEvent<T>) -> PagingEvent<T>
    ): Flow<PagingData<T>> {
        return map { it.copy(flow = it.flow.map(transform)) }
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