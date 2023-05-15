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

import android.util.SparseIntArray
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.viewpager2.R
import com.xiaocydx.cxrv.viewpager2.loop.LookupDirection
import com.xiaocydx.cxrv.viewpager2.loop.LoopAnchorUpdater
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent

/**
 * [ViewPager2]循环页面的滚动器，负责更新锚点信息和提供滚动函数
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerScroller(
    private val content: LoopPagerContent
) : OnPageChangeCallback(), LoopAnchorUpdater {
    private var runnable: SmoothScrollRunnable? = null
    private val viewPager2: ViewPager2
        get() = content.viewPager2

    init {
        viewPager2.registerOnPageChangeCallback(this)
    }

    // TODO: 修复多指无效的问题
    override fun onPageScrollStateChanged(state: Int) {
        // TODO: 需要在平滑滚动结束前，更新锚点信息调整位置，
        //  结束后更新，对Padding场景而言，有一帧未填充附加页面。
        if (state == SCROLL_STATE_DRAGGING) {
            updateAnchor(offset = 0, content.currentCount)
        }
    }

    override fun updateAnchor(offset: Int, contentCount: Int) {
        if (!content.supportLoop(contentCount)) return
        val headerFirst = content.firstExtraLayoutPosition(isHeader = true, contentCount)
        val headerLast = content.lastExtraLayoutPosition(isHeader = true, contentCount)
        val footerFirst = content.firstExtraLayoutPosition(isHeader = false, contentCount)
        val footerLast = content.lastExtraLayoutPosition(isHeader = false, contentCount)
        val anchorPosition = when (val currentItem = viewPager2.currentItem) {
            in headerFirst..headerLast -> currentItem + contentCount + offset
            in footerFirst..footerLast -> currentItem - contentCount + offset
            else -> return
        }
        scrollToPosition(anchorPosition)
    }

    /**
     * [ViewPager2]非平滑滚动至[layoutPosition]
     *
     * [LoopPagerAdapter]确保同步更新离屏缓存，因此非平滑滚动都可以用[optimizeNextFrameScroll]。
     */
    fun scrollToPosition(layoutPosition: Int) {
        if (layoutPosition == NO_POSITION || viewPager2.currentItem == layoutPosition) return
        removeRunnable()
        viewPager2.setCurrentItem(layoutPosition, false)
        viewPager2.optimizeNextFrameScroll(content)
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

    fun removeCallback() {
        removeRunnable()
        viewPager2.unregisterOnPageChangeCallback(this)
    }

    private fun removeRunnable() {
        runnable?.let(viewPager2::removeCallbacks)
        runnable = null
    }

    private inner class SmoothScrollRunnable(private val layoutPosition: Int) : Runnable {
        override fun run() {
            viewPager2.currentItem = layoutPosition
            runnable = null
        }
    }
}

@VisibleForTesting
internal var OPTIMIZE_NEXT_FRAME_SCROLL_ENABLED = true

@VisibleForTesting
internal var OPTIMIZE_NEXT_FRAME_RESTORE_ENABLED = true

/**
 * ### 优化初衷
 * 当滚动到起始端和结束端的附加页面时，再次触发滚动，会更新锚点信息，
 * 更新锚点信息是通过非平滑滚动实现，这会导致可见的`itemView`被移除，
 * 下一帧[RecyclerView]按更新后的锚点信息布局，填充新的[ViewHolder]，
 * 对图片内容而言，通常有图片缓存，因此更新锚点信息产生的影响较小，
 * 但对视频内容而言，可见的`itemView`被移除、绑定新的[ViewHolder]，
 * 产生的影响较大。
 *
 * ### 优化方案
 * 利用[ViewCacheExtension]解决上述问题，下一帧布局从[Recycler]的暂存区和离屏缓存，
 * 获取`bindingAdapterPosition`一致的[ViewHolder]，以此避免可见的`itemView`被移除、
 * 绑定新的[ViewHolder]。
 */
private fun ViewPager2.optimizeNextFrameScroll(content: LoopPagerContent) {
    if (!OPTIMIZE_NEXT_FRAME_SCROLL_ENABLED) return
    val original = recyclerView.getViewCacheExtensionOrNull()
    if (original is GetScrapForBindingAdapterPosition) return
    val extension = GetScrapForBindingAdapterPosition(content, recyclerView, original)
    extension.recycleCachedViewForCurrentViews()
    recyclerView.setViewCacheExtension(extension)
    if (OPTIMIZE_NEXT_FRAME_RESTORE_ENABLED) {
        doOnPreDraw { recyclerView.setViewCacheExtension(original) }
    }
}

private class GetScrapForBindingAdapterPosition(
    private val content: LoopPagerContent,
    private val recyclerView: RecyclerView,
    private val original: ViewCacheExtension?
) : ViewCacheExtension() {

    /**
     * 下一帧[RecyclerView]布局流程调用`RecyclerView.tryGetViewHolderForPositionByDeadline()`，
     * 若从离屏缓存获取了可用的`holder`，则不会调用[getViewForPositionAndType]填充当前`itemView`，
     * 进而导致当前`itemView`不会挪到新的锚点位置，这不符合优化方案的预期，因此需要回收离屏缓存。
     */
    fun recycleCachedViewForCurrentViews() {
        val cachedViews = recyclerView.mRecycler.mCachedViews ?: return

        @Suppress("UNCHECKED_CAST")
        var tempCachedViews = recyclerView.getTag(R.id.tag_vp2_temp_cached_views) as? SparseIntArray
        if (tempCachedViews == null) {
            tempCachedViews = SparseIntArray()
            recyclerView.setTag(R.id.tag_vp2_temp_cached_views, tempCachedViews)
        }

        for (index in cachedViews.indices) {
            val holder = cachedViews[index]
            val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
            tempCachedViews.put(bindingAdapterPosition, index)
        }

        if (tempCachedViews.size() > 0) {
            for (index in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(index)
                val holder = recyclerView.getChildViewHolder(child) ?: continue
                val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
                // 若离屏缓存包含bindingAdapterPosition，则将离屏缓存的holder回收进RecycledViewPool
                tempCachedViews.get(bindingAdapterPosition, -1)
                    .takeIf { it != -1 }?.let(recyclerView.mRecycler::recycleCachedViewAt)
            }
        }
        tempCachedViews.clear()
    }

    override fun getViewForPositionAndType(recycler: Recycler, position: Int, type: Int): View? {
        val runAnimations = recyclerView.mState.willRunSimpleAnimations()
        val view = if (runAnimations) null else recycler.getScrapForBindingAdapterPosition(position)
        return view ?: original?.getViewForPositionAndType(recycler, position, type)
    }

    /**
     * 判断逻辑参考自[Recycler.getScrapOrHiddenOrCachedHolderForPosition]
     */
    private fun Recycler.getScrapForBindingAdapterPosition(position: Int): View? {
        val attachedScrap = mAttachedScrap ?: emptyList<ViewHolder>()
        for (index in attachedScrap.indices) {
            val holder = attachedScrap[index]
            if (!holder.wasReturnedFromScrap()
                    && holder.isSameBindingAdapterPosition(position)
                    && !holder.isInvalid && !holder.isRemoved) {
                // 修正layoutPosition，确保后续能消费滚动偏移
                holder.mPosition = position
                holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP)
                return holder.itemView
            }
        }

        // recycleCachedViewForCurrentViews()仅回收了一部分holder
        val cachedViews = mCachedViews ?: emptyList<ViewHolder>()
        for (index in cachedViews.indices) {
            val holder = cachedViews[index]
            if (!holder.isInvalid
                    && holder.isSameBindingAdapterPosition(position)
                    && !holder.isAttachedToTransitionOverlay) {
                // 修正layoutPosition，确保后续能消费滚动偏移
                holder.mPosition = position
                mCachedViews?.removeAt(index)
                return holder.itemView
            }
        }
        return null
    }

    private fun ViewHolder.isSameBindingAdapterPosition(position: Int): Boolean {
        return content.toBindingAdapterPosition(layoutPosition) == content.toBindingAdapterPosition(position)
    }
}