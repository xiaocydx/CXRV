package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.flowWithLifecycle
import com.xiaocydx.sample.doOnStateChanged
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.systembar.SystemBar
import com.xiaocydx.sample.transition.enter.EnterTransitionController
import com.xiaocydx.sample.transition.enter.LOADING_DURATION
import com.xiaocydx.sample.transition.enter.TransitionFragment
import com.xiaocydx.sample.transition.enter.TransitionState
import com.xiaocydx.sample.transition.transform.TransformReceiver
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
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
 * @date 2023/8/4
 */
@SystemBar(
    gestureNavBarEdgeToEdge = true,
    statusBarColor = 0xFF79AA91.toInt(),
    navigationBarColor = 0xFF79AA91.toInt()
)
class AdFragment : TransitionFragment(), TransformReceiver {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupDebugLog()
        setReceiverEnterTransition().duration = 200
        recyclerView?.enableGestureNavBarEdgeToEdge()

        // 沿用EnterTransitionController解决过渡动画卡顿的问题
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

    private fun setupDebugLog() {
        viewLifecycle.doOnStateChanged { source, event ->
            val currentState = source.lifecycle.currentState
            Log.d("AdFragment", "currentState = ${currentState}, event = $event")
        }
    }
}