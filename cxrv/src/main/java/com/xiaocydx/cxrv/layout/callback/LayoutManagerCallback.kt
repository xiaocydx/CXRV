package com.xiaocydx.cxrv.layout.callback

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*

/**
 * @author xcc
 * @date 2022/8/11
 */
internal interface LayoutManagerCallback {

    fun onAttachedToWindow(view: RecyclerView): Unit = Unit

    fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler): Unit = Unit

    fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?): Unit = Unit

    fun onLayoutCompleted(layout: LayoutManager, state: State): Unit = Unit
}