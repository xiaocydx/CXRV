package com.xiaocydx.cxrv.concat

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ConcatAdapterHolder
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.RecyclerView

/**
 * 获取item所占用的跨度空间数帮助类
 *
 * ### 不支持嵌套[ConcatAdapter]
 * 因为暂时没发现嵌套[ConcatAdapter]的场景意义，所以不支持嵌套结构的spanSize获取，
 * 若后续需要支持嵌套结构，则通过递归获取Adapter，创建访问缓存的方式实现spanSize获取。
 *
 * @author xcc
 * @date 2021/9/26
 */
class ConcatSpanSizeLookup(
    private val recyclerView: RecyclerView,
    private val layoutManager: GridLayoutManager,
    private val sourceLookup: SpanSizeLookup = layoutManager.sourceSpanSizeLookup
) : SpanSizeLookup() {
    /**
     * [layoutManager]的spanCount可能会被重新设置，
     * 因此每次都通过[layoutManager]获取spanCount。
     */
    private val spanCount: Int
        get() = layoutManager.spanCount
    private var concatAdapterHolder: ConcatAdapterHolder? = null

    /**
     * [recyclerView]的adapter可能会被重新设置，
     * 因此每次都通过[recyclerView]获取adapter。
     */
    override fun getSpanSize(position: Int): Int {
        val adapter: RecyclerView.Adapter<*>? = recyclerView.adapter
        checkAndUpdateConcatAdapter(adapter)
        return when {
            concatAdapterHolder != null -> concatAdapterHolder!!.getSpanSize(position, spanCount)
            adapter is SpanSizeProvider -> adapter.getSpanSize(position, spanCount)
            else -> sourceLookup.getSpanSize(position)
        }
    }

    private fun checkAndUpdateConcatAdapter(adapter: RecyclerView.Adapter<*>?) {
        if (concatAdapterHolder != null && concatAdapterHolder!!.concatAdapter != adapter) {
            // RecyclerView被设置了新的adapter，废弃concatAdapterHolder
            concatAdapterHolder!!.dispose()
            concatAdapterHolder = null
        }

        if (concatAdapterHolder == null && adapter is ConcatAdapter) {
            concatAdapterHolder = ConcatAdapterHolder(adapter, sourceLookup)
        }
    }
}

/**
 * 实际场景设置的原始[SpanSizeLookup]
 */
private val GridLayoutManager.sourceSpanSizeLookup: SpanSizeLookup
    get() = when (spanSizeLookup) {
        null, is ConcatSpanSizeLookup -> GridLayoutManager.DefaultSpanSizeLookup()
        else -> spanSizeLookup
    }