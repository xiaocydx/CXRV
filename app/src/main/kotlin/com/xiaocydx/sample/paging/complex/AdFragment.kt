package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.flowWithLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycleScope
import com.xiaocydx.accompanist.transition.EnterTransitionController
import com.xiaocydx.accompanist.transition.transform.Transform
import com.xiaocydx.accompanist.transition.transform.setTransformTransition
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.systemBarController
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
class AdFragment : TransitionFragment(), SystemBar {

    init {
        systemBarController {
            statusBarColor = 0xFF79AA91.toInt()
            navigationBarColor = 0xFF79AA91.toInt()
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 为了让示例代码保持简洁，AdFragment重建后没有同步列表位置
        Transform.setTransformTransition(this)
        recyclerView?.insets()?.gestureNavBarEdgeToEdge()

        // 沿用EnterTransitionController解决过渡动画卡顿的问题
        val controller = EnterTransitionController(this)
        controller.postponeEnterTransition(timeMillis = LOADING_DURATION + 50L)
        viewModel.state
            .flowWithLifecycle(viewLifecycle)
            .distinctUntilChanged()
            .onEach { state ->
                when (state) {
                    TransitionState.Loading -> {
                        loadingAdapter.show()
                    }
                    TransitionState.Content -> {
                        controller.startPostponeEnterTransitionOrAwait()
                        loadingAdapter.hide()
                        contentAdapter.show()
                    }
                }
            }
            .launchIn(viewLifecycleScope)
    }

    companion object {
        fun show(activity: FragmentActivity, args: Bundle?) {
            val fm = activity.supportFragmentManager
            fm.commit {
                addToBackStack(null)
                add(android.R.id.content, AdFragment::class.java, args)
            }
        }
    }
}