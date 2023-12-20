package com.xiaocydx.sample.viewpager2.loop

import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.viewpager2.loop.LinearSmoothScrollerProvider
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.cxrv.viewpager2.loop.scrollState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Banner轮播交互的协程示例代码
 *
 * 以下是轮播开始的必要条件：
 * 1. [Lifecycle.State]至少为[state]。
 * 2. `Adapter.itemCount`支持循环。
 * 3. 没有触摸滚动，上一次平滑滚动结束。
 *
 * @param intervalMs 轮播间隔时长
 * @param durationMs 平滑滚动时长
 */
fun LoopPagerController.launchBanner(
    adapter: Adapter<*>,
    lifecycle: Lifecycle,
    state: Lifecycle.State = STARTED,
    intervalMs: Long = 1500,
    durationMs: Long = -1
): Job = lifecycle.coroutineScope.launch {
    val stateFlow = stateIn(scope = this, adapter)
    val provider = durationMs.takeIf { it > 0 }?.let {
        LinearSmoothScrollerProvider(it, AccelerateDecelerateInterpolator())
    }
    lifecycle.repeatOnLifecycle(state) {
        var scrollJob: Job? = null
        stateFlow.collect {
            if (it.canLoopScroll && scrollJob == null) {
                scrollJob = this@repeatOnLifecycle.launch scroll@{
                    delay(intervalMs.coerceAtLeast(100))
                    // 当smoothScrollToPosition()分发新的scrollState时，
                    // scrollJob还未转换到完成状态，提前置空避免无效取消。
                    scrollJob = null
                    if (currentPosition == NO_POSITION) return@scroll
                    val position = (currentPosition + 1) % adapter.itemCount
                    smoothScrollToPosition(position, provider = provider)
                }
            } else if (!it.canLoopScroll && scrollJob != null) {
                scrollJob?.cancel()
                scrollJob = null
            }
        }
    }
}

private fun LoopPagerController.stateIn(scope: CoroutineScope, adapter: Adapter<*>): StateFlow<BannerState> {
    val stateFlow = MutableStateFlow(BannerState(scrollState, supportLoop(adapter)))
    scope.launch(context = Dispatchers.Main.immediate, start = UNDISPATCHED) {
        val callback = object : OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (stateFlow.value.scrollState == state) return
                stateFlow.update { it.copy(scrollState = state) }
            }
        }
        val observer = object : AdapterDataObserver() {
            override fun onChanged() {
                val supportLoop = supportLoop(adapter)
                if (stateFlow.value.supportLoop == supportLoop) return
                stateFlow.update { it.copy(supportLoop = supportLoop) }
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onChanged()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = onChanged()
        }

        try {
            registerOnPageChangeCallback(callback)
            adapter.registerAdapterDataObserver(observer)
            awaitCancellation()
        } finally {
            unregisterOnPageChangeCallback(callback)
            adapter.unregisterAdapterDataObserver(observer)
        }
    }
    return stateFlow
}

private fun LoopPagerController.supportLoop(adapter: Adapter<*>): Boolean {
    return adapter.itemCount >= supportLoopCount
}

private data class BannerState(val scrollState: Int, val supportLoop: Boolean) {
    val canLoopScroll = scrollState == SCROLL_STATE_IDLE && supportLoop
}