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
@file:Suppress("PackageDirectoryMismatch", "INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")


package androidx.recyclerview.widget

import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import androidx.core.view.OneShotPreDrawListener
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.setCurrentItemDirect
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent

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
     * @param fromNotify 是否调用自[AdapterDataObserver]的分发过程
     * @param content    若[fromNotify]为`true`，则根据`content.previous`计算新锚点，
     * 使用`content.previous`而不是`content`的原因可以看[LoopPagerAdapter]的注释描述。
     */
    fun updateAnchorInfo(fromNotify: Boolean, content: LoopPagerContent)

    /**
     * 移除[updateAnchorInfo]添加的待处理操作
     */
    fun removeUpdateAnchorInfoPending()
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
    private val targetScrapStore = targetScrapStoreProvider()
    private var preDrawListener: OneShotPreDrawListener? = null

    override fun updateAnchorInfo(fromNotify: Boolean, content: LoopPagerContent) {
        val anchorPosition: Int
        if (content.supportLoop() != content.previous.supportLoop()) {
            // 支持循环和不支持循环的相互转换过程
            removeUpdateAnchorInfoPending()
            anchorPosition = content.toLayoutPosition(content.viewPager2.currentItem)
            updateAnchorInfoByScrollToPosition(anchorPosition, content)
            addUpdateAnchorInfoPending(content)
            return
        }

        // 在下一次布局完成之前，不需要再通过优化方案更新锚点信息
        if (hasUpdateAnchorInfoPending()) return
        val anchorContent = if (!fromNotify) content else content.previous
        anchorPosition = getNewAnchorPositionForContent(anchorContent)
        when {
            anchorPosition == NO_POSITION -> return
            // OPTIMIZATION_ENABLED = false用于单元测试，对比优化前和优化后的效果
            !OPTIMIZATION_ENABLED -> updateAnchorInfoByScrollToPosition(anchorPosition, anchorContent)
            else -> updateAnchorInfoInNextLayout(anchorPosition, fromNotify, anchorContent)
        }
        addUpdateAnchorInfoPending(content)
    }

    private fun hasUpdateAnchorInfoPending(): Boolean {
        return preDrawListener != null
    }

    private fun addUpdateAnchorInfoPending(content: LoopPagerContent) {
        // 当通知局部更新时，若RecyclerView.hasFixedSize()为true，并且下一帧条件满足，
        // 则是在Animation回调执行布局流程，此时不能靠同步屏障被移除断言布局流程已完成。
        preDrawListener = content.viewPager2.doOnPreDraw { preDrawListener = null }
    }

    override fun removeUpdateAnchorInfoPending() {
        preDrawListener?.removeListener()
        preDrawListener = null
    }

    /**
     * 若`viewPager.currentItem`是附加页面，则返回对应原始页面的`layoutPosition`
     */
    private fun getNewAnchorPositionForContent(content: LoopPagerContent): Int {
        if (!content.supportLoop()) return NO_POSITION
        val headerFirst = content.firstExtraLayoutPosition(isHeader = true)
        val headerLast = content.lastExtraLayoutPosition(isHeader = true)
        val footerFirst = content.firstExtraLayoutPosition(isHeader = false)
        val footerLast = content.lastExtraLayoutPosition(isHeader = false)
        return when (val currentItem = content.viewPager2.currentItem) {
            in headerFirst..headerLast -> currentItem + content.itemCount
            in footerFirst..footerLast -> currentItem - content.itemCount
            else -> NO_POSITION
        }
    }

    /**
     * 当[RecyclerView.onInterceptTouchEvent]将滚动状态设为[SCROLL_STATE_DRAGGING]时，
     * 会对[OnScrollListener]分发新的滚动状态，若此时调用[ViewPager2.setCurrentItem]，
     * 则会调用至[RecyclerView.stopScroll]，将滚动状态设为[SCROLL_STATE_IDLE]，
     * 导致[RecyclerView.onInterceptTouchEvent]返回`false`，在实际场景中，
     * 手势滚动可能会因为这次触摸事件拦截失败，而出现交互不流畅的问题。
     */
    private fun updateAnchorInfoByScrollToPosition(anchorPosition: Int, content: LoopPagerContent) {
        content.viewPager2.setCurrentItemDirect(anchorPosition)
        content.viewPager2.recyclerView.layoutManager?.scrollToPosition(anchorPosition)
    }

    /**
     * [RecyclerView]的布局流程会调用[Recycler.tryGetViewHolderForPositionByDeadline]填充`itemView`，
     * 该函数确保修改当前`targetScrap`的`layoutPosition`后，下一次布局基于新锚点填充当前`targetScrap`。
     */
    private fun updateAnchorInfoInNextLayout(anchorPosition: Int, fromNotify: Boolean, content: LoopPagerContent) {
        val recyclerView = content.viewPager2.recyclerView
        val cachedViews = recyclerView.mRecycler?.mCachedViews ?: return

        // 查找当前targetScrap，并基于新锚点设置layoutPosition
        val current = content.viewPager2.currentItem
        val offset = anchorPosition - current
        content.viewPager2.setCurrentItemDirect(anchorPosition)
        addTargetScrapForLayoutPosition(current, offset, content)
        if (content.extraPageLimit == LoopPagerContent.PADDING_EXTRA_PAGE_LIMIT) {
            addTargetScrapForLayoutPosition(current - 1, offset, content)
            addTargetScrapForLayoutPosition(current + 1, offset, content)
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
        if (!fromNotify) recyclerView.requestLayout()
        if (targetScrapStore.size > 0) targetScrapStore.clear()
    }

    private fun addTargetScrapForLayoutPosition(layoutPosition: Int, offset: Int, content: LoopPagerContent) {
        val recyclerView = content.viewPager2.recyclerView
        val holder = recyclerView.findViewHolderForLayoutPosition(layoutPosition) ?: return
        val bindingAdapterPosition = content.toBindingAdapterPosition(holder.layoutPosition)
        holder.offsetPosition(offset, false)
        targetScrapStore[bindingAdapterPosition] = holder
    }

    companion object {
        @set:VisibleForTesting
        internal var OPTIMIZATION_ENABLED = true

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