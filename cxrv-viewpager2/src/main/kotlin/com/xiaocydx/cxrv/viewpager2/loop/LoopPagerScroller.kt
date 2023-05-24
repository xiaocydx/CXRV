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
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.UpdateScenes.*
import androidx.recyclerview.widget.recyclerView
import androidx.recyclerview.widget.smoothScroller
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import androidx.viewpager2.widget.prepareSmoothScrollToPosition
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.DEFAULT_EXTRA_PAGE_LIMIT
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.PADDING_EXTRA_PAGE_LIMIT
import kotlin.math.absoluteValue

/**
 * [ViewPager2]循环页面的滚动器，负责更新锚点信息和提供滚动函数
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerScroller(
    private val content: LoopPagerContent,
    updater: LoopAnchorUpdater = LoopAnchorUpdaterImpl()
) : LoopAnchorUpdater by updater {
    private val callback = PageChangeCallbackImpl()
    private val viewPager2: ViewPager2
        get() = content.viewPager2

    init {
        viewPager2.registerOnPageChangeCallback(callback)
    }

    fun removeCallbacks() {
        removeUpdateAnchorInfoPending()
        viewPager2.unregisterOnPageChangeCallback(callback)
    }

    /**
     * [ViewPager2]非平滑滚动至[layoutPosition]
     */
    fun scrollToPosition(layoutPosition: Int) {
        if (layoutPosition == NO_POSITION || viewPager2.currentItem == layoutPosition) return
        updateAnchorInfo(Scroll, content)
        viewPager2.setCurrentItem(layoutPosition, false)
    }

    /**
     * [ViewPager2]平滑滚动至[layoutPosition]
     *
     * @param direction [layoutPosition]相应`item`的查找方向，以[ViewPager2]水平布局方向为例，
     * [LookupDirection.END]往右查找[layoutPosition]相应的`item`，并往右平滑滚动至[layoutPosition]。
     * @param provider  [SmoothScroller]的提供者，可用于修改平滑滚动的时长和插值器。
     */
    fun smoothScrollToPosition(
        layoutPosition: Int,
        direction: LookupDirection = LookupDirection.END,
        provider: SmoothScrollerProvider? = null
    ) {
        if (layoutPosition == NO_POSITION || viewPager2.currentItem == layoutPosition) return
        val current = viewPager2.currentItem
        val contentCount = content.itemCount
        val headerLast = content.lastExtraLayoutPosition(isHeader = true)
        val footerFirst = content.firstExtraLayoutPosition(isHeader = false)
        when (direction) {
            // 往开始方向查找
            LookupDirection.START -> when {
                current == headerLast -> {
                    updateAnchorInfo(SmoothScroll, content)
                    val pending = viewPager2.currentItem
                    smoothScrollToPositionInternal(layoutPosition, provider, pending)
                }
                layoutPosition in headerLast until current -> {
                    // layoutPosition在开始方向范围内，直接平滑滚动
                    smoothScrollToPositionInternal(layoutPosition, provider)
                }
                layoutPosition - contentCount == headerLast -> {
                    // layoutPosition - contentCount为附加页面，直接平滑滚动
                    smoothScrollToPositionInternal(layoutPosition - contentCount, provider)
                }
                else -> {
                    val pending = (layoutPosition + NEARBY_COUNT).coerceAtMost(footerFirst)
                    smoothScrollToPositionInternal(layoutPosition, provider, pending)
                }
            }
            // 往结束方向查找
            LookupDirection.END -> when {
                current == footerFirst -> {
                    updateAnchorInfo(SmoothScroll, content)
                    val pending = viewPager2.currentItem
                    smoothScrollToPositionInternal(layoutPosition, provider, pending)
                }
                layoutPosition in (current + 1) until footerFirst -> {
                    // layoutPosition在结束方向范围内，直接平滑滚动
                    smoothScrollToPositionInternal(layoutPosition, provider)
                }
                layoutPosition + contentCount == footerFirst -> {
                    // layoutPosition + contentCount为附加页面，直接平滑滚动
                    smoothScrollToPositionInternal(layoutPosition + contentCount, provider)
                }
                else -> {
                    val pending = (layoutPosition - NEARBY_COUNT).coerceAtLeast(headerLast)
                    smoothScrollToPositionInternal(layoutPosition, provider, pending)
                }
            }
        }
    }

    private fun smoothScrollToPositionInternal(
        current: Int,
        provider: SmoothScrollerProvider?,
        pending: Int = NO_POSITION
    ) {
        val recyclerView = viewPager2.recyclerView
        val previous = viewPager2.prepareSmoothScrollToPosition(current)
        var temp = pending
        if (temp == NO_POSITION && (current - previous).absoluteValue > NEARBY_COUNT) {
            temp = if (current > previous) current - NEARBY_COUNT else current + NEARBY_COUNT
        }
        if (temp != NO_POSITION) {
            recyclerView.scrollToPosition(temp)
            viewPager2.post {
                // 不考虑移除post的Runnable，因为滚动状态不好处理
                recyclerView.smoothScrollToPosition(current)
                provider?.let(::replaceSmoothScroller)
            }
        } else {
            recyclerView.smoothScrollToPosition(current)
            provider?.let(::replaceSmoothScroller)
        }
    }

    private fun replaceSmoothScroller(provider: SmoothScrollerProvider) {
        val recyclerView = viewPager2.recyclerView
        val smoothScroller = recyclerView.layoutManager?.smoothScroller ?: return
        val newSmoothScroller = provider.create(recyclerView.context)
        newSmoothScroller.targetPosition = smoothScroller.targetPosition
        recyclerView.layoutManager?.startSmoothScroll(newSmoothScroller)
    }

    private inner class PageChangeCallbackImpl : OnPageChangeCallback() {

        /**
         * 当开始手势拖动时，更新锚点信息，跟修复多指交替滚动不会同时进行
         */
        override fun onPageScrollStateChanged(state: Int) {
            if (state == SCROLL_STATE_DRAGGING) updateAnchorInfo(Dragging, content)
        }

        /**
         * 修复多指交替滚动未更新锚点信息的问题，不设置`viewPager2.currentItem`
         */
        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            // 判断viewPager2.currentItem避免跟scrollToPosition()和smoothScrollToPosition()产生冲突
            if (!content.supportLoop() || position == viewPager2.currentItem) return
            val headerFirst = content.firstExtraLayoutPosition(isHeader = true)
            val headerLast = content.lastExtraLayoutPosition(isHeader = true)
            val footerFirst = content.firstExtraLayoutPosition(isHeader = false)
            val currentPosition = when (content.extraPageLimit) {
                DEFAULT_EXTRA_PAGE_LIMIT -> when {
                    // 完全偏移附加页面headerFirst，根据headerFirst更新锚点信息
                    position == headerLast && positionOffset == 0f -> headerLast
                    // 完全偏移附加页面footerFirst，根据footerFirst更新锚点信息
                    position == footerFirst && positionOffset == 0f -> footerFirst
                    else -> return
                }
                PADDING_EXTRA_PAGE_LIMIT -> when {
                    // 开始偏移附加页面headerFirst，根据headerLast更新锚点信息
                    position == headerFirst && positionOffset > 0f -> headerLast
                    // 开始偏移附加页面footerLast，根据footerFirst更新锚点信息
                    position == footerFirst && positionOffset > 0f -> footerFirst
                    else -> return
                }
                else -> return
            }
            updateAnchorInfo(ScrolledFix(currentPosition), content)
        }
    }

    private companion object {
        const val NEARBY_COUNT = 3
    }
}