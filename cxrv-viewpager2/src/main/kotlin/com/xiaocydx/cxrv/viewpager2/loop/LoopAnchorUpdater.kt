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

@file:JvmName("LoopAnchorUpdaterInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.util.SparseArray
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.UpdateScenes.AdapterNotify
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.scrollToPositionDirect
import androidx.viewpager2.widget.setCurrentItemDirect
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.PADDING_EXTRA_PAGE_LIMIT

/**
 * [ViewPager2]循环页面的锚点信息更新器
 *
 * @author xcc
 * @date 2023/5/13
 */
internal interface LoopAnchorUpdater {

    /**
     * 若`viewPager.currentItem`是附加页面，则更新锚点信息
     *
     * 例如A、B、C是原始页面，带`*`的是附加页面，`content.extraPageLimit = 2`：
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * ```
     * 假设`viewPager.currentItem`为`C*`，更新锚点信息，
     * 下一帧布局流程以`C`为锚点，往两侧填充`itemView`。
     *
     * 实现类会优化更新锚点信息的过程，避免移除`itemView`，绑定新的[ViewHolder]，
     * 优化效果可以理解为将`B*、C*、A`的`itemView`，挪到`B、C，A*`的位置进行展示，
     *
     * @param scenes  更新锚点信息的场景，不同的场景根据具体需求重写相关函数。
     * @param content 若[scenes]为[AdapterNotify]，则根据`content.previous`计算新锚点。
     */
    fun updateAnchorInfo(scenes: UpdateScenes, content: LoopPagerContent)

    /**
     * 移除[updateAnchorInfo]添加的待处理操作
     */
    fun removeUpdateAnchorInfoPending()
}

/**
 * [LoopAnchorUpdater.updateAnchorInfo]更新锚点信息的场景
 */
internal sealed class UpdateScenes(val canSetCurrentItem: Boolean = true) {
    /**
     * 开始手势拖动
     */
    object Dragging : UpdateScenes()

    /**
     * `scrollToPosition()`
     */
    object Scroll : UpdateScenes()

    /**
     * `smoothScrollToPosition()`
     */
    object SmoothScroll : UpdateScenes()

    /**
     * [LoopPagerAdapter]的局部更新通知，根据`content.previous`计算新锚点
     */
    object AdapterNotify : UpdateScenes() {
        override fun getAnchorContent(content: LoopPagerContent) = content.previous
    }

    /**
     * 修复多指交替滚动未更新锚点信息的问题，不设置`viewPager2.currentItem`
     */
    class ScrolledFix(private val currentPosition: Int) : UpdateScenes(canSetCurrentItem = false) {
        override fun getCurrentPosition(content: LoopPagerContent) = currentPosition
    }

    /**
     * 获取计算新锚点的[LoopPagerContent]
     */
    open fun getAnchorContent(content: LoopPagerContent) = content

    /**
     * 获取计算新锚点的`currentPosition`
     *
     * @param content [getAnchorContent]的返回结果
     */
    open fun getCurrentPosition(content: LoopPagerContent) = content.viewPager2.currentItem
}

/**
 * ### 优化初衷
 * 当滚动到开始端和结束端的附加页面时，再次触发滚动，会更新锚点信息，
 * 更新锚点信息若通过非平滑滚动实现，则会导致可见的`itemView`被移除，
 * [RecyclerView]按更新后的锚点信息进行布局，绑定新的[ViewHolder]，
 * 对图片内容而言，通常有图片缓存，因此更新锚点信息产生的影响较小，
 * 但对视频内容而言，可见的`itemView`被移除、绑定新的[ViewHolder]，
 * 产生的影响较大。
 *
 * ### 优化方案
 * [updateAnchorInfoInNextLayout]查找目标`itemView`，处理跟离屏页面和离屏缓存的冲突。
 */
internal class LoopAnchorUpdaterImpl : LoopAnchorUpdater {
    private val targetScrapStore = SparseArray<ViewHolder>()
    private var preDrawListener: OneShotPreDrawListener? = null

    override fun updateAnchorInfo(scenes: UpdateScenes, content: LoopPagerContent) {
        if (!content.previous.supportLoop() && content.supportLoop()) {
            // 不支持循环到支持循环的转换过程不需要更新锚点信息
            removeUpdateAnchorInfoPending()
            return
        }

        // 在下一次布局完成之前，不需要再更新锚点信息
        if (hasUpdateAnchorInfoPending()) return
        val anchorContent = scenes.getAnchorContent(content)
        val anchorPosition = getNewAnchorPosition(scenes, anchorContent)
        if (anchorPosition == NO_POSITION) return
        updateAnchorInfoInNextLayout(anchorPosition, scenes, anchorContent)
        addUpdateAnchorInfoPending(anchorContent)
    }

    private fun hasUpdateAnchorInfoPending() = preDrawListener != null

    private fun addUpdateAnchorInfoPending(content: LoopPagerContent) {
        // 交换layoutPosition更新锚点信息，下一帧布局流程不会执行dispatchOnScrolled(0, 0)，
        // 因此主动执行dispatchOnScrolled(0, 0)，确保ScrollEventAdapter下一帧更新选中位置，
        // 进而确保OnPageChangeCallback.onPageScrolled()的position参数正确。
        preDrawListener = OneShotPreDrawListener.add(content.viewPager2) {
            content.viewPager2.recyclerView.dispatchOnScrolled(dx = 0, dy = 0)
            preDrawListener = null
        }
    }

    override fun removeUpdateAnchorInfoPending() {
        preDrawListener?.removeListener()
        preDrawListener = null
    }

    /**
     * 若`currentPosition`是附加页面，则返回对应原始页面的`layoutPosition`
     */
    private fun getNewAnchorPosition(scenes: UpdateScenes, content: LoopPagerContent): Int {
        if (!content.supportLoop()) return NO_POSITION
        val headerFirst = content.firstExtraLayoutPosition(isHeader = true)
        val headerLast = content.lastExtraLayoutPosition(isHeader = true)
        val footerFirst = content.firstExtraLayoutPosition(isHeader = false)
        val footerLast = content.lastExtraLayoutPosition(isHeader = false)
        return when (val currentPosition = scenes.getCurrentPosition(content)) {
            in headerFirst..headerLast -> currentPosition + content.itemCount
            in footerFirst..footerLast -> currentPosition - content.itemCount
            else -> NO_POSITION
        }
    }

    /**
     * [RecyclerView]的布局流程会调用[Recycler.tryGetViewHolderForPositionByDeadline]填充`itemView`，
     * 该函数确保修改当前`targetScrap`的`layoutPosition`后，下一次布局基于新锚点填充当前`targetScrap`。
     */
    private fun updateAnchorInfoInNextLayout(anchorPosition: Int, scenes: UpdateScenes, content: LoopPagerContent) {
        val currentPosition = scenes.getCurrentPosition(content)
        addTargetScrapForLayoutPosition(currentPosition, content)
        if (content.extraPageLimit == PADDING_EXTRA_PAGE_LIMIT) {
            addTargetScrapForLayoutPosition(currentPosition - 1, content)
            addTargetScrapForLayoutPosition(currentPosition + 1, content)
        }

        if (targetScrapStore.size() != 0) {
            swapLayoutPositionForOffscreenPage(content)
            swapLayoutPositionForCachedViews(content)
            swapLayoutPositionForTargetScrap(anchorPosition, currentPosition, content)
            targetScrapStore.clear()
        }

        // 下一次布局LinearLayoutManager会自行计算出当前targetScrap的锚点信息，
        // RecyclerView.dispatchLayoutStep3()回收上述已处理但未填充的离屏页面。
        if (scenes.canSetCurrentItem) {
            content.viewPager2.setCurrentItemDirect(anchorPosition)
        } else {
            content.viewPager2.scrollToPositionDirect(anchorPosition)
        }
    }

    private var ViewHolder.realLayoutPosition: Int
        get() = mPosition
        set(value) {
            mPosition = value
        }

    private fun addTargetScrapForLayoutPosition(layoutPosition: Int, content: LoopPagerContent) {
        val recyclerView = content.viewPager2.recyclerView
        val holder = recyclerView.findViewHolderForLayoutPosition(layoutPosition) ?: return
        val bindingAdapterPosition = content.toBindingAdapterPosition(holder.realLayoutPosition)
        targetScrapStore[bindingAdapterPosition] = holder
    }

    /**
     * 对有冲突的离屏页面设置`targetScrap`的`layoutPosition`，避免下一次布局基于新锚点填充离屏页面
     */
    private fun swapLayoutPositionForOffscreenPage(content: LoopPagerContent) {
        val recyclerView = content.viewPager2.recyclerView
        for (index in 0 until recyclerView.childCount) {
            val holder = recyclerView.getChildAt(index).let(recyclerView::getChildViewHolder)
            val bindingAdapterPosition = content.toBindingAdapterPosition(holder.realLayoutPosition)
            val scrap = targetScrapStore[bindingAdapterPosition]
            if (scrap == null || scrap === holder) continue
            holder.setLayoutPosition(scrap.realLayoutPosition, "OffscreenPage", content)
        }
    }

    /**
     * 对有冲突的离屏缓存设置`targetScrap`的`layoutPosition`，避免下一次布局基于新锚点填充离屏缓存
     */
    private fun swapLayoutPositionForCachedViews(content: LoopPagerContent) {
        val recyclerView = content.viewPager2.recyclerView
        val cachedViews = recyclerView.mRecycler?.mCachedViews ?: return
        for (index in cachedViews.indices) {
            val holder = cachedViews[index]
            val bindingAdapterPosition = content.toBindingAdapterPosition(holder.realLayoutPosition)
            val scrap = targetScrapStore[bindingAdapterPosition]
            if (scrap == null || scrap === holder) continue
            holder.setLayoutPosition(scrap.realLayoutPosition, "CachedViews", content)
        }
    }

    /**
     * 对`targetScrap`设置基于[anchorPosition]的`layoutPosition`，完成`layoutPosition`的交换
     */
    private fun swapLayoutPositionForTargetScrap(anchorPosition: Int, currentPosition: Int, content: LoopPagerContent) {
        val size = targetScrapStore.size()
        val offset = anchorPosition - currentPosition
        for (index in 0 until size) {
            val scrap = targetScrapStore.valueAt(index)
            val layoutPosition = scrap.realLayoutPosition + offset
            scrap.setLayoutPosition(layoutPosition, "TargetScrap", content)
        }
    }

    private fun ViewHolder.setLayoutPosition(layoutPosition: Int, tag: String, content: LoopPagerContent) {
        val firstLayoutPosition = content.firstLayoutPosition()
        val lastLayoutPosition = content.lastLayoutPosition()
        if (layoutPosition !in firstLayoutPosition..lastLayoutPosition) {
            throw IndexOutOfBoundsException("$tag setLayoutPosition()\n" +
                    "realLayoutPosition = $realLayoutPosition\n" +
                    "targetLayoutPosition = $layoutPosition\n" +
                    "layoutPositionRange = [$firstLayoutPosition, $lastLayoutPosition]\n" +
                    "holder = $this")
        }
        realLayoutPosition = layoutPosition
    }
}