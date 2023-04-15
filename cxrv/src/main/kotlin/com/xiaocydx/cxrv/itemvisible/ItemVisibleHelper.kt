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

package com.xiaocydx.cxrv.itemvisible

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.layout.runExtensionsPrimitive

/**
 * item可视帮助类
 *
 * 1. 默认仅支持[LinearLayoutManager]、[StaggeredGridLayoutManager]。
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
            null -> NO_POSITION
            is LinearLayoutManager -> lm.findFirstVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findFirstVisibleItemPosition(getInto(lm))
            else -> lm.runExtensionsPrimitive(NO_POSITION) { findFirstVisibleItemPosition(lm) }
        }

    /**
     * 第一个完全可视item的position
     */
    val firstCompletelyVisibleItemPosition: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            null -> NO_POSITION
            is LinearLayoutManager -> lm.findFirstCompletelyVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findFirstCompletelyVisibleItemPosition(getInto(lm))
            else -> lm.runExtensionsPrimitive(NO_POSITION) { findFirstCompletelyVisibleItemPosition(lm) }
        }

    /**
     * 最后一个可视item的position
     */
    val lastVisibleItemPosition: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            null -> NO_POSITION
            is LinearLayoutManager -> lm.findLastVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findLastVisibleItemPosition(getInto(lm))
            else -> lm.runExtensionsPrimitive(NO_POSITION) { findLastVisibleItemPosition(lm) }
        }

    /**
     * 最后一个完全可视item的position
     */
    val lastCompletelyVisibleItemPosition: Int
        get() = when (val lm: LayoutManager? = layoutManager) {
            null -> NO_POSITION
            is LinearLayoutManager -> lm.findLastCompletelyVisibleItemPosition()
            is StaggeredGridLayoutManager -> lm.findLastCompletelyVisibleItemPosition(getInto(lm))
            else -> lm.runExtensionsPrimitive(NO_POSITION) { findLastCompletelyVisibleItemPosition(lm) }
        }

    private fun getInto(lm: StaggeredGridLayoutManager): IntArray {
        if (spanPositions == null || spanPositions!!.size != lm.spanCount) {
            spanPositions = IntArray(lm.spanCount)
        }
        return spanPositions!!
    }
}