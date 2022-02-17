package com.xiaocydx.sample.paging

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.SingleLiveEvent

/**
 * @author xcc
 * @date 2022/2/17
 */
class SharedViewModel : ViewModel() {
    private val _menuAction = SingleLiveEvent<MenuAction>()
    private val _title = MutableLiveData<String>()
    val menuAction: LiveData<MenuAction> = _menuAction
    val title: LiveData<String> = _title

    fun submitMenuAction(action: MenuAction) {
        _menuAction.value = action
    }

    fun setTitle(title: String) {
        _title.value = title
    }
}