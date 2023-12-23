package com.xiaocydx.sample.paging.local

import androidx.lifecycle.ViewModel
import com.xiaocydx.accompanist.viewmodel.RetainedViewModels
import com.xiaocydx.accompanist.viewmodel.getOrPut
import com.xiaocydx.sample.common.FooListViewModel
import com.xiaocydx.sample.common.Menu
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2022/2/17
 */
class PagingSharedViewModel : ViewModel() {
    private val viewModels = RetainedViewModels<FooListViewModel>(host = this)
    private val _menuAction = MutableSharedFlow<MenuAction>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val menuAction = _menuAction.asSharedFlow()

    fun submitMenuAction(action: MenuAction) {
        _menuAction.tryEmit(action)
    }

    fun getListViewModel(key: String): FooListViewModel {
        return viewModels.getOrPut(key) { FooListViewModel() }
    }
}

enum class MenuAction(override val text: String): Menu {
    LINEAR("Linear"),
    GIRD("Grid"),
    STAGGERED("Staggered"),
    REFRESH("刷新"),
    REVERSE("反转布局"),
    INSERT_ITEM("插入Item"),
    REMOVE_ITEM("移除Item"),
    CLEAR_ALL("清除全部Item")
}