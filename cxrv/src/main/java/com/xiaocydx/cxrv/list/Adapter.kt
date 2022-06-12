package com.xiaocydx.cxrv.list

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.concat.ViewAdapter

/**
 * 设置[Adapter]，可用于链式调用场景
 *
 * ```
 * val adapter: Adapter<*> = ...
 * recyclerView.adapter(adapter)
 * ```
 */
infix fun <T : RecyclerView> T.adapter(adapter: Adapter<*>): T {
    this.adapter = adapter
    return this
}

/**
 * 获取[child]在绑定适配器中的position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildBindingAdapterPosition(child: View): Int {
    return getChildViewHolder(child)?.bindingAdapterPosition ?: RecyclerView.NO_POSITION
}

/**
 * 获取[child]的绑定适配器的最后position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildLastBindingAdapterPosition(child: View): Int {
    return getChildBindingAdapterItemCount(child) - 1
}

/**
 * 获取[child]的绑定适配器的itemCount
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildBindingAdapterItemCount(child: View): Int {
    return getChildBindingAdapter(child)?.itemCount ?: 0
}

/**
 * 获取[child]的绑定适配器
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildBindingAdapter(child: View): Adapter<*>? {
    return getChildViewHolder(child)?.bindingAdapter
}

/**
 * [child]在绑定适配器中的position是否为起始position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.isFirstChildBindingAdapterPosition(child: View): Boolean {
    return getChildBindingAdapterPosition(child) == 0
}

/**
 * [child]在绑定适配器中的position是否为最后position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.isLastChildBindingAdapterPosition(child: View): Boolean {
    return getChildBindingAdapterPosition(child) == getChildLastBindingAdapterPosition(child)
}

/**
 * [child]是否为Header或Footer
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.isHeaderOrFooter(child: View): Boolean {
    return getChildViewHolder(child)?.bindingAdapter is ViewAdapter<*>
}

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