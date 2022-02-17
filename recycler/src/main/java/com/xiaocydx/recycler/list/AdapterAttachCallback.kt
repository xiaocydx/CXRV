package com.xiaocydx.recycler.list

import androidx.recyclerview.widget.RecyclerView

/**
 * 适配器Attach相关的回调
 *
 * @author xcc
 * @date 2021/11/16
 */
interface AdapterAttachCallback {

    fun onAttachedToRecyclerView(recyclerView: RecyclerView): Unit = Unit

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView): Unit = Unit
}

/**
 * 在适配器附加到RecyclerView上时，调用一次[block]。
 */
inline fun ListAdapter<*, *>.doOnAttach(crossinline block: (RecyclerView) -> Unit) {
    recyclerView?.apply(block)?.let { return }
    addAdapterAttachCallback(object : AdapterAttachCallback {
        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            removeAdapterAttachCallback(this)
            block(recyclerView)
        }
    })
}