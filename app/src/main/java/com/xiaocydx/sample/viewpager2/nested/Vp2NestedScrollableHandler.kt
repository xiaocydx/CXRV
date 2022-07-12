package com.xiaocydx.sample.viewpager2.nested

import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * 处理[ViewPager2]嵌套[RecyclerView]等滚动控件的滚动冲突
 *
 * 1. 若此类用于容器方案，则需要传入[host]，例如[Vp2NestedScrollableHost]。
 * 2. 此类不支持处理多指的滚动冲突，这会增加代码的复杂度，
 * 而且调用场景也不需要处理多指的滚动冲突，因为实用性较低。
 *
 * @author xcc
 * @date 2022/7/8
 */
class Vp2NestedScrollableHandler constructor() {
    @ViewPager2.Orientation
    private var orientation = ORIENTATION_HORIZONTAL
    private var touchSlop = 0
    private var initialTouchX = 0
    private var initialTouchY = 0
    private var host: ViewGroup? = null

    /**
     * 是否已处理滚动冲突
     *
     * 若[handleInterceptTouchEvent]在[RecyclerView.OnItemTouchListener.onInterceptTouchEvent]下调用，
     * 则[handleInterceptTouchEvent]在[RecyclerView]开始滚动之后仍然会被调用，
     * 因此添加[nestedScrollableHandled]，用于避免冗余的滚动冲突处理。
     */
    private var nestedScrollableHandled = false

    constructor(host: ViewGroup) : this() {
        this.host = host
    }

    /**
     * [ViewPager2]的`touchSlop`是[ViewConfiguration.getScaledPagingTouchSlop]，
     * 该函数的实现逻辑是假设[ViewPager2]的`touchSlop`大于等于[child]的`touchSlop`，
     * 例如[RecyclerView]的默认`touchSlop`是[ViewConfiguration.getScaledTouchSlop]，
     * `pagingTouchSlop = 2 * scaledTouchSlop`。
     */
    fun handleInterceptTouchEvent(child: View, e: MotionEvent) {
        if (nestedScrollableHandled && e.action != MotionEvent.ACTION_DOWN) {
            return
        }
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                nestedScrollableHandled = false
                if (!ensureOrientation(child) || !ensureTouchSlop(child)) {
                    nestedScrollableHandled = true
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
                val isVp2Horizontal = orientation == ORIENTATION_HORIZONTAL
                val xDiff = if (isVp2Horizontal) dx.absoluteValue else 0
                val yDiff = if (isVp2Horizontal) 0 else dy.absoluteValue
                if (xDiff > touchSlop || yDiff > touchSlop) {
                    val disallowIntercept = if (isVp2Horizontal) {
                        if (child.canScrollVertically()) {
                            2 * dy.absoluteValue > dx.absoluteValue
                        } else {
                            // 若child不能和ViewPager2平行滚动，则允许ViewPager2拦截触摸事件
                            child.canScrollHorizontally(dx.toDirection())
                        }
                    } else {
                        if (child.canScrollHorizontally()) {
                            2 * dx.absoluteValue > dy.absoluteValue
                        } else {
                            // 若child不能和ViewPager2平行滚动，则允许ViewPager2拦截触摸事件
                            child.canScrollVertically(dy.toDirection())
                        }
                    }
                    child.requestDisallowInterceptTouchEvent(disallowIntercept)
                    nestedScrollableHandled = true
                }
            }
        }
    }

    private fun ensureOrientation(child: View): Boolean {
        var parent: View? = child.parent as? View
        while (parent != null && parent !is ViewPager2) {
            parent = parent.parent as? View
        }
        val viewPager2 = parent as? ViewPager2 ?: return false
        orientation = viewPager2.orientation
        return true
    }

    private fun ensureTouchSlop(child: View): Boolean {
        if (touchSlop <= 0) {
            touchSlop = ViewConfiguration.get(child.context).scaledTouchSlop
        }
        return touchSlop > 0
    }

    private fun Int.toDirection(): Int {
        return -this.sign
    }

    private fun Float.toRoundPx(): Int {
        return (this + 0.5f).toInt()
    }

    private fun View.canScrollHorizontally(): Boolean {
        return canScrollHorizontally(-1) || canScrollHorizontally(1)
    }

    private fun View.canScrollVertically(): Boolean {
        return canScrollVertically(-1) || canScrollVertically(1)
    }

    private fun View.requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        var parent: ViewParent? = parent
        if (parent == host) parent = host?.parent
        parent?.requestDisallowInterceptTouchEvent(disallowIntercept)
    }
}