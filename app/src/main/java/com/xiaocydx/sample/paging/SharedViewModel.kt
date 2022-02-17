package com.xiaocydx.sample.paging

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.SingleLiveEvent

/**
 * @author xcc
 * @date 2022/2/17
 */
class SharedViewModel : ViewModel() {
    private val _menuAction = SingleLiveEvent<MenuAction>()
    val menuAction: LiveData<MenuAction> = _menuAction

    fun submitMenuAction(action: MenuAction) {
        _menuAction.value = action
    }
}