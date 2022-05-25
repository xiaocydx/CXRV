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
    protected var rv: RecyclerView? = null
        private set
    protected var adapter: Adapter<*>? = null
        private set

    @CallSuper
    open fun register(rv: RecyclerView, adapter: Adapter<*>) {
        this.rv = rv
        this.adapter = adapter
        adapter.registerAdapterDataObserver(this)
    }

    @CallSuper
    open fun unregister(rv: RecyclerView, adapter: Adapter<*>) {
        this.rv = null
        this.adapter = null
        adapter.unregisterAdapterDataObserver(this)
    }
}