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
import android.view.ViewParent
import android.widget.HorizontalScrollView
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import com.xiaocydx.cxrv.viewpager2.R
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
class Vp2NestedScrollableHandler(
    private val childOrientation: (child: View) -> Int = defaultChildOrientation,
    private val canScrollHorizontally: (child: View, direction: Int) -> Boolean = defaultCanScrollHorizontally,
    private val canScrollVertically: (child: View, direction: Int) -> Boolean = defaultCanScrollVertically
) {
    @ViewPager2.Orientation
    private var vp2Orientation = ORIENTATION_HORIZONTAL
    private var childTouchSlop = 0
    private var initialTouchX = 0
    private var initialTouchY = 0
    private var isNestedScrollableHandled = false

    /**
     * 处理滚动处理的容器需要实现此接口
     */
    interface Host : ViewParent

    /**
     * [ViewPager2]的`touchSlop`是[ViewConfiguration.getScaledPagingTouchSlop]，
     * 该函数的实现逻辑是假设[ViewPager2]的`touchSlop`大于等于[child]的`touchSlop`，
     * 例如[RecyclerView]的默认`touchSlop`是[ViewConfiguration.getScaledTouchSlop]，
     * `scaledPagingTouchSlop = 2 * scaledTouchSlop`。
     */
    fun handleInterceptTouchEvent(child: View, e: MotionEvent) {
        if (isNestedScrollableHandled && e.action != MotionEvent.ACTION_DOWN) return
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                isNestedScrollableHandled = false
                child.resetRequestDisallowIntercept()
                if (!ensureVp2Orientation(child) || !ensureChildTouchSlop(child)
                        || childOrientation(child) == ORIENTATION_UNKNOWN) {
                    isNestedScrollableHandled = true
                    return
                }
                initialTouchX = e.x.toRoundPx()
                initialTouchY = e.y.toRoundPx()
                child.requestDisallowInterceptTouchEvent(disallowIntercept = true)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = e.x.toRoundPx() - initialTouchX
                val dy = e.y.toRoundPx() - initialTouchY
                val isVp2Horizontal = vp2Orientation == ORIENTATION_HORIZONTAL
                val isChildHorizontal = childOrientation(child) == ORIENTATION_HORIZONTAL
                val valueX = dx.absoluteValue
                val valueY = dy.absoluteValue
                if (valueX > childTouchSlop || valueY > childTouchSlop) {
                    val disallowIntercept = if (isVp2Horizontal) {
                        if (canScrollHorizontally(child, dx.toDirection())) {
                            valueX > 2 * valueY
                        } else {
                            !isChildHorizontal && 2 * valueY > valueX
                        }
                    } else {
                        if (canScrollVertically(child, dy.toDirection())) {
                            valueY > 2 * valueX
                        } else {
                            isChildHorizontal && 2 * valueX > valueY
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

    private var View.requestDisallowIntercept: Boolean?
        get() = getTag(R.id.tag_vp2_nested_request_disallowIntercept) as? Boolean
        set(value) {
            setTag(R.id.tag_vp2_nested_request_disallowIntercept, value)
        }

    private fun View.resetRequestDisallowIntercept() {
        this.requestDisallowIntercept = false
    }

    private fun View.requestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        this.requestDisallowIntercept = disallowIntercept
        val notHostParent = if (parent is Host) parent?.parent else parent
        notHostParent?.requestDisallowInterceptTouchEvent(disallowIntercept)
        if (!disallowIntercept) {
            var parent: View? = notHostParent as? View
            while (parent != null && parent.requestDisallowIntercept == null) {
                parent = parent.parent as? View
            }
            val previousDisallowIntercept = parent?.requestDisallowIntercept
            if (previousDisallowIntercept == true) {
                parent?.requestDisallowInterceptTouchEvent(previousDisallowIntercept)
            }
        }
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

    internal companion object {
        private const val ORIENTATION_UNKNOWN = -1
        val defaultCanScrollHorizontally = { child: View, direction: Int ->
            child.canScrollHorizontally(direction)
        }
        val defaultCanScrollVertically = { child: View, direction: Int ->
            child.canScrollVertically(direction)
        }
        val defaultChildOrientation = { child: View ->
            when (child) {
                is RecyclerView -> when (val lm = child.layoutManager) {
                    is LinearLayoutManager -> lm.orientation
                    is StaggeredGridLayoutManager -> lm.orientation
                    else -> ORIENTATION_UNKNOWN
                }
                is ViewPager2 -> child.orientation
                is ScrollView -> ORIENTATION_VERTICAL
                is HorizontalScrollView -> ORIENTATION_HORIZONTAL
                is NestedScrollView -> ORIENTATION_VERTICAL
                else -> ORIENTATION_UNKNOWN
            }
        }
    }
}