package com.xiaocydx.sample.transition.enter

import android.os.Bundle
import android.view.View
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.PrepareDeadline
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.prepareScrap
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 尝试在加载列表数据期间，通过[prepareScrap]预创建[ViewHolder]以解决卡顿问题，但结果是失败的，
 * 原因是预创建[ViewHolder]，虽然能解决创建View的耗时，但解决不了[RecyclerView]布局本身的耗时，
 * 当在一帧内填充大量的View时，`onBindViewHolder()`、`measureChild()`、`layoutChild()`等等函数，
 * 其执行时长按View的填充个数累积起来，就是耗时较长的`doFrame`消息，导致Fragment过渡动画卡顿。
 *
 * @author xcc
 * @date 2023/5/21
 */
class PrepareFragment : TransitionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prepareScrap()
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

    private fun prepareScrap() {
        // viewType = 0是默认值
        viewLifecycleScope.launch {
            recyclerView?.prepareScrap(
                prepareAdapter = contentAdapter,
                prepareDeadline = PrepareDeadline.FRAME_NS,
                block = { add(viewType = 0, count = 50) }
            )
        }
    }
}