/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.cxrv.viewpager2.nested

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * 处理[ViewPager2]嵌套[RecyclerView]等滚动控件的滚动冲突
 *
 * *注意**：
 * 1. 此类可用于实现容器方案，例如[Vp2NestedScrollableHost]。
 * 2. 此类不支持处理多指的滚动冲突，实际场景通常不需要处理多指的滚动冲突。
 *
 * * 处理相同方向的滚动冲突，当Child无法滚动时，才允许Parent拦截触摸事件。
 * * 处理不同方向的滚动冲突，Parent拦截触摸事件的条件更严格，不会那么“灵敏”。
 *
 * @author xcc
 * @date 2022/7/8
 */
class Vp2NestedScrollableHandler {
    @ViewPager2.Orientation
    private var vp2Orientation = ORIENTATION_HORIZONTAL
    private var childTouchSlop = 0
    private var initialTouchX = 0
    private var initialTouchY = 0
    private var isNestedScrollableHandled = false

    /**
     * [ViewPager2]的`touchSlop`是[ViewConfiguration.getScaledPagingTouchSlop]，
     * 该函数的实现逻辑是假设[ViewPager2]的`touchSlop`大于等于[child]的`touchSlop`，
     * 例如[RecyclerView]的默认`touchSlop`是[ViewConfiguration.getScaledTouchSlop]，
     * `pagingTouchSlop = 2 * scaledTouchSlop`。
     */
    fun handleInterceptTouchEvent(child: View, e: MotionEvent) {
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
                        if (child.canScrollHorizontally(dx.toDirection())) {
                            // child能和ViewPager2平行滚动，不允许ViewPager2拦截触摸事件
                            true
                        } else {
                            2 * dy.absoluteValue > dx.absoluteValue
                        }
                    } else {
                        if (child.canScrollVertically(dy.toDirection())) {
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

    private fun Int.toDirection() = -this.sign

    private fun Float.toRoundPx() = (this + 0.5f).toInt()

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