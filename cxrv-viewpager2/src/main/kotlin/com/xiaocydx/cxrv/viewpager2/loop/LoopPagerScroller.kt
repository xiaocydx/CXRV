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

@file:JvmName("LoopPagerScrollerInternalKt")
@file:Suppress("PackageDirectoryMismatch", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.recyclerview.widget

import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import androidx.viewpager2.widget.setCurrentItemDirect
import com.xiaocydx.cxrv.viewpager2.loop.LookupDirection
import com.xiaocydx.cxrv.viewpager2.loop.LoopAnchorUpdater
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.PADDING_EXTRA_PAGE_LIMIT

/**
 * [ViewPager2]循环页面的滚动器，负责更新锚点信息和提供滚动函数
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerScroller(
    private val content: LoopPagerContent
) : OnPageChangeCallback(), LoopAnchorUpdater {
    private val optimization = AnchorOptimization(content)
    private var runnable: SmoothScrollRunnable? = null
    private val viewPager2: ViewPager2
        get() = content.viewPager2

    init {
        viewPager2.registerOnPageChangeCallback(this)
    }

    fun removeCallback() {
        removeRunnable()
        viewPager2.unregisterOnPageChangeCallback(this)
    }

    private fun removeRunnable() {
        runnable?.let(viewPager2::removeCallbacks)
        runnable = null
    }

    override fun onPageScrollStateChanged(state: Int) {
        if (state == SCROLL_STATE_DRAGGING) updateAnchorForCurrent()
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
        val contentCount = content.currentCount
        val layoutFirst = content.firstLayoutPosition()
        val layoutLast = content.lastLayoutPosition()
        if (direction === LookupDirection.END) {
            // 往结束方向查找
            if (viewPager2.currentItem < layoutPosition) {
                // layoutPosition在结束方向范围内，直接平滑滚动
                viewPager2.currentItem = layoutPosition
            } else if (layoutPosition + contentCount in layoutFirst..layoutLast) {
                // layoutPosition + contentCount（附加页面）在结束方向范围内，直接平滑滚动
                viewPager2.currentItem = layoutPosition + contentCount
            } else {
                // layoutPosition在开始方向范围内，先非平滑滚动至layoutPosition - 3，
                // 同步屏障被移除后，再从layoutPosition - 3平滑滚动至layoutPosition。
                scrollToPosition((layoutPosition - 3).coerceAtLeast(layoutFirst))
                viewPager2.post(SmoothScrollRunnable(layoutPosition).also { runnable = it })
            }
        } else {
            // 往开始方向查找
            if (layoutPosition < viewPager2.currentItem) {
                // layoutPosition在开始方向范围内，直接平滑滚动
                viewPager2.currentItem = layoutPosition
            } else if (layoutPosition - contentCount in layoutFirst..layoutLast) {
                // layoutPosition - contentCount（附加页面）在开始方向范围内，直接平滑滚动
                viewPager2.currentItem = layoutPosition - contentCount
            } else {
                // layoutPosition在结束方向范围内，先非平滑滚动至layoutPosition + 3，
                // 同步屏障被移除后，再从layoutPosition + 3平滑滚动至layoutPosition。
                scrollToPosition((layoutPosition + 3).coerceAtMost(layoutLast))
                viewPager2.post(SmoothScrollRunnable(layoutPosition).also { runnable = it })
            }
        }
    }

    override fun updateAnchorForCurrent() {
        if (!content.supportLoop()) return
        val headerFirst = content.firstExtraLayoutPosition(isHeader = true)
        val headerLast = content.lastExtraLayoutPosition(isHeader = true)
        val footerFirst = content.firstExtraLayoutPosition(isHeader = false)
        val footerLast = content.lastExtraLayoutPosition(isHeader = false)
        val anchorPosition = when (val currentItem = viewPager2.currentItem) {
            in headerFirst..headerLast -> currentItem + content.currentCount
            in footerFirst..footerLast -> currentItem - content.currentCount
            else -> return
        }
        optimization.setNewAnchorPosition(anchorPosition)
    }

    override fun updateAnchorForInserted(lastContentCount: Int, positionStart: Int, itemCount: Int) {
        if (!content.supportLoop(lastContentCount)) return
        val headerLast = content.lastExtraLayoutPosition(isHeader = true, lastContentCount)
        val footerFirst = content.firstExtraLayoutPosition(isHeader = false, lastContentCount)
        val bindingFirst = content.firstBindingAdapterPosition(lastContentCount)
        val bindingLast = content.lastBindingAdapterPosition(lastContentCount)
        val currentItem = viewPager2.currentItem
        val anchorPosition = when {
            currentItem == headerLast && positionStart == bindingLast + 1 -> {
                headerLast + lastContentCount
            }
            currentItem == footerFirst && positionStart == bindingFirst -> {
                footerFirst - lastContentCount + itemCount
            }
            else -> return
        }
        viewPager2.setCurrentItem(anchorPosition, false)
    }

    private inner class SmoothScrollRunnable(private val layoutPosition: Int) : Runnable {
        override fun run() {
            viewPager2.currentItem = layoutPosition
            runnable = null
        }
    }
}

/**
 * ### 优化初衷
 * 当滚动到开始端和结束端的附加页面时，再次触发滚动，会更新锚点信息，
 * 更新锚点信息若通过非平滑滚动实现，则会导致可见的`itemView`被移除，
 * [RecyclerView]按更新后的锚点信息进行布局，填充新的[ViewHolder]，
 * 对图片内容而言，通常有图片缓存，因此更新锚点信息产生的影响较小，
 * 但对视频内容而言，可见的`itemView`被移除、绑定新的[ViewHolder]，
 * 产生的影响较大。
 *
 * ### 优化方案
 * 1. [updateAnchorInfoInNextLayout]查找目标`itemView`，处理跟离屏页面和离屏缓存的冲突。
 * 2. [updateAnchorInfoByScrollToPosition]处理[ViewPager2.setCurrentItem]的滚动状态。
 */
internal class AnchorOptimization(private val content: LoopPagerContent) {
    private val targetScrapStore = targetScrapStoreProvider()

    fun setNewAnchorPosition(anchorPosition: Int) {
        val viewPager2 = content.viewPager2
        val canOptimize = !CHECK_SCROLL_STATE || viewPager2.scrollState == SCROLL_STATE_DRAGGING
        val hasPendingAdapterUpdates = viewPager2.recyclerView.hasPendingAdapterUpdates()
        when {
            !canOptimize -> viewPager2.setCurrentItem(anchorPosition, false)
            hasPendingAdapterUpdates -> updateAnchorInfoByScrollToPosition(anchorPosition)
            else -> updateAnchorInfoInNextLayout(anchorPosition)
        }
        clear()
    }

    /**
     * 当[RecyclerView.onInterceptTouchEvent]将滚动状态设为[SCROLL_STATE_DRAGGING]时，
     * 会对[OnScrollListener]分发新的滚动状态，若此时调用[ViewPager2.setCurrentItem]，
     * 则会调用至[RecyclerView.stopScroll]，将滚动状态设为[SCROLL_STATE_IDLE]，
     * 导致[RecyclerView.onInterceptTouchEvent]返回`false`，在实际场景中，
     * 手势滚动可能会因为这次触摸事件拦截失败，而出现交互不流畅的问题。
     */
    private fun updateAnchorInfoByScrollToPosition(anchorPosition: Int) {
        content.viewPager2.setCurrentItemDirect(anchorPosition)
        content.viewPager2.recyclerView.layoutManager?.scrollToPosition(anchorPosition)
    }

    /**
     * [RecyclerView]的布局流程会调用[Recycler.tryGetViewHolderForPositionByDeadline]填充`itemView`，
     * 该函数确保修改当前`targetScrap`的`layoutPosition`后，下一次布局基于新锚点填充当前`targetScrap`。
     */
    private fun updateAnchorInfoInNextLayout(anchorPosition: Int) {
        val recyclerView = content.viewPager2.recyclerView
        val recycler = recyclerView.mRecycler ?: return
        val cachedViews = recycler.mCachedViews ?: return

        // 查找当前targetScrap，并基于新锚点设置layoutPosition
        val current = content.viewPager2.currentItem
        val offset = anchorPosition - current
        content.viewPager2.setCurrentItemDirect(anchorPosition)
        recyclerView.addTargetScrapForLayoutPosition(current, offset)
        if (content.extraPageLimit == PADDING_EXTRA_PAGE_LIMIT) {
            recyclerView.addTargetScrapForLayoutPosition(current - 1, offset)
            recyclerView.addTargetScrapForLayoutPosition(current + 1, offset)
        }

        // 对有冲突的离屏页面设置targetScrap的oldPosition，避免下一次布局基于新锚点填充离屏页面
        for (index in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildAt(index).let(recyclerView::getChildViewHolder)
            val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
            val scrap = targetScrapStore[bindingAdapterPosition]
            if (scrap == null || scrap === holder) continue
            holder.offsetPosition(scrap.oldPosition - holder.layoutPosition, false)
        }

        // 对有冲突的离屏缓存设置targetScrap的oldPosition，避免下一次布局基于新锚点填充离屏缓存
        for (index in cachedViews.indices) {
            val holder = cachedViews[index]
            val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
            val scrap = targetScrapStore[bindingAdapterPosition]
            if (scrap == null || scrap === holder) continue
            holder.offsetPosition(scrap.oldPosition - holder.layoutPosition, false)
        }

        // 下一次布局LinearLayoutManager会自行计算出当前targetScrap的锚点信息，
        // RecyclerView.dispatchLayoutStep3()回收上述已处理但未填充的离屏页面。
        recyclerView.requestLayout()
    }

    private fun RecyclerView.addTargetScrapForLayoutPosition(layoutPosition: Int, offset: Int) {
        val holder = findViewHolderForLayoutPosition(layoutPosition) ?: return
        val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
        holder.offsetPosition(offset, false)
        targetScrapStore[bindingAdapterPosition] = holder
    }

    private fun clear() {
        if (targetScrapStore.size > 0) targetScrapStore.clear()
    }

    companion object {
        @set:VisibleForTesting
        internal var CHECK_SCROLL_STATE = true

        @set:VisibleForTesting
        internal var targetScrapStoreProvider: () -> TargetScrapStore = { DefaultTargetScrapStore() }
    }
}

/**
 * 使用[SparseArray]会让单元测试报错（未找到原因），因此用接口进行兼容
 */
internal interface TargetScrapStore {
    val size: Int
    operator fun get(bindingAdapterPosition: Int): ViewHolder?
    operator fun set(bindingAdapterPosition: Int, holder: ViewHolder)
    fun valueAt(index: Int): ViewHolder
    fun clear()
}

private class DefaultTargetScrapStore : TargetScrapStore {
    private val sparseArray = SparseArray<ViewHolder>()

    override val size: Int
        get() = sparseArray.size()

    override fun get(bindingAdapterPosition: Int): ViewHolder? {
        return sparseArray[bindingAdapterPosition]
    }

    override fun set(bindingAdapterPosition: Int, holder: ViewHolder) {
        sparseArray[bindingAdapterPosition] = holder
    }

    override fun valueAt(index: Int): ViewHolder {
        return sparseArray.valueAt(index)
    }

    override fun clear() = sparseArray.clear()
}