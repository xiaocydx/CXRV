@file:Suppress("unused")

package com.xiaocydx.recycler.concat

import androidx.core.view.doOnAttach
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.recycler.R

/**
 * item所占用的跨度空间数提供者
 *
 * [SpanSizeProvider]的实现方式可参考[ViewAdapter]。
 *
 * @author xcc
 * @date 2021/9/27
 */
interface SpanSizeProvider {

    /**
     * item是否占用所有跨度空间
     *
     * 仅当[RecyclerView.getLayoutManager]为[StaggeredGridLayoutManager]时有效,
     * 实现方式对应[StaggeredGridLayoutManager.LayoutParams.setFullSpan]。
     *
     * **注意**：在[Adapter.onViewAttachedToWindow]下
     * 调用了[SpanSizeProvider.onViewAttachedToWindow]，该函数才会生效。
     *
     * @return item是否占用所有跨度空间，返回true表示占用所有跨度空间。
     */
    fun fullSpan(position: Int, holder: ViewHolder): Boolean = false

    /**
     * 获取item所占用的跨度空间数
     *
     * 仅当[RecyclerView.getLayoutManager]为[GridLayoutManager]时有效,
     * 实现方式对应[GridLayoutManager.SpanSizeLookup.getSpanSize]。
     *
     * **注意**：在[Adapter.onAttachedToRecyclerView]下
     * 调用了[SpanSizeProvider.onAttachedToRecyclerView]，该函数才会生效。
     *
     * @param spanCount 对应[GridLayoutManager.getSpanCount]
     * @return item所占用的跨度空间数，返回[spanCount]表示占用所有跨度空间。
     */
    fun getSpanSize(position: Int, spanCount: Int): Int = 1
}

/**
 * 显式Receiver，方便实现类内部调用扩展函数
 */
val SpanSizeProvider.spanSizeProvider: SpanSizeProvider
    get() = this

/**
 * 在[Adapter.onViewAttachedToWindow]下调用
 */
fun SpanSizeProvider.onViewAttachedToWindow(holder: ViewHolder) {
    (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)
        ?.let { it.isFullSpan = fullSpan(holder.bindingAdapterPosition, holder) }
}

/**
 * 在[Adapter.onAttachedToRecyclerView]下调用
 */
fun SpanSizeProvider.onAttachedToRecyclerView(
    recyclerView: RecyclerView
) = with(recyclerView) {
    if (getTag(R.id.tag_span_size_lookup) == true) {
        // 避免ConcatAdapter的adapter元素重复执行设置操作
        return
    }

    if (layoutManager != null) {
        trySetSpanSizeLookup()
    } else {
        // RecyclerView此时还未设置LayoutManager，将设置操作延后执行
        doOnAttach { trySetSpanSizeLookup() }
    }
    setTag(R.id.tag_span_size_lookup, true)
}

private fun RecyclerView.trySetSpanSizeLookup() {
    (layoutManager as? GridLayoutManager)
        ?.takeIf { it.spanSizeLookup !is ConcatSpanSizeLookup }
        ?.let { it.spanSizeLookup = ConcatSpanSizeLookup(this, it) }
}