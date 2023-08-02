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

@file:JvmName("TransitionInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.transition

import android.annotation.SuppressLint
import android.view.ViewGroup

@SuppressLint("RestrictedApi")
fun Transition.dependsOn(other: Transition, sceneRoot: ViewGroup) {
    val source = this
    if (source === other) return
    other.addListener(object : Transition.TransitionListener {
        override fun onTransitionStart(transition: Transition) = source.start()
        override fun onTransitionPause(transition: Transition) = source.pause(sceneRoot)
        override fun onTransitionResume(transition: Transition) = source.resume(sceneRoot)
        override fun onTransitionCancel(transition: Transition) {
            other.removeListener(this)
            source.cancel()
        }

        override fun onTransitionEnd(transition: Transition) {
            other.removeListener(this)
            source.end()
        }
    })
}