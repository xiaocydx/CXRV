package com.xiaocydx.sample.transition

import android.os.Bundle
import android.view.View
import androidx.lifecycle.flowWithLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycleScope
import com.xiaocydx.accompanist.transition.EnterTransitionController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [EnterTransitionController]推迟Fragment过渡动画，推迟时间未到达，列表数据加载完成，申请重新布局，
 * 并开始Fragment过渡动画，此时的交互体验接近Activity的窗口动画，即看到Fragment页面就有列表内容，
 * 而不是先显示Loading，再看到列表内容。
 *
 * 对于列表数据加载比较快的情况，这种处理方式虽然没直接解决重新布局的耗时问题，但是能提升交互体验。
 *
 * @author xcc
 * @date 2023/5/21
 */
class NotWaitEndFragment : TransitionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val controller = EnterTransitionController(this)
        controller.postponeEnterTransition(timeMillis = LOADING_DURATION + 50L)
        viewModel.state
            .flowWithLifecycle(viewLifecycle)
            .distinctUntilChanged()
            .onEach { state ->
                when (state) {
                    TransitionState.LOADING -> {
                        loadingAdapter.show()
                    }
                    TransitionState.CONTENT -> {
                        controller.startPostponeEnterTransitionOrAwait()
                        loadingAdapter.hide()
                        contentAdapter.show()
                    }
                }
            }
            .launchIn(viewLifecycleScope)
    }
}