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
 * [EnterTransitionController]推迟Fragment过渡动画，推迟的超时时间未到达，列表数据加载完成，
 * 申请重新布局，并开始Fragment过渡动画，此时的交互体验接近Activity的窗口动画，即看到Fragment页面时，
 * 就有列表内容，而不是先显示Loading，再看到列表内容，等于是在动画开始前处理耗时较长的`doFrame`消息。
 *
 * 对列表数据加载比较快的场景而言，这种处理方式虽然没直接解决布局耗时的问题，但能提升交互体验。
 *
 * @author xcc
 * @date 2023/5/21
 */
class NotTimeoutFragment : SlideFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val controller = EnterTransitionController(this)
        controller.postponeEnterTransition(timeoutMillis = LOADING_DURATION + 50L)
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