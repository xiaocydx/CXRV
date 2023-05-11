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

@file:JvmName("PageLoopCallbackInternalKt")
@file:Suppress("PackageDirectoryMismatch", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.recyclerview.widget

import android.view.View
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent
import com.xiaocydx.cxrv.viewpager2.loop.RecordDataObserver

/**
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerCallback(
    private val content: LoopPagerContent,
    private val viewPager2: ViewPager2
) : ViewPager2.OnPageChangeCallback() {
    private var isAttached = false
    private val observer = AdapterDataObserverImpl()

    fun attach() {
        if (isAttached) return
        isAttached = true
        viewPager2.registerOnPageChangeCallback(this)
        content.adapter.registerAdapterDataObserver(observer)
    }

    fun detach() {
        if (!isAttached) return
        isAttached = false
        viewPager2.unregisterOnPageChangeCallback(this)
        content.adapter.unregisterAdapterDataObserver(observer)
    }

    // TODO:
    //  1. 修复多指无效的问题
    //  2. 滚动过程更新会是什么表现？
    override fun onPageScrollStateChanged(state: Int) {
        if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
            setPendingAnchorInfo(content.itemCount)
        }
    }

    private fun setPendingAnchorInfo(contentCount: Int) {
        if (!content.canLoop) return
        val extraPageLimit = content.extraPageLimit
        val currentItem = when (val layoutPosition = viewPager2.currentItem) {
            extraPageLimit - 1 -> contentCount + layoutPosition
            contentCount + extraPageLimit -> extraPageLimit
            else -> return
        }
        setCurrentItem(currentItem)
    }

    private fun setCurrentItem(currentItem: Int) {
        if (viewPager2.currentItem == currentItem) return
        viewPager2.setCurrentItem(currentItem, false)
        viewPager2.optimizeNextFrameSetCurrentItemScroll(content)
    }

    private inner class AdapterDataObserverImpl : RecordDataObserver(content.adapter) {

        // TODO: 补充全部更新逻辑，解决离屏缓存的更新问题
        override fun itemRangeInserted(positionStart: Int, itemCount: Int) {
            when {
                lastItemCount == 0 -> return
                positionStart == lastItemCount -> setPendingAnchorInfo(lastItemCount)
                positionStart == 0 -> setPendingAnchorInfo(lastItemCount)
            }
        }
    }
}

private fun ViewPager2.optimizeNextFrameSetCurrentItemScroll(content: LoopPagerContent) {
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