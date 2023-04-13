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

package com.xiaocydx.cxrv.recycle

import android.view.Choreographer
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext

/**
 * @author xcc
 * @date 2023/4/13
 */
internal open class ChoreographerDispatcher : CoroutineDispatcher() {
    private val thread = Thread.currentThread()
    private val choreographer = Choreographer.getInstance()

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread() !== thread
    }

    @CallSuper
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        choreographer.postFrameCallback { block.run() }
    }

    final override fun equals(other: Any?): Boolean {
        return other is ChoreographerDispatcher
                && other.choreographer === choreographer
    }

    final override fun hashCode(): Int {
        return System.identityHashCode(choreographer)
    }
}