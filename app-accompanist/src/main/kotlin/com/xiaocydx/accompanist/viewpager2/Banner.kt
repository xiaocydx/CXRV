/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.accompanist.viewpager2

import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
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
    val stateFlow = stateIn(adapter, lifecycle, state, scope = this)
    val provider = durationMs.takeIf { it > 0 }?.let {
        LinearSmoothScrollerProvider(it, AccelerateDecelerateInterpolator())
    }
    var scrollJob: Job? = null
    stateFlow.collect {
        if (it.canScroll && scrollJob == null) {
            scrollJob = launch scroll@{
                delay(intervalMs.coerceAtLeast(100))
                // 当smoothScrollToPosition()分发新的scrollState时，
                // scrollJob还未转换到完成状态，提前置空避免无效取消。
                scrollJob = null
                if (currentPosition == NO_POSITION) return@scroll
                val position = (currentPosition + 1) % adapter.itemCount
                smoothScrollToPosition(position, provider = provider)
            }
        } else if (!it.canScroll && scrollJob != null) {
            scrollJob?.cancel()
            scrollJob = null
        }
    }
}

private fun LoopPagerController.stateIn(
    adapter: Adapter<*>,
    lifecycle: Lifecycle,
    state: Lifecycle.State,
    scope: CoroutineScope,
): StateFlow<BannerState> {
    val stateFlow = MutableStateFlow(BannerState(lifecycle.isActive(state), scrollState, supportLoop(adapter)))
    scope.launch(context = Dispatchers.Main.immediate, start = UNDISPATCHED) {
        val lifecycleObserver = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                val isActive = lifecycle.isActive(state)
                if (stateFlow.value.isActive == isActive) return
                stateFlow.update { it.copy(isActive = isActive) }
            }
        }

        val pageChangeCallback = object : OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                if (stateFlow.value.scrollState == state) return
                stateFlow.update { it.copy(scrollState = state) }
            }
        }

        val adapterDataObserver = object : AdapterDataObserver() {
            override fun onChanged() {
                val supportLoop = supportLoop(adapter)
                if (stateFlow.value.supportLoop == supportLoop) return
                stateFlow.update { it.copy(supportLoop = supportLoop) }
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onChanged()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = onChanged()
        }

        try {
            lifecycle.addObserver(lifecycleObserver)
            registerOnPageChangeCallback(pageChangeCallback)
            adapter.registerAdapterDataObserver(adapterDataObserver)
            awaitCancellation()
        } finally {
            lifecycle.removeObserver(lifecycleObserver)
            unregisterOnPageChangeCallback(pageChangeCallback)
            adapter.unregisterAdapterDataObserver(adapterDataObserver)
        }
    }
    return stateFlow
}

private fun Lifecycle.isActive(state: Lifecycle.State): Boolean {
    return currentState.isAtLeast(state)
}

private fun LoopPagerController.supportLoop(adapter: Adapter<*>): Boolean {
    return adapter.itemCount >= supportLoopCount
}

private data class BannerState(val isActive: Boolean, val scrollState: Int, val supportLoop: Boolean) {
    val canScroll = isActive && scrollState == SCROLL_STATE_IDLE && supportLoop
}