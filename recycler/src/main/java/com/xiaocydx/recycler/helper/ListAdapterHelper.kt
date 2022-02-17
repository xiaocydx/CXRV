package com.xiaocydx.recycler.helper

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.StaggeredGridLayoutManager
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
        // 即使Adapter是[ConcatAdapter]的元素，也不会影响该判断逻辑
        if (positionStart == 0
                && helper.firstCompletelyVisibleItemPosition == positionStart) {
            recyclerView?.scrollToPosition(0)
        }
        postInvalidateItemDecorations()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        postInvalidateItemDecorations()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        postInvalidateItemDecorations()
    }

    /**
     * 发送同步消息，确保瀑布流布局添加/移除/交换item后，ItemDecoration能正常显示
     */
    private fun postInvalidateItemDecorations() {
        if (!isStaggeredGrid) {
            return
        }
        recyclerView?.post { invalidateItemDecorationsInternal() }
    }

    /**
     * 瀑布流布局会在添加/移除/交换item时，使所有ItemDecoration无效
     */
    fun invalidateItemDecorations() {
        if (isStaggeredGrid) {
            return
        }
        invalidateItemDecorationsInternal()
    }

    private fun invalidateItemDecorationsInternal() {
        val rv = recyclerView ?: return
        if (previousEmpty || rv.itemDecorationCount < 1) {
            return
        }
        rv.invalidateItemDecorations()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        helper.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        helper.recyclerView = null
    }
}