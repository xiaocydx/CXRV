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

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.recyclerview.widget.RecyclerView.State
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

    override fun onPreAttachedToWindow(view: RecyclerView) {
        callbacks.accessEach { it.onPreAttachedToWindow(view) }
    }

    override fun onPreDetachedFromWindow(view: RecyclerView, recycler: Recycler) {
        callbacks.accessEach { it.onPreDetachedFromWindow(view, recycler) }
    }

    override fun onPreAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        callbacks.accessEach { it.onPreAdapterChanged(layout, oldAdapter, newAdapter) }
    }

    override fun onPreLayoutChildren(recycler: Recycler, state: State) {
        callbacks.accessEach { it.onPreLayoutChildren(recycler, state) }
    }

    override fun preRequestSimpleAnimationsInNextLayout() {
        callbacks.accessEach { it.preRequestSimpleAnimationsInNextLayout() }
    }

    override fun postCalculateItemDecorationsForChild(child: View, outRect: Rect) {
        callbacks.accessEach { it.postCalculateItemDecorationsForChild(child, outRect) }
    }

    override fun onPreItemsChanged(recyclerView: RecyclerView) {
        callbacks.accessEach { it.onPreItemsChanged(recyclerView) }
    }

    override fun onPreItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        callbacks.accessEach { it.onPreItemsAdded(recyclerView, positionStart, itemCount) }
    }

    override fun onPreItemsRemoved(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        callbacks.accessEach { it.onPreItemsRemoved(recyclerView, positionStart, itemCount) }
    }

    override fun onPreItemsUpdated(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        callbacks.accessEach { it.onPreItemsUpdated(recyclerView, positionStart, itemCount) }
    }

    override fun onPreItemsUpdated(recyclerView: RecyclerView, positionStart: Int, itemCount: Int, payload: Any?) {
        callbacks.accessEach { it.onPreItemsUpdated(recyclerView, positionStart, itemCount, payload) }
    }

    override fun onPreItemsMoved(recyclerView: RecyclerView, from: Int, to: Int, itemCount: Int) {
        callbacks.accessEach { it.onPreItemsMoved(recyclerView, from, to, itemCount) }
    }

    override fun onPreLayoutCompleted(layout: LayoutManager, state: State) {
        callbacks.accessEach { it.onPreLayoutCompleted(layout, state) }
    }

    override fun onCleared() {
        callbacks.accessEach { it.onCleared() }
    }
}