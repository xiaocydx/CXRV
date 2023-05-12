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
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent
import com.xiaocydx.cxrv.viewpager2.loop.RecordDataObserver

/**
 * @author xcc
 * @date 2023/5/11
 */
internal class LoopPagerAdapter(
    private val content: LoopPagerContent,
    private val updateAnchor: (contentCount: Int) -> Unit
) : Adapter<ViewHolder>() {
    private val observer = AdapterDataObserverImpl()
    private val header = ContentExtraPage(isHeader = true)
    private val footer = ContentExtraPage(isHeader = false)

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
        return header.itemCount + content.itemCount + footer.itemCount
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
        private var currentAsItem = content.supportLoop
        private var previousAsItem = currentAsItem
        private val lastContentCount: Int
            get() = lastItemCount

        val itemCount: Int
            get() = if (currentAsItem) content.extraPageLimit else 0

        fun recordContentCount() = recordItemCount()

        private fun updateExtraPage(payload: Any? = null) {
            currentAsItem = content.supportLoop
            // TODO: 一定要更新全部？
            val start = if (isHeader) 0 else this@LoopPagerAdapter.itemCount - itemCount
            when {
                !previousAsItem && currentAsItem -> {
                    this@LoopPagerAdapter.notifyItemRangeInserted(start, itemCount)
                }
                previousAsItem && !currentAsItem -> {
                    this@LoopPagerAdapter.notifyItemRangeRemoved(start, itemCount)
                }
                previousAsItem && currentAsItem -> {
                    this@LoopPagerAdapter.notifyItemRangeChanged(start, itemCount, payload)
                }
            }
            previousAsItem = currentAsItem
        }

        // TODO: 补充全部更新逻辑，解决离屏缓存的更新问题
        override fun itemRangeInserted(positionStart: Int, itemCount: Int) {
            when {
                lastContentCount == 0 -> if (itemCount > 1) updateExtraPage()
                isHeader && positionStart == lastContentCount -> {
                    updateExtraPage()
                    updateAnchor(lastContentCount)
                }
                !isHeader && positionStart == 0 -> {
                    updateExtraPage()
                    updateAnchor(lastContentCount)
                }
            }
        }
    }
}