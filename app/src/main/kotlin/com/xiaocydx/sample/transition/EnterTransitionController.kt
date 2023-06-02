package com.xiaocydx.sample.transition

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.savedstate.SavedStateRegistry
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import androidx.transition.Transition as AndroidXTransition

/**
 * `Fragment.enterTransition`的控制器
 *
 * @author xcc
 * @date 2023/2/4
 */
class EnterTransitionController(private val fragment: Fragment) {
    private var isStarted = false
    private var isPostponed = false
    private var enterTransition: AndroidXTransition? = null

    /**
     * 推迟`Fragment.enterTransition`
     *
     * @param timeoutMillis 推迟的超时时长
     */
    @MainThread
    fun postponeEnterTransition(timeoutMillis: Long) {
        assertMainThread()
        if (timeoutMillis <= 0 || isStarted || isPostponed) return
        isPostponed = true
        // TODO: Android 8.0以下存在同步屏障未移除的情况
        fragment.postponeEnterTransition(timeoutMillis, TimeUnit.MILLISECONDS)
        enterTransition = requireNotNull(fragment.enterTransition as AndroidXTransition)
        enterTransition!!.addListener(AndroidXTransitionOnStart())
        fragment.setSavedStateProvider()
    }

    /**
     * 开始`Fragment.enterTransition`或者等待结束
     */
    @MainThread
    suspend fun startPostponeEnterTransitionOrAwait() {
        assertMainThread()
        val transition = enterTransition
        if (transition == null || !isPostponed) return
        awaitMainThreadIdle(transition)
        if (!isStarted) {
            fragment.startPostponedEnterTransition()
        } else {
            transition.awaitEnd()
        }
    }

    private fun assertMainThread() {
        assert(Thread.currentThread() === Looper.getMainLooper().thread)
    }

    /**
     * 主动开始[transition]之前，先等待主线程空闲，尽可能让动画运行期间不被堆积的消息影响，
     * 若[transition]的超时时间到达，还未等到主线程空闲，则结束挂起，等待[transition]结束。
     */
    private suspend fun awaitMainThreadIdle(transition: AndroidXTransition) {
        // fragment重建流程不需要等待主线程空闲
        if (fragment.consumeRestoredState()) return
        val queue = Looper.myQueue()
        if (isStarted || Build.VERSION.SDK_INT >= 23 && queue.isIdle) return
        suspendCancellableCoroutine { cont ->
            var idleHandler: MessageQueue.IdleHandler? = null
            val listener = object : AndroidXTransitionListener() {
                override fun onTransitionStart(transition: AndroidXTransition) {
                    transition.removeListener(this)
                    idleHandler?.let(queue::removeIdleHandler) ?: return
                    cont.resume(Unit)
                }
            }
            transition.addListener(listener)

            idleHandler = MessageQueue.IdleHandler {
                transition.removeListener(listener)
                cont.resume(Unit)
                false
            }
            queue.addIdleHandler(idleHandler)

            cont.invokeOnCancellation {
                assertMainThread()
                transition.removeListener(listener)
                queue.removeIdleHandler(idleHandler)
            }
        }
    }

    private suspend fun AndroidXTransition.awaitEnd() = suspendCancellableCoroutine { cont ->
        val listener = object : AndroidXTransitionListener() {
            override fun onTransitionEnd(transition: AndroidXTransition) {
                removeListener(this)
                cont.resume(Unit)
            }
        }
        addListener(listener)
        cont.invokeOnCancellation {
            assertMainThread()
            removeListener(listener)
        }
    }

    private inner class AndroidXTransitionOnStart : AndroidXTransitionListener() {
        override fun onTransitionStart(transition: AndroidXTransition) {
            transition.removeListener(this)
            isStarted = true
        }
    }

    private abstract class AndroidXTransitionListener : AndroidXTransition.TransitionListener {
        override fun onTransitionPause(transition: AndroidXTransition) = Unit
        override fun onTransitionResume(transition: AndroidXTransition) = Unit
        override fun onTransitionStart(transition: AndroidXTransition) = Unit
        override fun onTransitionCancel(transition: AndroidXTransition) = Unit
        override fun onTransitionEnd(transition: AndroidXTransition) = Unit
    }

    private companion object : SavedStateRegistry.SavedStateProvider {
        private const val KEY = "com.xiaocydx.sample.transition.EnterTransitionController"

        override fun saveState() = Bundle(1)

        fun Fragment.consumeRestoredState(): Boolean {
            return savedStateRegistry.consumeRestoredStateForKey(KEY) != null
        }

        fun Fragment.setSavedStateProvider() {
            savedStateRegistry.unregisterSavedStateProvider(KEY)
            savedStateRegistry.registerSavedStateProvider(KEY, this@Companion)
        }
    }
}