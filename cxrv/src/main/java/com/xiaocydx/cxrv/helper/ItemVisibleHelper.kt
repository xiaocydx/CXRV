package com.xiaocydx.cxrv.helper

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.extension.findFirstCompletelyVisibleItemPosition
import com.xiaocydx.cxrv.extension.findFirstVisibleItemPosition
import com.xiaocydx.cxrv.extension.findLastCompletelyVisibleItemPosition
import com.xiaocydx.cxrv.extension.findLastVisibleItemPosition

/**
 * item可视帮助类
 *
 * 1. 仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
 * 2. 适用于监听RecyclerView滚动，频繁判断item是否可视、获取可视item位置的场景，
 * 可减少判断[StaggeredGridLayoutManager]的item是否可视的position数组创建。
 *
 * @author xcc
 * @date 2021/10/2
 */
class ItemVisibleHelper(var recyclerView: RecyclerView? = null) {
    private var spanPositions: IntArray? = null
    private val layoutManager: LayoutManager?
        get() = recyclerView?.layoutManager

    /**
     * 第一个item是否可视
     */
    val isFirstItemVisible: Boolean
        get() = when (layoutManager) {
            null -> false
            else -> firstVisibleItemPosition == 0
        }

    /**
     * 第一个item是否完全可视
     */
    val isFirstItemCompletelyVisible: Boolean
        get() = when (layoutManager) {
            null -> false
            else -> firstCompletelyVisibleItemPosition == 0
        }

    /**
     * 最后一个item是否可视
     */
    val isLastItemVisible: Boolean
        get() = when (val lm: LayoutManager? = layoutManager) {
            null -> false
            else -> lastVisibleItemPosition == lm.itemCount - 1
        }

    /**
     * 最后一个item是否完全可视
     */
    val isLastItemCompletelyVisible: Boolean
        get() = when (val lm: LayoutManager? = layoutManager) {
            null -> false
            else -> lastCompletelyVisibleItemPosition == lm.itemCount - 1
        }

    /**
     * 第一个可视item的position
     */
    val firstVisibleItemPosition: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findFirstVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findFirstVisibleItemPosition(getInto(lm))
            else -> RecyclerView.NO_POSITION
        }

    /**
     * 第一个完全可视item的position
     */
    val firstCompletelyVisibleItemPosition: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findFirstCompletelyVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findFirstCompletelyVisibleItemPosition(getInto(lm))
            else -> RecyclerView.NO_POSITION
        }

    /**
     * 最后一个可视item的position
     */
    val lastVisibleItemPosition: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findLastVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findLastVisibleItemPosition(getInto(lm))
            else -> RecyclerView.NO_POSITION
        }

    /**
     * 最后一个完全可视item的position
     */
    val lastCompletelyVisibleItemPosition: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            is LinearLayoutManager -> lm.findLastCompletelyVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findLastCompletelyVisibleItemPosition(getInto(lm))
            else -> RecyclerView.NO_POSITION
        }

    private fun getInto(lm: StaggeredGridLayoutManager): IntArray {
        if (spanPositions == null || spanPositions!!.size != lm.spanCount) {
            spanPositions = IntArray(lm.spanCount)
        }
        return spanPositions!!
    }
}