package com.xiaocydx.cxrv.layout.callback

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*

/**
 * [LayoutManager]的部分函数回调
 *
 * @author xcc
 * @date 2022/8/11
 */
internal interface LayoutManagerCallback {

    /**
     * 对应[LayoutManager.onAttachedToWindow]
     *
     * 该函数应当在`super.onAttachedToWindow(view)`之前被调用。
     */
    fun onAttachedToWindow(view: RecyclerView): Unit = Unit

    /**
     * 对应[LayoutManager.onDetachedFromWindow]
     *
     * 该函数在`super.onDetachedFromWindow(view, recycler)`之前被调用。
     */
    fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler): Unit = Unit

    /**
     * 对应[LayoutManager.onAdapterChanged]
     */
    fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?): Unit = Unit

    /**
     * 对应[LayoutManager.onLayoutCompleted]
     */
    fun onLayoutCompleted(layout: LayoutManager, state: State): Unit = Unit

    /**
     * 从[CompositeLayoutManagerCallback]移除时，该函数会被调用
     */
    fun onCleared(): Unit = Unit
}