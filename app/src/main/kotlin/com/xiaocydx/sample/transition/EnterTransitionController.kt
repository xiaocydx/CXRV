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

package com.xiaocydx.sample.transition

import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.MessageQueue
import android.view.View
import android.view.animation.Animation
import android.widget.ProgressBar
import androidx.annotation.MainThread
import androidx.core.view.doOnNextLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import kotlinx.coroutines.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import android.transition.Transition as AndroidTransition
import androidx.transition.Transition as AndroidXTransition

/**
 * `Fragment.enterTransition`的控制器
 *
 * @author xcc
 * @date 2023/2/4
 */
class EnterTransitionController(private val fragment: Fragment) {
    private var isPostponed = false
    private var postponeEnterJob: Job? = null
    private var enterTransitionJob: Job? = null
    private var enterTransitionProvider = defaultEnterTransitionProvider

    /**
     * 推迟`enterTransition`，可以跟[startPostponeEnterTransitionOrAwait]搭配使用
     *
     * @param timeMillis `enterTransition`的推迟时长，单位毫秒
     */
    @MainThread
    fun postponeEnterTransition(timeMillis: Long) {
        assertMainThread()
        if (timeMillis <= 0 || isPostponed) return
        isPostponed = true

        fragment.registerPostponeSavedStateProvider()
        if (fragment.consumeRestoredStateForPostpone()) {
            // fragment的重建流程不需要处理过渡动画
            return
        }

        // 该函数的调用时机不确定，在viewLifecycle创建之前，或者创建之后，
        // 并且当前处理的是fragment过渡动画，跟fragment的整体生命周期有关，
        // 因此使用lifecycleScope，而不是viewLifecycle.coroutineScope。
        val coroutineScope = fragment.lifecycleScope
        postponeEnterJob = coroutineScope.launch {
            fragment.postponeEnterTransition()
            delay(timeMillis)
        }

        enterTransitionJob = coroutineScope.launch {
            postponeEnterJob?.join()
            postponeEnterJob = null
            fragment.startPostponedEnterTransition()
            fragment.enterTransitionProvider().let(::EnterTransitionCompat).awaitEnd()
        }

        enterTransitionJob!!.invokeOnCompletion {
            postponeEnterJob = null
            enterTransitionJob = null
            enterTransitionProvider = defaultEnterTransitionProvider
        }
    }

    /**
     * 开始[postponeEnterTransition]推迟的`enterTransition`，
     * 若推迟`enterTransition`已完成，则等待过渡动画运行结束。
     */
    @MainThread
    suspend fun startPostponeEnterTransitionOrAwait() {
        assertMainThread()
        awaitMainThreadIdle()
        postponeEnterJob?.cancel() ?: enterTransitionJob?.join()
    }

    @MainThread
    @SinceKotlin("999.9")
    @Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")
    fun setEnterTransitionProvider(provider: Fragment.() -> Any) {
        // fragment.sharedElementEnterTransition的表现待验证
        assertMainThread()
        enterTransitionProvider = provider
    }

    /**
     * 主动开始`enterTransition`之前，先等待主线程空闲，尽可能避免动画被堆积的消息影响，
     * 若`enterTransition`的推迟时间到达，还未等到主线程空闲，则结束挂起，等待动画结束。
     */
    @MainThread
    private suspend fun awaitMainThreadIdle() {
        val postponeEnterJob = postponeEnterJob
        if (postponeEnterJob == null || postponeEnterJob.isCompleted) return

        val queue = Looper.myQueue()
        if (Build.VERSION.SDK_INT >= 23 && queue.isIdle) return

        tryPreventPostSyncBarrier {
            suspendCancellableCoroutine { cont ->
                var idleHandler: MessageQueue.IdleHandler? = null
                val disposable = postponeEnterJob.invokeOnCompletion {
                    idleHandler?.let(queue::removeIdleHandler)
                    cont.resume(Unit)
                }

                idleHandler = MessageQueue.IdleHandler {
                    disposable.dispose()
                    cont.resume(Unit)
                    false
                }
                queue.addIdleHandler(idleHandler)

                cont.invokeOnCancellationSafely {
                    disposable.dispose()
                    queue.removeIdleHandler(idleHandler)
                }
            }
        }
    }

    /**
     * [Fragment.postponeEnterTransition]不会对`Fragment.view`的添加过程造成影响，
     * 添加至视图树的`Fragment.view`，可能包含不断调用[View.invalidate]进行绘制的逻辑，
     * 例如无限循环的[Animation]或者低版本的[ProgressBar]，每一帧`doFrame`消息处理完，
     * 主线程消息队列都添加了同步屏障，这会导致[MessageQueue.IdleHandler]长时间不执行，
     * 进而将[awaitMainThreadIdle]的执行逻辑变为等待`enterTransition`的推迟时间到达。
     *
     * 等待主线程空闲之前，先将`Fragment.view.isVisible`设为`false`，尝试避免不断添加同步屏障，
     * 设置`Fragment.view.isVisible`会分发调用子View的可视变更回调，若子View处理了不可视的情况，
     * 不再不断调用[View.invalidate]进行绘制，则表示尝试成功。
     *
     * **注意**：该函数只能尝试避免`Fragment.view`的子View不断添加同步屏障的问题，
     * 不能避免`parent`或者同级子View不断添加同步屏障的问题，这需要调用者自己协调。
     */
    private suspend inline fun tryPreventPostSyncBarrier(block: () -> Unit) {
        val view = fragment.view
        if (view == null || !view.isVisible) {
            block()
            return
        }

        if (!view.isLaidOut && view.isLayoutRequested) {
            // 等待view完成一次布局，确保view有尺寸，过渡动画能正常运行
            suspendCancellableCoroutine { cont ->
                view.doOnNextLayout { cont.resume(Unit) }
            }
        }

        // 这是跟调用Lock.lock()和Lock.unlock()类似的写法，
        // view.isVisible = false写进try {...}，若抛出异常，
        // 则finally {...}会再抛出异常，导致首次异常信息丢失。
        view.isVisible = false
        try {
            block()
        } finally {
            view.isVisible = true
        }
    }

    private companion object : SavedStateRegistry.SavedStateProvider {
        private const val KEY = "com.xiaocydx.sample.transition.EnterTransitionController"
        private val defaultEnterTransitionProvider: Fragment.() -> Any = {
            requireNotNull(enterTransition) { "Fragment未设置enterTransition" }
        }

        override fun saveState() = Bundle(1)

        fun Fragment.consumeRestoredStateForPostpone(): Boolean {
            return savedStateRegistry.consumeRestoredStateForKey(KEY) != null
        }

        fun Fragment.registerPostponeSavedStateProvider() {
            savedStateRegistry.registerSavedStateProvider(KEY, this@Companion)
        }
    }
}

private fun assertMainThread() {
    assert(Thread.currentThread() === Looper.getMainLooper().thread) {
        "当前是处理Fragment过渡动画的流程，必须在主线程调用该函数"
    }
}

private inline fun CancellableContinuation<*>.invokeOnCancellationSafely(
    crossinline handler: CompletionHandler
) = invokeOnCancellation {
    assertMainThread()
    handler.invoke(it)
}

private class EnterTransitionCompat(private val enterTransition: Any) {

    @MainThread
    suspend fun awaitEnd() {
        assertMainThread()
        when (enterTransition) {
            is AndroidTransition -> enterTransition.awaitEnd()
            is AndroidXTransition -> enterTransition.awaitEnd()
            else -> throw IllegalArgumentException(
                "enterTransition的类型必须是${AndroidTransition::class.java.name}，" +
                        "或者是${AndroidXTransition::class.java.name}"
            )
        }
    }

    @MainThread
    private suspend fun AndroidTransition.awaitEnd() = suspendCancellableCoroutine { cont ->
        val listener = AndroidTransitionOnEnd(cont)
        addListener(listener)
        cont.invokeOnCancellationSafely { removeListener(listener) }
    }

    @MainThread
    private suspend fun AndroidXTransition.awaitEnd() = suspendCancellableCoroutine { cont ->
        val listener = AndroidXTransitionOnEnd(cont)
        addListener(listener)
        cont.invokeOnCancellationSafely { removeListener(listener) }
    }

    private class AndroidTransitionOnEnd(
        private val continuation: Continuation<Unit>
    ) : AndroidTransition.TransitionListener {
        override fun onTransitionPause(transition: AndroidTransition) = Unit
        override fun onTransitionResume(transition: AndroidTransition) = Unit
        override fun onTransitionStart(transition: AndroidTransition) = Unit
        override fun onTransitionCancel(transition: AndroidTransition) = Unit
        override fun onTransitionEnd(transition: AndroidTransition) {
            transition.removeListener(this)
            continuation.resume(Unit)
        }
    }

    private class AndroidXTransitionOnEnd(
        private val continuation: Continuation<Unit>
    ) : AndroidXTransition.TransitionListener {
        override fun onTransitionPause(transition: AndroidXTransition) = Unit
        override fun onTransitionResume(transition: AndroidXTransition) = Unit
        override fun onTransitionStart(transition: AndroidXTransition) = Unit
        override fun onTransitionCancel(transition: AndroidXTransition) = Unit
        override fun onTransitionEnd(transition: AndroidXTransition) {
            transition.removeListener(this)
            continuation.resume(Unit)
        }
    }
}