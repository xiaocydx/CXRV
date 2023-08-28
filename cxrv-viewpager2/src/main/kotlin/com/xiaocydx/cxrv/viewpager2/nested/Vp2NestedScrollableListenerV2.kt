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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

internal class Vp2NestedScrollableListenerV2 : RecyclerView.OnItemTouchListener {
    private val handler = Vp2NestedScrollableHandlerV2()

    /**
     * 调用时机
     * 1. [RecyclerView.onInterceptTouchEvent]。
     * 2. [RecyclerView.onTouchEvent]。
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        val orientation = when (val lm = rv.layoutManager) {
            is LinearLayoutManager -> lm.orientation
            is StaggeredGridLayoutManager -> lm.orientation
            else -> throw UnsupportedOperationException()
        }
        handler.handleInterceptTouchEvent(rv, orientation, e)
        return false
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) = Unit

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) = Unit
}