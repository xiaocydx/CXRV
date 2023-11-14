package com.xiaocydx.sample.liststate

import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.extensions.Menu
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2023/8/17
 */
class ListStateSharedViewModel : ViewModel() {
    private val _menuAction = MutableSharedFlow<MenuAction>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val menuAction = _menuAction.asSharedFlow()

    fun submitMenuAction(action: MenuAction) {
        _menuAction.tryEmit(action)
    }
}

enum class MenuAction(override val text: String): Menu {
    NORMAL("普通列表"),
    PAGING("分页列表"),
    REFRESH("刷新"),
    INSERT_ITEM("插入Item"),
    REMOVE_ITEM("移除Item"),
    CLEAR_ODD("清除奇数Item"),
    CLEAR_EVEN("清除偶数Item"),
    CLEAR_ALL("清除全部Item")
}