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
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.ViewHolder.FLAG_RETURNED_FROM_SCRAP
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.viewpager2.R
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
        optimization.removeListener()
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
        val anchorPosition = getNewAnchorPositionForCurrent()
        if (layoutPosition == anchorPosition) {
            optimization.optimizeLayoutForCurrent(anchorPosition)
        }
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
        val anchorPosition = getNewAnchorPositionForCurrent()
        if (anchorPosition == NO_POSITION) return
        optimization.optimizeLayoutForCurrent(anchorPosition)
        viewPager2.setCurrentItem(anchorPosition, false)
    }

    private fun getNewAnchorPositionForCurrent(): Int {
        if (!content.supportLoop()) return NO_POSITION
        val headerFirst = content.firstExtraLayoutPosition(isHeader = true)
        val headerLast = content.lastExtraLayoutPosition(isHeader = true)
        val footerFirst = content.firstExtraLayoutPosition(isHeader = false)
        val footerLast = content.lastExtraLayoutPosition(isHeader = false)
        return when (val currentItem = viewPager2.currentItem) {
            in headerFirst..headerLast -> currentItem + content.currentCount
            in footerFirst..footerLast -> currentItem - content.currentCount
            else -> return NO_POSITION
        }
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

@set:VisibleForTesting
internal var ANCHOR_OPTIMIZATION_ENABLED = true

@set:VisibleForTesting
internal var tempAttachedScrapProvider: () -> TempAttachedScrap = { DefaultTempAttachedScrap() }

/**
 * ### 优化初衷
 * 当滚动到开始端和结束端的附加页面时，再次触发滚动，会更新锚点信息，
 * 更新锚点信息是通过非平滑滚动实现，这会导致可见的`itemView`被移除，
 * [RecyclerView]按更新后的锚点信息进行布局，填充新的[ViewHolder]，
 * 对图片内容而言，通常有图片缓存，因此更新锚点信息产生的影响较小，
 * 但对视频内容而言，可见的`itemView`被移除、绑定新的[ViewHolder]，
 * 产生的影响较大。
 *
 * ### 优化方案
 * 利用[ViewCacheExtension]填充目标位置的`itemView`，
 * 避免可见的`itemView`被移除、绑定新的[ViewHolder]：
 * 1. [prepareAttachedScrap]标记目标位置`itemView`。
 * 2. [removeAndRecycleViewsIfNecessary]处理离屏页面。
 * 3. [recycleCachedViewsIfNecessary]处理离屏缓存。
 */
private class AnchorOptimization(private val content: LoopPagerContent) : RecyclerListener {
    private val tempAttachedScrap = tempAttachedScrapProvider()
    private val pendingRemovals = mutableSetOf<ViewHolder>()

    init {
        content.viewPager2.recyclerView.addRecyclerListener(this)
    }

    fun removeListener() {
        content.viewPager2.recyclerView.removeRecyclerListener(this)
    }

    /**
     * [RecyclerView.dispatchLayoutStep3]会回收[Recycler.mAttachedScrap]，
     * 此时可以检查[tempAttachedScrap]是否已被填充，若未被填充，则重置属性。
     */
    override fun onViewRecycled(holder: ViewHolder) {
        if (holder.isViewCacheExtensionScrap) {
            holder.viewCacheExtensionPosition = NO_POSITION
        }
    }

    fun optimizeLayoutForCurrent(anchorPosition: Int) {
        val recyclerView = content.viewPager2.recyclerView
        if (!ANCHOR_OPTIMIZATION_ENABLED) {
            recyclerView.setViewCacheExtension(null)
            return
        }
        var extension = recyclerView.getViewCacheExtensionOrNull()
        if (extension !is GetScrapForBindingAdapterPosition) {
            extension = GetScrapForBindingAdapterPosition(content, extension)
            recyclerView.setViewCacheExtension(extension)
        }
        prepareAttachedScrap(anchorPosition)
        removeAndRecycleViewsIfNecessary()
        recycleCachedViewsIfNecessary()
        clear()
    }

    private fun prepareAttachedScrap(anchorPosition: Int) {
        val recyclerView = content.viewPager2.recyclerView
        val current = content.viewPager2.currentItem
        val offset = anchorPosition - current
        recyclerView.addAttachedScrapForLayoutPosition(current, offset)
        if (content.extraPageLimit == PADDING_EXTRA_PAGE_LIMIT) {
            recyclerView.addAttachedScrapForLayoutPosition(current - 1, offset)
            recyclerView.addAttachedScrapForLayoutPosition(current + 1, offset)
        }
    }

    /**
     * [RecyclerView]的布局流程会调用[Recycler.tryGetViewHolderForPositionByDeadline]填充`itemView`，
     * 若从`mAttachedScrap`获取到`scrap`，则不会根据`itemId`或`bindingAdapterPosition`获取当前`itemView`，
     * 进而导致当前`itemView`不会挪到新的锚点位置，这不符合优化方案的预期，需要回收离屏页面的`itemView`。
     *
     * **注意**：当`ViewPager2.getOffscreenPageLimit > 0`时才会出现这个问题。
     */
    private fun removeAndRecycleViewsIfNecessary() {
        val recyclerView = content.viewPager2.recyclerView
        val layoutManager = recyclerView.layoutManager ?: return
        val recycler = recyclerView.mRecycler ?: return
        for (index in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(index)
            val holder = recyclerView.getChildViewHolder(child)
            if (holder == null || holder.isViewCacheExtensionScrap) continue
            val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
            tempAttachedScrap[bindingAdapterPosition]?.let { pendingRemovals.add(holder) }
        }

        if (pendingRemovals.isNotEmpty()) {
            pendingRemovals.forEach { holder ->
                // 对holder添加FLAG_INVALID，将无法回收进离屏缓存
                holder.addFlags(ViewHolder.FLAG_INVALID)
                layoutManager.removeAndRecycleView(holder.itemView, recycler)
            }
            pendingRemovals.clear()
        }
    }

    /**
     * [RecyclerView]的布局流程会调用[Recycler.tryGetViewHolderForPositionByDeadline]填充`itemView`，
     * 若从离屏缓存获取到`cachedView`，则不会根据`itemId`或`bindingAdapterPosition`获取当前`itemView`，
     * 进而导致当前`itemView`不会挪到新的锚点位置，这不符合优化方案的预期，需要回收离屏缓存的`cachedView`。
     */
    fun recycleCachedViewsIfNecessary() {
        val recyclerView = content.viewPager2.recyclerView
        val recycler = recyclerView.mRecycler ?: return
        val cachedViews = recycler.mCachedViews ?: return
        for (index in cachedViews.indices.reversed()) {
            val holder = cachedViews[index]
            if (holder.isViewCacheExtensionScrap) continue
            val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
            tempAttachedScrap[bindingAdapterPosition] ?: continue
            recycler.recycleCachedViewAt(index)
        }
    }

    private fun RecyclerView.addAttachedScrapForLayoutPosition(layoutPosition: Int, offset: Int) {
        val holder = findViewHolderForLayoutPosition(layoutPosition) ?: return
        holder.viewCacheExtensionPosition = layoutPosition + offset
        val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
        tempAttachedScrap[bindingAdapterPosition] = holder
    }

    private fun clear() {
        if (tempAttachedScrap.size > 0) tempAttachedScrap.clear()
        if (pendingRemovals.isNotEmpty()) pendingRemovals.clear()
    }
}

/**
 * 兼容单元测试
 */
internal interface TempAttachedScrap {
    val size: Int
    operator fun get(bindingAdapterPosition: Int): ViewHolder?
    operator fun set(bindingAdapterPosition: Int, holder: ViewHolder)
    fun clear()
}

private class DefaultTempAttachedScrap : TempAttachedScrap {
    private val sparseArray = SparseArray<ViewHolder>()

    override val size: Int
        get() = sparseArray.size()

    override fun get(bindingAdapterPosition: Int): ViewHolder? {
        return sparseArray[bindingAdapterPosition]
    }

    override fun set(bindingAdapterPosition: Int, holder: ViewHolder) {
        sparseArray[bindingAdapterPosition] = holder
    }

    override fun clear() = sparseArray.clear()
}

/**
 * [Recycler.tryGetViewHolderForPositionByDeadline]：
 * 1. [Recycler.getScrapOrHiddenOrCachedHolderForPosition]
 * 2. [Recycler.getScrapOrCachedViewForId]
 * 添加[FLAG_RETURNED_FROM_SCRAP]，让这两步不能获取到`scrap`。
 */
private var ViewHolder.viewCacheExtensionPosition: Int
    get() {
        if (!wasReturnedFromScrap()) return NO_POSITION
        return itemView.getTag(R.id.tag_vp2_view_cache_extension_scrap) as? Int ?: NO_POSITION
    }
    set(value) {
        itemView.setTag(R.id.tag_vp2_view_cache_extension_scrap, value)
        if (value != NO_POSITION) addFlags(FLAG_RETURNED_FROM_SCRAP) else clearReturnedFromScrapFlag()
    }

private val ViewHolder.isViewCacheExtensionScrap: Boolean
    get() = viewCacheExtensionPosition != NO_POSITION

private class GetScrapForBindingAdapterPosition(
    private val content: LoopPagerContent,
    private val original: ViewCacheExtension?
) : ViewCacheExtension() {

    override fun getViewForPositionAndType(recycler: Recycler, position: Int, type: Int): View? {
        val view = recycler.getScrapForBindingAdapterPosition(position)
        return view ?: original?.getViewForPositionAndType(recycler, position, type)
    }

    /**
     * 判断逻辑参考自[Recycler.getScrapOrHiddenOrCachedHolderForPosition]
     */
    private fun Recycler.getScrapForBindingAdapterPosition(position: Int): View? {
        if (content.viewPager2.recyclerView.mState.willRunSimpleAnimations()) return null
        val attachedScrap = mAttachedScrap ?: emptyList<ViewHolder>()
        for (index in attachedScrap.indices) {
            val holder = attachedScrap[index]
            if (holder.viewCacheExtensionPosition == position
                    && holder.isSameBindingAdapterPosition(position)
                    && !holder.isInvalid && !holder.isRemoved && !holder.isUpdated) {
                // 更新layoutPosition，确保后续能消费滚动偏移
                holder.mPosition = position
                holder.viewCacheExtensionPosition = NO_POSITION
                holder.addFlags(FLAG_RETURNED_FROM_SCRAP)
                return holder.itemView
            }
        }
        return null
    }

    private fun ViewHolder.isSameBindingAdapterPosition(position: Int): Boolean {
        return content.toBindingAdapterPosition(layoutPosition) == content.toBindingAdapterPosition(position)
    }
}