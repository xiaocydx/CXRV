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

    override fun onLayoutCompleted(layout: LayoutManager, state: State) {
        callbacks.accessEach { it.onLayoutCompleted(layout, state) }
    }
}