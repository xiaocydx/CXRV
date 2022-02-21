package com.xiaocydx.sample.paging

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * @author xcc
 * @date 2022/2/17
 */
class SharedViewModel : ViewModel() {
    private val _menuAction: MutableSharedFlow<MenuAction> = MutableSharedFlow(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val menuAction: Flow<MenuAction> = _menuAction.asSharedFlow()

    fun submitMenuAction(action: MenuAction) {
        _menuAction.tryEmit(action)
    }
}