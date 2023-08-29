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

package com.xiaocydx.cxrv.viewpager2.loop

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.viewpager2.nested.Vp2NestedScrollableHandler

/**
 * 处理[ViewPager2]嵌套[ViewPager2]（LoopPager）的滚动冲突
 *
 * @author xcc
 * @date 2023/6/23
 */
internal class LoopPagerScrollableListener : RecyclerView.OnItemTouchListener {
    private val handler = Vp2NestedScrollableHandler(
        canScrollHorizontally = { child, _ ->
            (child as? RecyclerView)?.layoutManager?.canScrollHorizontally() ?: false
        },
        canScrollVertically = { child, _ ->
            (child as? RecyclerView)?.layoutManager?.canScrollVertically() ?: false
        }
    )

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