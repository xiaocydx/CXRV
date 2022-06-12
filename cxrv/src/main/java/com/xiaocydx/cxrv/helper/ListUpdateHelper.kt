package com.xiaocydx.cxrv.helper

import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver

/**
 * 列表更新帮助类
 *
 * @author xcc
 * @date 2022/4/27
 */
abstract class ListUpdateHelper : AdapterDataObserver() {
    private val observer = AdapterDataObserverWrapper()
    protected var rv: RecyclerView? = null
        private set
    protected var adapter: Adapter<*>? = null
        private set
    protected val previousItemCount: Int
        get() = observer.previousItemCount

    @CallSuper
    open fun register(rv: RecyclerView, adapter: Adapter<*>) {
        this.rv = rv
        this.adapter = adapter
        observer.recordItemCount()
        adapter.registerAdapterDataObserver(observer)
    }

    @CallSuper
    open fun unregister(rv: RecyclerView, adapter: Adapter<*>) {
        this.rv = null
        this.adapter = null
        observer.recordItemCount()
        adapter.unregisterAdapterDataObserver(observer)
    }

    private inner class AdapterDataObserverWrapper : AdapterDataObserver() {
        var previousItemCount = 0

        fun recordItemCount() {
            previousItemCount = adapter?.itemCount ?: 0
        }

        override fun onChanged() {
            this@ListUpdateHelper.onChanged()
            recordItemCount()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            this@ListUpdateHelper.onItemRangeChanged(positionStart, itemCount, payload)
            recordItemCount()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            this@ListUpdateHelper.onItemRangeInserted(positionStart, itemCount)
            recordItemCount()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            this@ListUpdateHelper.onItemRangeRemoved(positionStart, itemCount)
            recordItemCount()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            this@ListUpdateHelper.onItemRangeMoved(fromPosition, toPosition, itemCount)
            recordItemCount()
        }
    }
}