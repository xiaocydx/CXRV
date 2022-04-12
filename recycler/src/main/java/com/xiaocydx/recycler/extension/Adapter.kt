package com.xiaocydx.recycler.extension

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.recycler.concat.ViewAdapter

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
 * 是否有可显示的item
 */
internal val Adapter<*>.hasDisplayItem: Boolean
    get() = itemCount > 0

/**
 * [position]是否为第一个可显示的item
 */
internal fun Adapter<*>.isFirstDisplayItem(position: Int): Boolean {
    return hasDisplayItem && position == 0
}

/**
 * [position]是否为最后一个可显示的item
 */
internal fun Adapter<*>.isLastDisplayItem(position: Int): Boolean {
    return position == itemCount - 1
}