package com.xiaocydx.sample.divider

import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.common.Menu
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

enum class MenuAction(override val text: String) : Menu {
    Linear("Linear"),
    Gird("Grid"),
    Staggered("Staggered"),
    Concat("Concat"),
    ReverseLayout("反转布局"),
    ReverseOrientation("反转方向"),
    IncreaseSpanCount("增加SpanCount"),
    DecreaseSpanCount("减少SpanCount"),
    InsertItem("插入Item"),
    RemoveItem("移除Item")
}