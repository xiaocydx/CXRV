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

package com.xiaocydx.cxrv.internal

import android.os.Handler
import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import androidx.annotation.CallSuper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume

/**
 * 没有选择使用[awaitFrame]的原因是要增加[delayMillis]参数
 */
internal suspend fun Choreographer.awaitFrame(
    delayMillis: Long = 0L
): Long = suspendCancellableCoroutine { cont ->
    val callback = FrameCallback { cont.resume(it) }
    postFrameCallbackDelayed(callback, delayMillis)
    cont.invokeOnCancellation { removeFrameCallback(callback) }
}

/**
 * 往Animation类型`CallbackQueue`前面添加[FrameCallback]的[Choreographer]调度器
 */
internal open class ChoreographerContext : CoroutineDispatcher(), DispatchAtFrontOfQueue {
    private val thread = Thread.currentThread()
    private val choreographer = Choreographer.getInstance()

    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread() !== thread
    }

    /**
     *  **注意**：负延时不等于[Handler.postAtFrontOfQueue]往队列头部插入消息的作用，
     *  负延时只能让[dispatch]添加的全部[FrameCallback]比其它[FrameCallback]先执行。
     */
    @CallSuper
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        choreographer.postFrameCallbackDelayed({ block.run() }, Long.MIN_VALUE)
    }

    final override fun equals(other: Any?): Boolean {
        return other is ChoreographerContext && other.choreographer === choreographer
    }

    final override fun hashCode(): Int {
        return System.identityHashCode(choreographer)
    }
}