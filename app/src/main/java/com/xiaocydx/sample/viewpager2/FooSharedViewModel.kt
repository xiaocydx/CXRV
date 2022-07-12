package com.xiaocydx.sample.viewpager2

import androidx.lifecycle.ViewModel
import com.xiaocydx.sample.paging.FooListViewModel
import com.xiaocydx.sample.viewpager2.shared.RetainedViewModels
import com.xiaocydx.sample.viewpager2.shared.getOrPut

/**
 * @author xcc
 * @date 2022/7/12
 */
class FooSharedViewModel : ViewModel() {
    private val viewModels =
            RetainedViewModels<FooListViewModel>(host = this)

    fun getListViewModel(key: String): FooListViewModel {
        return viewModels.getOrPut(key) { FooListViewModel() }
    }
}