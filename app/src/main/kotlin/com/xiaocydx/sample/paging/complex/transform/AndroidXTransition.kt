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
import android.transition.Transition as AndroidTransition
import androidx.transition.Transition as AndroidXTransition

@SuppressLint("RestrictedApi")
internal fun AndroidXTransition.dependsOn(other: AndroidTransition, sceneRoot: ViewGroup) {
    other.addListener(object : AndroidTransition.TransitionListener {
        override fun onTransitionStart(transition: AndroidTransition) = start()
        override fun onTransitionPause(transition: AndroidTransition) = pause(sceneRoot)
        override fun onTransitionResume(transition: AndroidTransition) = resume(sceneRoot)
        override fun onTransitionCancel(transition: AndroidTransition) {
            other.removeListener(this)
            cancel()
        }

        override fun onTransitionEnd(transition: AndroidTransition) {
            other.removeListener(this)
            end()
        }
    })
}