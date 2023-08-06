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

package com.xiaocydx.sample.paging.complex.transform

import android.transition.Transition
import android.transition.Transition.TransitionListener

inline fun Transition.doOnEnd(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = object : TransitionListenerAdapter {
    override fun onTransitionEnd(transition: Transition) {
        if (once) transition.removeListener(this)
        action(transition)
    }
}.also(::addListener)

inline fun Transition.doOnStart(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = object : TransitionListenerAdapter {
    override fun onTransitionStart(transition: Transition) {
        if (once) transition.removeListener(this)
        action(transition)
    }
}.also(::addListener)

inline fun Transition.doOnCancel(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = object : TransitionListenerAdapter {
    override fun onTransitionCancel(transition: Transition) {
        if (once) transition.removeListener(this)
        action(transition)
    }
}.also(::addListener)

inline fun Transition.doOnResume(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = object : TransitionListenerAdapter {
    override fun onTransitionResume(transition: Transition) {
        if (once) transition.removeListener(this)
        action(transition)
    }
}.also(::addListener)

inline fun Transition.doOnPause(
    once: Boolean = false,
    crossinline action: (transition: Transition) -> Unit
): TransitionListener = object : TransitionListenerAdapter {
    override fun onTransitionPause(transition: Transition) {
        if (once) transition.removeListener(this)
        action(transition)
    }
}.also(::addListener)

interface TransitionListenerAdapter : TransitionListener {
    override fun onTransitionStart(transition: Transition) = Unit
    override fun onTransitionEnd(transition: Transition) = Unit
    override fun onTransitionCancel(transition: Transition) = Unit
    override fun onTransitionPause(transition: Transition) = Unit
    override fun onTransitionResume(transition: Transition) = Unit
}