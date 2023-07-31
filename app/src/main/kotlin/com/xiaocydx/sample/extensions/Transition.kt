package com.xiaocydx.sample.extensions

import android.transition.Transition
import android.transition.Transition.TransitionListener

inline fun Transition.doOnEnd(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = addListener(onEnd = { transition, listener ->
    if (once) transition.removeListener(listener)
    action(transition)
})

inline fun Transition.doOnStart(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = addListener(onStart = { transition, listener ->
    if (once) transition.removeListener(listener)
    action(transition)
})

inline fun Transition.doOnCancel(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = addListener(onCancel = { transition, listener ->
    if (once) transition.removeListener(listener)
    action(transition)
})

inline fun Transition.doOnResume(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = addListener(onResume = { transition, listener ->
    if (once) transition.removeListener(listener)
    action(transition)
})

inline fun Transition.doOnPause(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = addListener(onPause = { transition, listener ->
    if (once) transition.removeListener(listener)
    action(transition)
})

inline fun Transition.addListener(
    crossinline onEnd: (transition: Transition, listener: TransitionListener) -> Unit = { _, _ -> },
    crossinline onStart: (transition: Transition, listener: TransitionListener) -> Unit = { _, _ -> },
    crossinline onCancel: (transition: Transition, listener: TransitionListener) -> Unit = { _, _ -> },
    crossinline onResume: (transition: Transition, listener: TransitionListener) -> Unit = { _, _ -> },
    crossinline onPause: (transition: Transition, listener: TransitionListener) -> Unit = { _, _ -> }
): TransitionListener {
    val listener = object : TransitionListener {
        override fun onTransitionEnd(transition: Transition) = onEnd(transition, this)
        override fun onTransitionResume(transition: Transition) = onResume(transition, this)
        override fun onTransitionPause(transition: Transition) = onPause(transition, this)
        override fun onTransitionCancel(transition: Transition) = onCancel(transition, this)
        override fun onTransitionStart(transition: Transition) = onStart(transition, this)
    }
    addListener(listener)
    return listener
}
