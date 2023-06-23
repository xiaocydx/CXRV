package com.xiaocydx.cxrv.viewpager2.loop

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import kotlin.math.absoluteValue

/**
 * 处理[ViewPager2]嵌套[ViewPager2]（LoopPager）的滚动冲突
 *
 * @author xcc
 * @date 2023/6/23
 */
class LoopPagerScrollableListener : RecyclerView.OnItemTouchListener {
    private val handler = LoopPagerScrollableHandler()

    /**
     * 调用时机
     * 1. [RecyclerView.onInterceptTouchEvent]。
     * 2. [RecyclerView.onTouchEvent]。
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        handler.handleInterceptTouchEvent(rv, e)
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
}

private class LoopPagerScrollableHandler {
    @ViewPager2.Orientation
    private var vp2Orientation = ORIENTATION_HORIZONTAL
    private var childTouchSlop = 0
    private var initialTouchX = 0
    private var initialTouchY = 0
    private var isNestedScrollableHandled = false

    fun handleInterceptTouchEvent(child: RecyclerView, e: MotionEvent) {
        if (isNestedScrollableHandled && e.action != MotionEvent.ACTION_DOWN) return
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                isNestedScrollableHandled = false
                if (!ensureVp2Orientation(child) || !ensureChildTouchSlop(child)) {
                    isNestedScrollableHandled = true
                    return
                }
                initialTouchX = e.x.toRoundPx()
                initialTouchY = e.y.toRoundPx()
                // 不允许ViewPager2拦截触摸事件，接下来才有处理滚动冲突的机会
                child.requestDisallowInterceptTouchEvent(disallowIntercept = true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x.toRoundPx() - initialTouchX
                val dy = e.y.toRoundPx() - initialTouchY
                val isVp2Horizontal = vp2Orientation == ORIENTATION_HORIZONTAL
                val xDiff = if (isVp2Horizontal) dx.absoluteValue else 0
                val yDiff = if (isVp2Horizontal) 0 else dy.absoluteValue
                if (xDiff > childTouchSlop || yDiff > childTouchSlop) {
                    val disallowIntercept = if (isVp2Horizontal) {
                        if (child.canScrollHorizontally()) {
                            // child能和ViewPager2平行滚动，不允许ViewPager2拦截触摸事件
                            true
                        } else {
                            2 * dy.absoluteValue > dx.absoluteValue
                        }
                    } else {
                        if (child.canScrollVertically()) {
                            // child能和ViewPager2平行滚动，不允许ViewPager2拦截触摸事件
                            true
                        } else {
                            2 * dx.absoluteValue > dy.absoluteValue
                        }
                    }
                    child.requestDisallowInterceptTouchEvent(disallowIntercept)
                    isNestedScrollableHandled = true
                }
            }
        }
    }

    private fun ensureVp2Orientation(child: View): Boolean {
        val viewPager2 = child.findParentViewPager2() ?: return false
        vp2Orientation = viewPager2.orientation
        return true
    }

    private fun ensureChildTouchSlop(child: View): Boolean {
        if (childTouchSlop <= 0) {
            childTouchSlop = ViewConfiguration.get(child.context).scaledTouchSlop
        }
        return childTouchSlop > 0
    }

    private fun Float.toRoundPx() = (this + 0.5f).toInt()

    private fun RecyclerView.canScrollHorizontally(): Boolean {
        return layoutManager?.canScrollHorizontally() ?: false
    }

    private fun RecyclerView.canScrollVertically(): Boolean {
        return layoutManager?.canScrollVertically() ?: false
    }

    /**
     * 从[ViewPager2]开始，向上递归不允许拦截，不处理当前View到[ViewPager2]之间的滚动冲突，
     * 这能确保内部滚动正常，例如[ViewPager2]嵌套[RecyclerView]再嵌套[RecyclerView]的场景。
     */
    private fun View.requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        val vp2Rv = findParentViewPager2()?.getChildAt(0) as? RecyclerView
        vp2Rv?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }

    private fun View.findParentViewPager2(): ViewPager2? {
        var parent: View? = parent as? View
        // parent是ViewPager2，表示当前View是ViewPager2.mRecyclerView
        if (parent is ViewPager2) parent = parent.parent as? View
        while (parent != null && parent !is ViewPager2) {
            parent = parent.parent as? View
        }
        return parent as? ViewPager2
    }
}