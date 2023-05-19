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

import androidx.recyclerview.widget.LoopAnchorUpdater
import androidx.recyclerview.widget.LoopAnchorUpdaterImpl
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback

/**
 * [ViewPager2]循环页面的滚动器，负责更新锚点信息和提供滚动函数
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerScroller(
    private val content: LoopPagerContent
) : OnPageChangeCallback(), LoopAnchorUpdater by LoopAnchorUpdaterImpl() {
    private var runnable: SmoothScrollRunnable? = null
    private val viewPager2: ViewPager2
        get() = content.viewPager2

    init {
        viewPager2.registerOnPageChangeCallback(this)
    }

    fun removeCallback() {
        removeRunnable()
        removeUpdateAnchorInfoPending()
        viewPager2.unregisterOnPageChangeCallback(this)
    }

    private fun removeRunnable() {
        runnable?.let(viewPager2::removeCallbacks)
        runnable = null
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == SCROLL_STATE_DRAGGING) {
            updateAnchorInfo(fromNotify = false, content)
        }
    }

    /**
     * [ViewPager2]非平滑滚动至[layoutPosition]
     */
    fun scrollToPosition(layoutPosition: Int) {
        if (layoutPosition == NO_POSITION || viewPager2.currentItem == layoutPosition) return
        removeRunnable()
        viewPager2.setCurrentItem(layoutPosition, false)
    }

    /**
     * [ViewPager2]平滑滚动至[layoutPosition]
     *
     * @param direction [layoutPosition]相应`item`的查找方向，以[ViewPager2]水平布局方向为例，
     * [LookupDirection.END]往右查找[layoutPosition]相应的`item`，并往右平滑滚动至[layoutPosition]。
     */
    fun smoothScrollToPosition(layoutPosition: Int, direction: LookupDirection = LookupDirection.END) {
        if (layoutPosition == NO_POSITION || viewPager2.currentItem == layoutPosition) return
        removeRunnable()
        val current = viewPager2.currentItem
        val contentCount = content.itemCount
        val headerLayoutLast = content.lastExtraLayoutPosition(isHeader = true)
        val footerLayoutFirst = content.firstExtraLayoutPosition(isHeader = false)
        when (direction) {
            // 往开始方向查找
            LookupDirection.START -> when {
                current == headerLayoutLast -> {
                    updateAnchorInfo(fromNotify = false, content)
                    smoothScrollToPositionAfterRemoveSyncBarrier(layoutPosition)
                }
                layoutPosition in headerLayoutLast until current -> {
                    // layoutPosition在开始方向范围内，直接平滑滚动
                    viewPager2.currentItem = layoutPosition
                }
                layoutPosition - contentCount == headerLayoutLast -> {
                    // layoutPosition - contentCount为附加页面，直接平滑滚动
                    viewPager2.currentItem = layoutPosition - contentCount
                }
                else -> {
                    scrollToPosition((layoutPosition + 3).coerceAtMost(footerLayoutFirst))
                    smoothScrollToPositionAfterRemoveSyncBarrier(layoutPosition)
                }
            }
            // 往结束方向查找
            LookupDirection.END -> when {
                current == footerLayoutFirst -> {
                    updateAnchorInfo(fromNotify = false, content)
                    smoothScrollToPositionAfterRemoveSyncBarrier(layoutPosition)
                }
                layoutPosition in (current + 1) until footerLayoutFirst -> {
                    // layoutPosition在结束方向范围内，直接平滑滚动
                    viewPager2.currentItem = layoutPosition
                }
                layoutPosition + contentCount == footerLayoutFirst -> {
                    // layoutPosition + contentCount为附加页面，直接平滑滚动
                    viewPager2.currentItem = layoutPosition + contentCount
                }
                else -> {
                    scrollToPosition((layoutPosition - 3).coerceAtLeast(headerLayoutLast))
                    smoothScrollToPositionAfterRemoveSyncBarrier(layoutPosition)
                }
            }
        }
    }

    private fun smoothScrollToPositionAfterRemoveSyncBarrier(layoutPosition: Int) {
        viewPager2.post(SmoothScrollRunnable(layoutPosition).also { runnable = it })
    }

    private inner class SmoothScrollRunnable(private val layoutPosition: Int) : Runnable {
        override fun run() {
            viewPager2.currentItem = layoutPosition
            runnable = null
        }
    }
}