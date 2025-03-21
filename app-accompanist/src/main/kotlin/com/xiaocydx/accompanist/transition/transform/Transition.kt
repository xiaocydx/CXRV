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

import android.animation.TypeEvaluator
import android.graphics.Matrix
import androidx.transition.Transition.TransitionListener
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.take

@Suppress("FunctionName")
fun MatrixEvaluator(): TypeEvaluator<Matrix> = TransitionUtils.MatrixEvaluator()

sealed class TransitionEvent {
    data object Start : TransitionEvent()
    data object End : TransitionEvent()
    data object Cancel : TransitionEvent()
    data object Pause : TransitionEvent()
    data object Resume : TransitionEvent()
}

/**
 * 通过[Flow]的操作符，简化[TransitionListener]的条件移除实现
 */
fun Transition.transitionEvent() = callbackFlow {
    val listener = object : TransitionListener {
        override fun onTransitionStart(transition: Transition) {
            trySend(TransitionEvent.Start)
        }

        override fun onTransitionEnd(transition: Transition) {
            trySend(TransitionEvent.End)
        }

        override fun onTransitionCancel(transition: Transition) {
            trySend(TransitionEvent.Cancel)
        }

        override fun onTransitionPause(transition: Transition) {
            trySend(TransitionEvent.Pause)
        }

        override fun onTransitionResume(transition: Transition) {
            trySend(TransitionEvent.Resume)
        }
    }
    addListener(listener)
    awaitClose { removeListener(listener) }
}.buffer(Channel.UNLIMITED)

inline fun <reified T : TransitionEvent> Flow<TransitionEvent>.takeFirst() =
        filterIsInstance<T>().take(count = 1)