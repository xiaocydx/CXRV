package com.xiaocydx.sample.transition

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import android.transition.Transition as AndroidTransition
import androidx.transition.Transition as AndroidXTransition

/**
 * @author xcc
 * @date 2023/2/4
 */
class EnterTransitionController(
    private val fragment: Fragment,
    private val mainDispatcher: MainCoroutineDispatcher = Dispatchers.Main.immediate
) {
    private var isStarted = false
    private var isPostponed = false
    private var enterTransition: Any? = null
    private var continuation: Continuation<Unit>? = null
    private var androidTransitionListener: AndroidTransitionListener? = null
    private var androidXTransitionListener: AndroidXTransitionListener? = null

    fun postponeEnterTransition(timeoutMillis: Long) = runOnMainThread {
        if (timeoutMillis <= 0 || isPostponed) return@runOnMainThread
        isPostponed = true
        fragment.postponeEnterTransition(timeoutMillis, TimeUnit.MILLISECONDS)
        enterTransition = fragment.enterTransition
        addEnterTransitionListener()
    }

    suspend fun startPostponeEnterTransitionOrAwait() = withMainDispatcher {
        if (!isPostponed) return@withMainDispatcher
        if (!isStarted) {
            removeEnterTransitionListener()
            fragment.startPostponedEnterTransition()
            return@withMainDispatcher
        }
        suspendCancellableCoroutine { cont -> continuation = cont }
    }

    @MainThread
    private fun onEnterTransitionStart() {
        isStarted = true
    }

    @MainThread
    private fun addEnterTransitionListener() {
        when (val transition = enterTransition) {
            is AndroidTransition -> {
                androidTransitionListener = AndroidTransitionListener()
                transition.addListener(androidTransitionListener)
            }
            is AndroidXTransition -> {
                androidXTransitionListener = AndroidXTransitionListener()
                transition.addListener(androidXTransitionListener!!)
            }
        }
    }

    @MainThread
    private fun removeEnterTransitionListener() {
        when (val transition = enterTransition) {
            is AndroidTransition -> androidTransitionListener?.let(transition::removeListener)
            is AndroidXTransition -> androidXTransitionListener?.let(transition::removeListener)
        }
        isStarted = false
        enterTransition = null
        androidTransitionListener = null
        androidXTransitionListener = null
        continuation?.resume(Unit)
        continuation = null
    }

    private inline fun runOnMainThread(crossinline block: () -> Unit) {
        val dispatcher = mainDispatcher.immediate
        if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            dispatcher.dispatch(EmptyCoroutineContext) { block() }
        } else {
            block()
        }
    }

    private suspend inline fun withMainDispatcher(crossinline block: suspend () -> Unit) {
        val dispatcher = mainDispatcher.immediate
        if (dispatcher.isDispatchNeeded(EmptyCoroutineContext)) {
            withContext(dispatcher) { block() }
        } else {
            block()
        }
    }

    private inner class AndroidTransitionListener : AndroidTransition.TransitionListener {
        override fun onTransitionPause(transition: AndroidTransition) = Unit
        override fun onTransitionResume(transition: AndroidTransition) = Unit
        override fun onTransitionStart(transition: AndroidTransition) = onEnterTransitionStart()
        override fun onTransitionCancel(transition: AndroidTransition) = removeEnterTransitionListener()
        override fun onTransitionEnd(transition: AndroidTransition) = removeEnterTransitionListener()
    }

    private inner class AndroidXTransitionListener : AndroidXTransition.TransitionListener {
        override fun onTransitionPause(transition: AndroidXTransition) = Unit
        override fun onTransitionResume(transition: AndroidXTransition) = Unit
        override fun onTransitionStart(transition: AndroidXTransition) = onEnterTransitionStart()
        override fun onTransitionCancel(transition: AndroidXTransition) = removeEnterTransitionListener()
        override fun onTransitionEnd(transition: AndroidXTransition) = removeEnterTransitionListener()
    }
}