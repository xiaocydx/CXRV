package com.xiaocydx.sample.viewpager2.loop

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.cxrv.viewpager2.loop.SmoothScrollerProvider
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.math.sqrt

/**
 * Banner轮播交互的协程示例代码
 *
 * 以下是轮播开始的必要条件：
 * 1. `Adapter.itemCount`支持循环。
 * 2. [Lifecycle.State]至少为[state]。
 * 3. 未通过触摸开始滚动，上一次平滑滚动结束。
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
    val provider = durationMs.takeIf { it > 0 }?.let(::BannerSmoothScrollerProvider)
    lifecycle.repeatOnLifecycle(state) {
        while (true) {
            awaitSupportLoop(adapter)
            delay(intervalMs.coerceAtLeast(100))
            awaitScrollIdle()
            if (currentPosition == NO_POSITION) continue
            val position = (currentPosition + 1) % adapter.itemCount
            smoothScrollToPosition(position, provider = provider)
        }
    }
}

/**
 * 处理轮播平滑滚动的时长和插值器
 */
private class BannerSmoothScrollerProvider(private val durationMs: Long) : SmoothScrollerProvider {
    private val interpolator = AccelerateDecelerateInterpolator()

    override fun create(context: Context): SmoothScroller = BannerSmoothScroller(context)

    private inner class BannerSmoothScroller(context: Context) : LinearSmoothScroller(context) {

        override fun onTargetFound(targetView: View, state: State, action: Action) {
            val dx = calculateDxToMakeVisible(targetView, horizontalSnapPreference)
            val dy = calculateDyToMakeVisible(targetView, verticalSnapPreference)
            val distance = sqrt((dx * dx + dy * dy).toDouble()).toInt()
            var time = calculateTimeForDeceleration(distance)
            time = durationMs.toInt().coerceAtLeast(time)
            if (time > 0) action.update(-dx, -dy, time, interpolator)
        }
    }
}

/**
 * [repeatOnLifecycle]确保在主线程取消[awaitSupportLoop]和[awaitScrollIdle]挂起的协程，反注册不会有并发问题。
 */
private fun assertMainThread() {
    assert(Thread.currentThread() === Looper.getMainLooper().thread)
}

/**
 * 等待`Adapter.itemCount`支持循环
 */
private suspend fun LoopPagerController.awaitSupportLoop(adapter: Adapter<*>) {
    if (adapter.itemCount >= supportLoopCount) return
    suspendCancellableCoroutine { cont ->
        val observer = object : AdapterDataObserver() {
            private var isResumed = false

            override fun onChanged() {
                if (isResumed || adapter.itemCount < supportLoopCount) return
                isResumed = true
                adapter.unregisterAdapterDataObserver(this)
                cont.resume(Unit)
            }

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onChanged()
        }

        adapter.registerAdapterDataObserver(observer)
        cont.invokeOnCancellation {
            assertMainThread()
            adapter.unregisterAdapterDataObserver(observer)
        }
    }
}

/**
 * 等待`scrollState`为[SCROLL_STATE_IDLE]
 *
 * 该函数处理两种情况：
 * 1. 若已通过触摸开始滚动，则等待手指抬起，才开始这一次平滑滚动。
 * 2. 若上一次平滑滚动还未结束，则等待其结束，才开始这一次平滑滚动。
 */
private suspend fun LoopPagerController.awaitScrollIdle() {
    if (scrollState == SCROLL_STATE_IDLE) return
    var callback: OnPageChangeCallback? = null
    suspendCancellableCoroutine { cont ->
        callback = object : OnPageChangeCallback() {
            private var isResumed = false

            override fun onPageScrollStateChanged(state: Int) {
                if (isResumed || state != SCROLL_STATE_IDLE) return
                isResumed = true
                cont.resume(Unit)
            }
        }
        registerOnPageChangeCallback(callback!!)
        cont.invokeOnCancellation {
            assertMainThread()
            unregisterOnPageChangeCallback(callback!!)
        }
    }
    // 分发过程不支持remove，因此用yield()跟分发过程错开
    yield()
    unregisterOnPageChangeCallback(callback!!)
}