package com.xiaocydx.sample.transition

import android.os.Bundle
import android.view.View
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.PrepareDeadline
import androidx.recyclerview.widget.prepareScrap
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * @author xcc
 * @date 2023/5/21
 */
class PrepareScrapSlideFragment : SlideFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleScope.launch {
            // FIXME: 调用度问题
            val result = recyclerView.prepareScrap(
                prepareAdapter = contentAdapter,
                prepareDeadline = PrepareDeadline.FOREVER_NS,
                block = { add(viewType = 0, count = 100) }
            )
            // val count = result.getPreparedScrapCount(0)
            // println(count)
        }

        recyclerView.itemAnimator?.addDuration = 2000
        recyclerView.itemAnimator = null

        viewModel.state
            .flowWithLifecycle(viewLifecycle)
            .distinctUntilChanged()
            .onEach { state ->
                when (state) {
                    SlideState.LOADING -> {
                        loadingAdapter.showLoading()
                    }
                    SlideState.CONTENT -> {
                        loadingAdapter.hideLoading()
                        contentAdapter.insertItems()
                    }
                }
            }
            .launchIn(viewLifecycleScope)
    }
}