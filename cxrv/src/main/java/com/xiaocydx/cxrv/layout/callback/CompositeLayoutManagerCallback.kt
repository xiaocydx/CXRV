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

package com.xiaocydx.cxrv.layout.callback

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.accessEach

/**
 * [LayoutManagerCallback]的分发调度器
 *
 * @author xcc
 * @date 2022/8/11
 */
internal class CompositeLayoutManagerCallback(initialCapacity: Int) : LayoutManagerCallback {
    private val callbacks = ArrayList<LayoutManagerCallback>(initialCapacity)

    fun addLayoutManagerCallback(callback: LayoutManagerCallback) {
        if (callbacks.contains(callback)) return
        callbacks.add(callback)
    }

    fun removeLayoutManagerCallback(callback: LayoutManagerCallback) {
        if (!callbacks.remove(callback)) return
        callback.onCleared()
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        callbacks.accessEach { it.onAttachedToWindow(view) }
    }

    override fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler) {
        callbacks.accessEach { it.onDetachedFromWindow(view, recycler) }
    }

    override fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        callbacks.accessEach { it.onAdapterChanged(layout, oldAdapter, newAdapter) }
    }

    override fun onLayoutChildren(recycler: Recycler, state: State) {
        callbacks.accessEach { it.onLayoutChildren(recycler, state) }
    }

    override fun requestSimpleAnimationsInNextLayout() {
        callbacks.accessEach { it.requestSimpleAnimationsInNextLayout() }
    }

    override fun onLayoutCompleted(layout: LayoutManager, state: State) {
        callbacks.accessEach { it.onLayoutCompleted(layout, state) }
    }

    override fun onCleared() {
        callbacks.accessEach { it.onCleared() }
    }
}