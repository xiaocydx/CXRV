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

package com.xiaocydx.cxrv.viewpager2.pageloop

import android.view.Choreographer
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

/**
 * 当`adapter.itemCount`大于0时，调用[action]
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class NotEmptyObserver(
    private val adapter: Adapter<*>,
    private val action: () -> Unit
) : RecyclerView.AdapterDataObserver() {
    private var isCompleted = false

    init {
        if (adapter.itemCount > 0) {
            isCompleted = true
            action()
        } else {
            adapter.registerAdapterDataObserver(this)
        }
    }

    fun removeObserver() {
        if (isCompleted) return
        adapter.unregisterAdapterDataObserver(this)
    }

    override fun onChanged() = tryComplete()

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onChanged()

    private fun tryComplete() {
        if (isCompleted || adapter.itemCount == 0) return
        removeObserver()
        isCompleted = true
        // 跟AdapterDataObserver的分发流程错开，并确保在下一帧rv布局流程之前调用action()
        Choreographer.getInstance().postFrameCallbackDelayed({ action() }, Long.MIN_VALUE)
    }
}