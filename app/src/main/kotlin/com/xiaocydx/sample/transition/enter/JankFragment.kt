package com.xiaocydx.sample.transition.enter

import android.os.Bundle
import android.view.View
import androidx.lifecycle.flowWithLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 模拟加载列表数据的场景，复现Fragment过渡动画卡顿问题
 *
 * @author xcc
 * @date 2023/5/21
 */
class JankFragment : TransitionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.state
            .flowWithLifecycle(viewLifecycle)
            .distinctUntilChanged()
            .onEach { state ->
                when (state) {
                    TransitionState.LOADING -> {
                        loadingAdapter.show()
                    }
                    TransitionState.CONTENT -> {
                        loadingAdapter.hide()
                        contentAdapter.show()
                    }
                }
            }
            .launchIn(viewLifecycleScope)
    }
}