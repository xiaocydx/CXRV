package com.xiaocydx.sample.viewpager2

import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.paging.FooListViewModel
import com.xiaocydx.sample.viewpager2.shared.RetainedViewModels
import com.xiaocydx.sample.viewpager2.shared.getOrPut
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * @author xcc
 * @date 2022/7/12
 */
class FooCategoryViewModel : ViewModel() {
    private val viewModels =
            RetainedViewModels<FooListViewModel>(host = this)
    private val _state = MutableStateFlow(
        FooCategoryState(list = (1L..10L).map { FooCategory(id = it) })
    )

    val state = _state.asStateFlow()

    fun getListViewModel(categoryId: Long): FooListViewModel {
        return viewModels.getOrPut(categoryId.toString()) { FooListViewModel() }
    }

    fun addItemToLast() = updateState {
        val item = list.lastOrNull()
        copy(list = list + FooCategory(id = (item?.id ?: 0) + 1))
    }

    fun removeCurrentItem() = updateState {
        val item = list.getOrNull(currentItem) ?: return
        viewModels.remove(item.id.toString())
        var newItem = currentItem
        if (newItem == list.lastIndex) {
            newItem = (newItem - 1).coerceAtLeast(0)
        }
        val list = list
            .toMutableList().apply { removeAt(currentItem) }
        copy(list = list, currentItem = newItem, pendingItem = newItem)
    }

    fun moveCurrentItemToFirst() = updateState {
        if (currentItem == 0) return
        val list = list.toMutableList()
        Collections.swap(list, 0, currentItem)
        copy(list = list, currentItem = 0, pendingItem = 0)
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

const val NO_ITEM = -1

data class FooCategoryState(
    val list: List<FooCategory>,
    val currentItem: Int = 0,
    val pendingItem: Int = NO_ITEM
)

data class FooCategory(
    val id: Long,
    val title: String = "LIST-${id}"
)