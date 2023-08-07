package com.xiaocydx.sample.transition

import android.os.Bundle
import android.view.View
import androidx.lifecycle.flowWithLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [EnterTransitionController]推迟Fragment过渡动画，推迟时间达到，开始过渡动画，
 * 动画运行期间，列表数据加载完成，等待动画结束再申请重新布局，避免造成动画卡顿。
 *
 * @author xcc
 * @date 2023/5/21
 */
class WaitEndFragment : TransitionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val controller = EnterTransitionController(this)
        controller.postponeEnterTransition(timeMillis = LOADING_DURATION - 50L)
        viewModel.state
            .flowWithLifecycle(viewLifecycle)
            .distinctUntilChanged()
            .onEach { state ->
                when (state) {
                    SlideState.LOADING -> {
                        loadingAdapter.showLoading()
                    }
                    SlideState.CONTENT -> {
                        controller.startPostponeEnterTransitionOrAwait()
                        loadingAdapter.hideLoading()
                        contentAdapter.insertItems()
                    }
                }
            }
            .launchIn(viewLifecycleScope)
    }
}