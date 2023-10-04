package com.xiaocydx.sample.divider

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2023/10/4
 */
class DividerSharedViewModel : ViewModel() {
    private val _menuAction = MutableSharedFlow<MenuAction>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val menuAction = _menuAction.asSharedFlow()

    fun submitMenuAction(action: MenuAction) {
        _menuAction.tryEmit(action)
    }
}

enum class MenuAction(val text: String) {
    LINEAR("Linear"),
    GIRD("Grid"),
    STAGGERED("Staggered"),
    CONCAT("Concat"),
    REVERSE_LAYOUT("反转布局"),
    REVERSE_ORIENTATION("反转方向"),
    INCREASE_SPAN_COUNT("增加SpanCount"),
    DECREASE_SPAN_COUNT("减少SpanCount"),
    INSERT_ITEM("插入Item"),
    REMOVE_ITEM("移除Item")
}