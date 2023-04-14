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
import android.os.Looper
import android.os.MessageQueue
import androidx.annotation.CallSuper
import androidx.core.os.HandlerCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainCoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

/**
 * 主线程异步消息Handler
 */
private val asyncHandler by lazy { HandlerCompat.createAsync(Looper.getMainLooper()) }

/**
 * 当前是否为主线程
 */
internal val isMainThread: Boolean
    get() = Thread.currentThread() === Looper.getMainLooper().thread

/**
 * 若当前不为主线程，则将[action]post到主线程中执行
 */
internal inline fun runOnMainThread(crossinline action: () -> Unit) {
    if (isMainThread) action() else asyncHandler.post { action() }
}

/**
 * 断言当前为主线程
 */
internal fun assertMainThread() {
    assert(isMainThread) { "只能在主线程中调用当前函数" }
}

/**
 * **注意**：实现此接口的调度器仅用于特殊场景，例如解决doFrame消息插队问题
 */
internal sealed interface DispatchAtFrontOfQueue : ContinuationInterceptor {

    /**
     * 将[block]调度到队列前面，前面指的是插到其它执行逻辑之前，可以不是队列头部，
     * 若[block]调度失败，则取消`context.job`，并且不能通过其它调度器完成[block]。
     */
    fun dispatch(context: CoroutineContext, block: Runnable)
}

/**
 * 往[MessageQueue]头部插入消息的主线程调度器
 */
internal open class MainHandlerContext : MainCoroutineDispatcher(), DispatchAtFrontOfQueue {
    private val handler: Handler = asyncHandler
    final override val immediate: MainCoroutineDispatcher
        get() = this

    /**
     * 当前线程可能临时设置了主线程[Looper]，因此对比[Thread]而不是[Looper]
     */
    override fun isDispatchNeeded(context: CoroutineContext): Boolean {
        return Thread.currentThread() !== Looper.getMainLooper().thread
    }

    @CallSuper
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        if (!handler.postAtFrontOfQueue(block)) {
            context.cancel(CancellationException("往主线程发送消息被拒绝"))
        }
    }

    final override fun equals(other: Any?): Boolean {
        return other is MainHandlerContext && other.handler === handler
    }

    final override fun hashCode(): Int {
        return System.identityHashCode(handler)
    }
}