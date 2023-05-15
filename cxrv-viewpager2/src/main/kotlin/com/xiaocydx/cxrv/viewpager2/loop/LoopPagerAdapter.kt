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

@file:JvmName("LoopPagerAdapterInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.*
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.payload.Payload
import com.xiaocydx.cxrv.payload.value
import com.xiaocydx.cxrv.viewpager2.loop.LoopAnchorUpdater
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent
import com.xiaocydx.cxrv.viewpager2.loop.RecordDataObserver
import java.lang.Integer.min
import kotlin.math.max

/**
 * [ViewPager2]循环页面的适配器，负责实现附加页面和同步更新离屏缓存
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerAdapter(
    private val content: LoopPagerContent,
    private val updater: LoopAnchorUpdater
) : Adapter<ViewHolder>() {
    private val observer = AdapterDataObserverImpl()
    private val header = ContentExtraPage(isHeader = true)
    private val footer = ContentExtraPage(isHeader = false)

    /**
     * [LoopPagerAdapter]仅转发函数调用，不会创建其它类型的[ViewHolder]
     */
    @Suppress("UNCHECKED_CAST")
    private val contentAdapter = content.adapter as Adapter<ViewHolder>

    init {
        super.setHasStableIds(contentAdapter.hasStableIds())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return contentAdapter.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        // 将holder.mBindingAdapter修改为contentAdapter，
        // 确保不影响使用bindingAdapter和bindingAdapterPosition实现的功能。
        holder.mBindingAdapter = contentAdapter
        contentAdapter.onBindViewHolder(holder, content.toBindingAdapterPosition(position), payloads)
    }

    override fun findRelativeAdapterPositionIn(adapter: Adapter<*>, holder: ViewHolder, position: Int): Int {
        // 将holder.bindingAdapterPosition修改为contentAdapter的position，
        // 确保不影响使用bindingAdapter和bindingAdapterPosition实现的功能。
        val isValidAdapter = adapter === this || adapter === contentAdapter
        return if (isValidAdapter) content.toBindingAdapterPosition(position) else NO_POSITION
    }

    override fun getItemViewType(position: Int): Int {
        return contentAdapter.getItemViewType(content.toBindingAdapterPosition(position))
    }

    override fun setHasStableIds(hasStableIds: Boolean) {
        contentAdapter.setHasStableIds(hasStableIds)
        super.setHasStableIds(hasStableIds)
    }

    override fun getItemId(position: Int): Long {
        return contentAdapter.getItemId(content.toBindingAdapterPosition(position))
    }

    override fun getItemCount(): Int {
        return header.itemCount + content.currentCount + footer.itemCount
    }

    override fun onViewRecycled(holder: ViewHolder) {
        contentAdapter.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        return contentAdapter.onFailedToRecycleView(holder)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        contentAdapter.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        contentAdapter.onViewDetachedFromWindow(holder)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        header.recordContentCount()
        footer.recordContentCount()
        contentAdapter.apply {
            registerAdapterDataObserver(header)
            registerAdapterDataObserver(footer)
            registerAdapterDataObserver(observer)
            onAttachedToRecyclerView(recyclerView)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        contentAdapter.apply {
            unregisterAdapterDataObserver(header)
            unregisterAdapterDataObserver(footer)
            unregisterAdapterDataObserver(observer)
            onDetachedFromRecyclerView(recyclerView)
        }
    }

    override fun setStateRestorationPolicy(strategy: StateRestorationPolicy) {
        contentAdapter.stateRestorationPolicy = strategy
        super.setStateRestorationPolicy(strategy)
    }

    private inner class AdapterDataObserverImpl : AdapterDataObserver() {

        @SuppressLint("NotifyDataSetChanged")
        override fun onChanged() {
            this@LoopPagerAdapter.notifyDataSetChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            onItemRangeChanged(positionStart, itemCount, null)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            val start = positionStart + header.itemCount
            this@LoopPagerAdapter.notifyItemRangeChanged(start, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            val start = positionStart + header.itemCount
            this@LoopPagerAdapter.notifyItemRangeInserted(start, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            val start = positionStart + header.itemCount
            this@LoopPagerAdapter.notifyItemRangeRemoved(start, itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            val from = fromPosition + header.itemCount
            val to = toPosition + header.itemCount
            this@LoopPagerAdapter.notifyItemMoved(from, to)
        }
    }

    private inner class ContentExtraPage(
        private val isHeader: Boolean
    ) : RecordDataObserver(content.adapter) {
        private var currentAsItem = content.supportLoop()
        private var previousAsItem = currentAsItem
        private val lastContentCount: Int
            get() = lastItemCount

        val itemCount: Int
            get() = if (currentAsItem) content.extraPageLimit else 0

        fun recordContentCount() = recordItemCount()

        override fun changed() {
            updateAllExtraPage()
        }

        override fun itemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            updateRangeExtraPage(positionStart, itemCount, payload)
        }

        override fun itemRangeInserted(positionStart: Int, itemCount: Int) {
            val updateCount = content.extraPageLimit
            // 更新footer，跟itemRangeChanged()计算交集的过程一致，
            // 更新header，需要偏移positionStart，再计算交集，例如：
            // extraPageLimit = 2, {B* ，C* ，A ，B ，C ，A* ，B*}
            // bindingAdapterPositionRange = [0, 2]
            // headerBindingAdapterPositionRange = [1, 2]
            // 1. positionStart <= 1，无需更新header。
            // 2. positionStart = 2，更新headerBindingAdapterPositionRange = [1, 1]
            // 3. positionStart = 3，更新headerBindingAdapterPositionRange = [1, 2]
            // positionStart - updateCount能让updateRangeExtraPage()计算出预期的交集。
            val start = if (isHeader) positionStart - updateCount else positionStart
            if (updateRangeExtraPage(start, updateCount)) {
                // 如果viewPager.currentItem是附加页面，那么更新会导致当前可见内容发生变化，
                // 这不符合预期，需要更新锚点信息，可以理解为将当前内容，挪到新的锚点进行展示。
                updater.updateAnchor(offset = 0, lastContentCount)
            }
        }

        override fun itemRangeRemoved(positionStart: Int, itemCount: Int) {
            val bindingFirst = content.firstExtraBindingAdapterPosition(isHeader, lastContentCount)
            val bindingLast = content.lastExtraBindingAdapterPosition(isHeader, lastContentCount)
            when {
                // 更新header，未移除最后一个，跟itemRangeChanged()计算交集的过程一致
                isHeader && positionStart < bindingLast -> updateRangeExtraPage(positionStart, itemCount)
                // 更新footer，未移除第一个，跟itemRangeChanged()计算交集的过程一致
                !isHeader && positionStart > bindingFirst -> updateRangeExtraPage(positionStart, itemCount)
                else -> updateAllExtraPage()
            }
        }

        override fun itemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            // 更新header和footer，需要先算出更新范围，再计算交集
            val start = min(fromPosition, toPosition)
            val end = max(fromPosition, toPosition)
            if (updateRangeExtraPage(start, itemCount = end - start + 1)) {
                updater.updateAnchor(offset = if (isHeader) -1 else 1, lastContentCount)
            }
        }

        private fun updateAllExtraPage(): Boolean {
            val layoutFirst = content.firstExtraLayoutPosition(isHeader, lastContentCount)
            return updateExtraPage(layoutFirst, updateCount = itemCount)
        }

        private fun updateRangeExtraPage(positionStart: Int, itemCount: Int, payload: Any? = PAYLOAD): Boolean {
            var bindingFirst = content.firstExtraBindingAdapterPosition(isHeader, lastContentCount)
            var bindingLast = content.lastExtraBindingAdapterPosition(isHeader, lastContentCount)
            if (bindingFirst == NO_POSITION) {
                // 之前没有附加页面，当前可能有附加页面，按当前数值更新
                return updateExtraPage(NO_POSITION, updateCount = 0)
            }

            val positionEnd = positionStart + itemCount - 1
            if (positionStart > bindingLast || positionEnd < bindingFirst) {
                // first..last跟positionStart..positionEnd之间没有交集
                return false
            }

            // 配置属性确保first..last小于等于positionStart..positionEnd
            val previousFirst = bindingFirst
            bindingFirst = bindingFirst.coerceAtLeast(positionStart)
            bindingLast = bindingLast.coerceAtMost(positionEnd)
            val offset = bindingFirst - previousFirst

            // 对比前后bindingFirst得出的offset，也是layoutFirst的offset，
            // 以此实现bindingAdapterPosition到layoutPosition的转换计算。
            val layoutFirst = content.firstExtraLayoutPosition(isHeader, lastContentCount) + offset
            val updateCount = bindingLast - bindingFirst + 1
            return updateExtraPage(layoutFirst, updateCount, payload)
        }

        private fun updateExtraPage(layoutFirst: Int, updateCount: Int, payload: Any? = PAYLOAD): Boolean {
            currentAsItem = content.supportLoop()
            var updated = true
            var first = layoutFirst
            var count = updateCount
            if (currentAsItem && first == NO_POSITION) {
                // 之前没有附加页面，当前有附加页面，按当前数值更新
                first = content.firstExtraLayoutPosition(isHeader)
                count = itemCount
            }
            when {
                first == NO_POSITION -> updated = false
                !previousAsItem && currentAsItem -> {
                    this@LoopPagerAdapter.notifyItemRangeInserted(first, count)
                }
                previousAsItem && !currentAsItem -> {
                    this@LoopPagerAdapter.notifyItemRangeRemoved(first, count)
                }
                previousAsItem && currentAsItem -> {
                    this@LoopPagerAdapter.notifyItemRangeChanged(first, count, payload)
                }
                else -> updated = false
            }
            previousAsItem = currentAsItem
            return updated
        }
    }

    private companion object {
        // TODO: 是否需要合并去重？
        val PAYLOAD = Payload { add(Payload.value(31)) }
    }
}