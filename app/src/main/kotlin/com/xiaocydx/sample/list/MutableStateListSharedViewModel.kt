package com.xiaocydx.sample.list

import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.common.Menu
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2023/8/17
 */
class MutableStateListSharedViewModel : ViewModel() {
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
    Normal("普通列表"),
    Paging("分页列表"),
    Refresh("刷新"),
    InsertItem("插入Item"),
    RemoveItem("移除Item"),
    ClearOdd("清除奇数Item"),
    ClearEven("清除偶数Item"),
    ClearAll("清除全部Item")
}