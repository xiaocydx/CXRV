package com.xiaocydx.sample.viewpager2.loop

import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/**
 * Banner轮播交互的协程示例代码
 *
 * 以下是轮播开始的必要条件：
 * 1. `Adapter.itemCount`支持循环。
 * 2. [Lifecycle.State]至少为[state]。
 * 3. 未通过触摸开始滚动，上一次平滑滚动结束。
 */
fun LoopPagerController.launchBanner(
    adapter: Adapter<*>,
    lifecycle: Lifecycle,
    state: Lifecycle.State = STARTED,
    intervalMs: Long = 1500
): Job = lifecycle.coroutineScope.launch {
    lifecycle.repeatOnLifecycle(state) {
        while (true) {
            awaitSupportLoop(adapter)
            delay(intervalMs.coerceAtLeast(300))
            awaitScrollIdle()
            if (currentPosition == NO_POSITION) continue
            val position = (currentPosition + 1) % adapter.itemCount
            smoothScrollToPosition(position)
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