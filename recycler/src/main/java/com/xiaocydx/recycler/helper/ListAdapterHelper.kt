package com.xiaocydx.recycler.helper

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.recycler.extension.doOnFrameComplete
import com.xiaocydx.recycler.extension.hasDisplayItem
import com.xiaocydx.recycler.list.AdapterAttachCallback
import com.xiaocydx.recycler.list.ListAdapter

/**
 * [ListAdapter]的更新帮助类
 *
 * @author xcc
 * @date 2021/11/16
 */
internal class ListAdapterHelper(
    private val adapter: ListAdapter<*, *>
) : AdapterDataObserver(), AdapterAttachCallback {
    private var helper = ItemVisibleHelper()
    private var previousEmpty = !adapter.hasDisplayItem
    private val recyclerView: RecyclerView?
        inline get() = helper.recyclerView
    private val isStaggeredGrid: Boolean
        inline get() = recyclerView?.layoutManager is StaggeredGridLayoutManager

    init {
        adapter.addListChangedListener {
            previousEmpty = !adapter.hasDisplayItem
        }
        adapter.addAdapterAttachCallback(this)
        adapter.registerAdapterDataObserver(this)
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (positionStart == 0 && helper.isFirstItemCompletelyVisible) {
            // 即使Adapter是ConcatAdapter的元素，也不会影响该判断逻辑
            recyclerView?.scrollToPosition(0)
        }
        if (isStaggeredGrid) {
            postInvalidateItemDecorations()
        } else if (positionStart == adapter.itemCount - itemCount) {
            invalidateItemDecorations()
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if ((fromPosition == 0 || toPosition == 0) && helper.isFirstItemCompletelyVisible) {
            // 即使Adapter是ConcatAdapter的元素，也不会影响该判断逻辑
            recyclerView?.scrollToPosition(0)
        }
        if (isStaggeredGrid) {
            postInvalidateItemDecorations()
        }
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (isStaggeredGrid) {
            postInvalidateItemDecorations()
        }
    }

    private fun invalidateItemDecorations() {
        val rv = recyclerView ?: return
        if (previousEmpty || rv.itemDecorationCount < 1) {
            return
        }
        rv.invalidateItemDecorations()
    }

    private fun postInvalidateItemDecorations() {
        val rv = recyclerView ?: return
        if (previousEmpty || rv.itemDecorationCount < 1) {
            return
        }
        recyclerView?.doOnFrameComplete {
            rv.invalidateItemDecorations()
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        helper.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        helper.recyclerView = null
    }
}