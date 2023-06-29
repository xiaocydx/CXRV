package com.xiaocydx.sample.viewpager2.shared

import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.foo.FooListViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @author xcc
 * @date 2022/7/12
 */
class FooCategoryViewModel : ViewModel() {
    private val viewModels = RetainedViewModels<FooListViewModel>(host = this)
    private val _state = MutableStateFlow(FooCategoryState(
        list = (1L..10L).map { FooCategory(id = it) }
    ))

    val state = _state.asStateFlow()

    fun getFooViewModel(categoryId: Long): FooListViewModel {
        return viewModels.getOrPut(categoryId.toString()) { FooListViewModel() }
    }

    fun addItemToLast() = updateState {
        val id = if (list.isEmpty()) 1 else list.maxOf { it.id } + 1
        copy(list = list + FooCategory(id = id))
    }

    fun removeCurrentItem() = updateState {
        val item = list.getOrNull(currentItem) ?: return
        viewModels.remove(item.id.toString())
        var newItem = currentItem
        if (newItem == list.lastIndex) {
            newItem = (newItem - 1).coerceAtLeast(0)
        }
        val list = list.toMutableList().apply { removeAt(currentItem) }
        copy(list = list, currentItem = newItem, pendingItem = newItem)
    }

    fun moveCurrentItemToFirst() = updateState {
        val newItem = 0
        if (currentItem == newItem) return
        val list = list.toMutableList()
        list.add(newItem, list.removeAt(currentItem))
        copy(list = list, currentItem = newItem, pendingItem = newItem)
    }

    fun setCurrentItem(currentItem: Int) = updateState {
        if (this.currentItem == currentItem) return
        copy(currentItem = currentItem)
    }

    fun consumePendingItem() = updateState {
        if (pendingItem == NO_ITEM) return
        copy(pendingItem = NO_ITEM)
    }

    private inline fun updateState(newState: FooCategoryState.() -> FooCategoryState) {
        _state.value = newState(_state.value)
    }
}

private const val NO_ITEM = -1

data class FooCategoryState(
    val list: List<FooCategory>,
    val currentItem: Int = 0,
    val pendingItem: Int = NO_ITEM
)

data class FooCategory(
    val id: Long,
    val title: String = "LIST-${id}"
)

val FooCategoryState.hasPendingItem: Boolean
    get() = pendingItem != NO_ITEM