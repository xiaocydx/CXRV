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

package com.xiaocydx.cxrv.recycle.prepare

import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.runOnMainThread
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 预创建的截至时间
 *
 * @author xcc
 * @date 2023/11/10
 */
internal fun interface PrepareDeadline {

    /**
     * 等待预创建的截至时间，单位ns
     */
    @MainThread
    suspend fun awaitDeadlineNs(): Long
}

/**
 * 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
 */
@Suppress("FunctionName")
internal fun FrameTimeDeadline(adapter: Adapter<*>) = PrepareDeadline {
    assertMainThread()
    suspendCancellableCoroutine { cont -> FrameTimeObserver(adapter, cont).attach() }
}

@MainThread
internal class FrameTimeObserver(
    private val adapter: Adapter<*>,
    private val cont: CancellableContinuation<Long>
) : RecyclerView.AdapterDataObserver() {
    private var isResumed = false
    private var isRegister = false

    fun attach() {
        if (adapter.itemCount > 0) return resume()
        registerAdapterDataObserver()
        cont.invokeOnCancellation { runOnMainThread(::unregisterAdapterDataObserver) }
    }

    override fun onChanged() = resume()

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = resume()

    private fun registerAdapterDataObserver() {
        isRegister = true
        adapter.registerAdapterDataObserver(this)
    }

    private fun unregisterAdapterDataObserver() {
        if (!isRegister) return
        isRegister = false
        adapter.unregisterAdapterDataObserver(this)
    }

    private fun resume() {
        if (isResumed) return
        isResumed = true
        unregisterAdapterDataObserver()
        val resumeAction = FrameCallback { frameTimeNs -> cont.resume(frameTimeNs) }
        // Long.MIN_VALUE / 2确保在ChoreographerContext调度之后、RecyclerView布局流程之前执行
        val beforeNextLayout = Long.MIN_VALUE / 2
        Choreographer.getInstance().postFrameCallbackDelayed(resumeAction, beforeNextLayout)
    }
}