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

package com.xiaocydx.cxrv.recycle.scrap

import android.view.Choreographer
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.runOnMainThread
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@MainThread
internal suspend fun RecyclerView.Adapter<*>.awaitDeadlineNs(): Long {
    assertMainThread()
    return suspendCancellableCoroutine { cont ->
        DeadlineNsObserver(this, cont).attach()
    }
}

@MainThread
internal class DeadlineNsObserver(
    private val adapter: RecyclerView.Adapter<*>,
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
        Choreographer.getInstance().postFrameCallbackDelayed({ frameTimeNs ->
            cont.resume(frameTimeNs)
        }, Long.MIN_VALUE)
    }
}