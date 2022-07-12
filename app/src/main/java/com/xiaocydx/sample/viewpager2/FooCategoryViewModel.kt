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
    private val _categoryState = MutableStateFlow(
        FooCategoryState(items = (1L..10L).map { FooCategory(id = it) })
    )

    val categoryState = _categoryState.asStateFlow()

    fun getListViewModel(categoryId: Long): FooListViewModel {
        return viewModels.getOrPut(categoryId.toString()) { FooListViewModel() }
    }

    fun addItemToLast() = updateState {
        val item = items.lastOrNull()
        copy(items = items + FooCategory(id = (item?.id ?: 0) + 1))
    }

    fun removeCurrentItem() {
        updateState {
            val item = items.getOrNull(currentItem) ?: return
            viewModels.remove(item.id.toString())
            var newItem = currentItem
            if (newItem == items.lastIndex) {
                newItem = (newItem - 1).coerceAtLeast(0)
            }
            val items = items
                .toMutableList().apply { removeAt(currentItem) }
            copy(items = items, currentItem = newItem, pendingItem = newItem)
        }
        clearPendingItem()
    }

    fun moveCurrentItemToFirst() {
        updateState {
            if (currentItem == 0) return
            val items = items.toMutableList()
            Collections.swap(items, 0, currentItem)
            copy(items = items, currentItem = 0, pendingItem = 0)
        }
        clearPendingItem()
    }

    fun setCurrentItem(currentItem: Int) {
        if (categoryState.value.currentItem == currentItem) return
        updateState { copy(currentItem = currentItem) }
    }

    private fun clearPendingItem() {
        if (categoryState.value.pendingItem == NO_ITEM) return
        updateState { copy(pendingItem = NO_ITEM) }
    }

    private inline fun updateState(newState: FooCategoryState.() -> FooCategoryState) {
        _categoryState.value = newState(_categoryState.value)
    }
}

const val NO_ITEM = -1

data class FooCategoryState(
    val items: List<FooCategory>,
    val currentItem: Int = 0,
    val pendingItem: Int = NO_ITEM
)

data class FooCategory(
    val id: Long,
    val title: String = "LIST-${id}"
)