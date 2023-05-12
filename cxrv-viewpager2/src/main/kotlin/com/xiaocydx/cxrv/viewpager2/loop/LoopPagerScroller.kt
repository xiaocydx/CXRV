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

import android.view.View
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.viewpager2.loop.LookupDirection
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent

/**
 * [ViewPager2]循环页面的滚动器，负责更新锚点信息和提供滚动函数
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerScroller(private val content: LoopPagerContent) : OnPageChangeCallback() {
    private var runnable: SmoothScrollRunnable? = null
    private var lastState = viewPager2.scrollState
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

    // TODO: 修复多指无效的问题
    override fun onPageScrollStateChanged(state: Int) {
        val isTouchScrollBeginning = state == SCROLL_STATE_DRAGGING
        // TODO: 平滑滚动过程需要更早更新锚点信息，停下来再更新，对Padding场景而言，有一帧额外页面是空白
        val isSmoothScrollEnding = lastState == SCROLL_STATE_SETTLING && state == SCROLL_STATE_IDLE
        if (isTouchScrollBeginning || isSmoothScrollEnding) {
            updateAnchor(content.itemCount)
        }
        lastState = state
    }

    fun updateAnchor(contentCount: Int) {
        if (!content.supportLoop) return
        val targetPosition = when (val currentItem = viewPager2.currentItem) {
            in content.startExtraLayoutPositionRange(contentCount) -> currentItem + contentCount
            in content.endExtraLayoutPositionRange(contentCount) -> currentItem - contentCount
            else -> return
        }
        scrollToPosition(targetPosition)
    }

    fun scrollToPosition(layoutPosition: Int) {
        if (layoutPosition == NO_POSITION || viewPager2.currentItem == layoutPosition) return
        removeRunnable()
        viewPager2.setCurrentItem(layoutPosition, false)
        viewPager2.optimizeNextFrameScroll(content)
    }

    fun smoothScrollToPosition(layoutPosition: Int, direction: LookupDirection = LookupDirection.END) {
        if (layoutPosition == NO_POSITION || viewPager2.currentItem == layoutPosition) return
        removeRunnable()
        val contentCount = content.itemCount
        val layoutPositionRange = content.layoutPositionRange()
        if (direction === LookupDirection.END) {
            if (viewPager2.currentItem < layoutPosition) {
                viewPager2.currentItem = layoutPosition
            } else if (layoutPosition + contentCount in layoutPositionRange) {
                viewPager2.currentItem = layoutPosition + contentCount
            } else {
                scrollToPosition((layoutPosition - 3).coerceAtLeast(layoutPositionRange.first))
                viewPager2.post(SmoothScrollRunnable(layoutPosition).also { runnable = it })
            }
        } else {
            if (layoutPosition < viewPager2.currentItem) {
                viewPager2.currentItem = layoutPosition
            } else if (layoutPosition - contentCount in layoutPositionRange) {
                viewPager2.currentItem = layoutPosition - contentCount
            } else {
                scrollToPosition((layoutPosition + 3).coerceAtMost(layoutPositionRange.last))
                viewPager2.post(SmoothScrollRunnable(layoutPosition).also { runnable = it })
            }
        }
    }

    private inner class SmoothScrollRunnable(private val layoutPosition: Int) : Runnable {
        override fun run() {
            viewPager2.currentItem = layoutPosition
            runnable = null
        }
    }
}

/**
 * 当滚动到起始端和结束端的额外页面，再次滚动时，会更新锚点信息以支持循环，
 * 更新锚点信息是通过非平滑滚动实现，这会导致可见的`itemView`被移除，
 * 下一帧[RecyclerView]按更新后的锚点信息布局，填充新的[ViewHolder]，
 * 对图片内容而言，一般有图片缓存，因此更新锚点信息的布局产生的影响比较小，
 * 但对视频内容而言，可见的`itemView`被移除、重新绑定新的[ViewHolder]，
 * 产生的影响比较大。
 *
 * 利用[ViewCacheExtension]解决产生的影响，下一帧布局从[Recycler]的暂存区和离屏缓存，
 * 获取`bindingAdapterPosition`一致的[ViewHolder]，以此避免可见的`itemView`被移除、
 * 重新绑定新的[ViewHolder]。
 */
private fun ViewPager2.optimizeNextFrameScroll(content: LoopPagerContent) {
    val recyclerView = getChildAt(0) as? RecyclerView ?: return
    // reflect < 1ms
    val original = recyclerView.getViewCacheExtensionOrNull()
    if (original is GetScrapOrCachedViewExtension) return
    val extension = GetScrapOrCachedViewExtension(content, recyclerView, original)
    recyclerView.setViewCacheExtension(extension)
    doOnPreDraw { recyclerView.setViewCacheExtension(original) }
}

// TODO: 统一mViewCacheExtensionField的缓存
private fun RecyclerView.getViewCacheExtensionOrNull(): ViewCacheExtension? {
    val mViewCacheExtensionField = runCatching {
        mRecycler.javaClass.getDeclaredField("mViewCacheExtension")
    }.getOrNull()?.apply { isAccessible = true } ?: return null
    return mViewCacheExtensionField.get(mRecycler) as? ViewCacheExtension
}

private class GetScrapOrCachedViewExtension(
    private val content: LoopPagerContent,
    private val recyclerView: RecyclerView,
    private val original: ViewCacheExtension?
) : ViewCacheExtension() {

    override fun getViewForPositionAndType(recycler: Recycler, position: Int, type: Int): View? {
        val runAnimations = recyclerView.mState.willRunSimpleAnimations()
        val view = if (runAnimations) null else recycler.getScrapOrCachedViewForPosition(position)
        return view ?: original?.getViewForPositionAndType(recycler, position, type)
    }

    /**
     * 判断逻辑参考自[Recycler.getScrapOrHiddenOrCachedHolderForPosition]
     */
    private fun Recycler.getScrapOrCachedViewForPosition(position: Int): View? {
        val attachedScrap = mAttachedScrap ?: emptyList<ViewHolder>()
        for (index in attachedScrap.indices) {
            val holder = attachedScrap[index]
            if (!holder.wasReturnedFromScrap()
                    && holder.isSameBindingAdapterPosition(position)
                    && !holder.isInvalid && !holder.isRemoved) {
                holder.mPosition = position
                holder.addFlags(ViewHolder.FLAG_RETURNED_FROM_SCRAP)
                return holder.itemView
            }
        }

        val cachedViews = mCachedViews ?: emptyList<ViewHolder>()
        for (index in cachedViews.indices) {
            val holder = cachedViews[index]
            if (!holder.isInvalid
                    && holder.isSameBindingAdapterPosition(position)
                    && !holder.isAttachedToTransitionOverlay) {
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