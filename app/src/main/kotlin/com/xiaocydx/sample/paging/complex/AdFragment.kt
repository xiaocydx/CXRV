package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.lifecycle.flowWithLifecycle
import com.xiaocydx.accompanist.insets.enableGestureNavBarEdgeToEdge
import com.xiaocydx.accompanist.lifecycle.doOnStateChanged
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycleScope
import com.xiaocydx.accompanist.transition.EnterTransitionController
import com.xiaocydx.accompanist.transition.transform.TransformReceiver
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.sample.transition.LOADING_DURATION
import com.xiaocydx.sample.transition.TransitionFragment
import com.xiaocydx.sample.transition.TransitionState
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
class AdFragment : TransitionFragment(), SystemBar, TransformReceiver {

    init {
        systemBarController {
            statusBarColor = 0xFF79AA91.toInt()
            navigationBarColor = 0xFF79AA91.toInt()
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
        }
    }

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