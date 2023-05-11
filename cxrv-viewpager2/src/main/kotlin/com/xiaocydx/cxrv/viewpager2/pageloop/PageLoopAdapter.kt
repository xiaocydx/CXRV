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

@file:JvmName("PageHeaderFooterInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.viewpager2.pageloop.PageLoopContent

/**
 * @author xcc
 * @date 2023/5/11
 */
internal class PageLoopAdapter(
    private val content: PageLoopContent,
    private val updateAnchor: () -> Unit,
) : Adapter<ViewHolder>() {
    private val observer = AdapterDataObserverImpl()
    private val header = ExtraPage(isHeader = true, content, this, updateAnchor)
    private val footer = ExtraPage(isHeader = false, content, this, updateAnchor)

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
        val bindingAdapterPosition = content.toBindingAdapterPosition(position)
        // 将holder.mBindingAdapter，修改为contentAdapter，
        // 确保不影响依靠bindingAdapter和bindingAdapterPosition实现的功能。
        holder.mBindingAdapter = contentAdapter
        contentAdapter.onBindViewHolder(holder, bindingAdapterPosition, payloads)
    }

    override fun findRelativeAdapterPositionIn(adapter: Adapter<*>, holder: ViewHolder, position: Int): Int {
        // 将holder.bindingAdapterPosition，修改为contentAdapter的position，
        // 确保不影响依靠bindingAdapter和bindingAdapterPosition实现的功能。
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
        contentAdapter.registerAdapterDataObserver(observer)
        contentAdapter.onAttachedToRecyclerView(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        contentAdapter.unregisterAdapterDataObserver(observer)
        contentAdapter.onDetachedFromRecyclerView(recyclerView)
    }

    override fun setStateRestorationPolicy(strategy: StateRestorationPolicy) {
        contentAdapter.stateRestorationPolicy = strategy
        super.setStateRestorationPolicy(strategy)
    }

    @SuppressLint("NotifyDataSetChanged")
    private inner class AdapterDataObserverImpl : AdapterDataObserver() {
        private var lastContentCount = 0

        fun recordContentCount() {
            lastContentCount = content.itemCount
        }

        override fun onChanged() {
            recordContentCount()
            this@PageLoopAdapter.notifyDataSetChanged()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            onItemRangeChanged(positionStart, itemCount, null)
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            header.onContentRangeChanged(positionStart, itemCount, payload, lastContentCount)
            footer.onContentRangeChanged(positionStart, itemCount, payload, lastContentCount)
            recordContentCount()
            val start = positionStart + header.itemCount
            this@PageLoopAdapter.notifyItemRangeChanged(start, itemCount, payload)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            header.onContentRangeInserted(positionStart, itemCount, lastContentCount)
            footer.onContentRangeInserted(positionStart, itemCount, lastContentCount)
            recordContentCount()
            val start = positionStart + header.itemCount
            this@PageLoopAdapter.notifyItemRangeInserted(start, itemCount)
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            header.onContentRangeRemoved(positionStart, itemCount, lastContentCount)
            footer.onContentRangeRemoved(positionStart, itemCount, lastContentCount)
            recordContentCount()
            val start = positionStart + header.itemCount
            this@PageLoopAdapter.notifyItemRangeRemoved(start, itemCount)
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            header.onContentRangeMoved(fromPosition, toPosition, itemCount, lastContentCount)
            footer.onContentRangeMoved(fromPosition, toPosition, itemCount, lastContentCount)
            recordContentCount()
            val from = fromPosition + header.itemCount
            val to = toPosition + header.itemCount
            this@PageLoopAdapter.notifyItemMoved(from, to)
        }
    }
}

private class ExtraPage(
    private val isHeader: Boolean,
    private val content: PageLoopContent,
    private val adapter: PageLoopAdapter,
    private val updateAnchor: () -> Unit
) {
    private var currentAsItem = false
    private var previousAsItem = currentAsItem

    val itemCount: Int
        get() = if (currentAsItem) content.extraPageLimit else 0

    fun updateItem(payload: Any? = null) {
        currentAsItem = content.canLoop
        // TODO: 一定要更新全部？
        val start = if (isHeader) 0 else adapter.itemCount - itemCount
        when {
            !previousAsItem && currentAsItem -> {
                adapter.notifyItemRangeInserted(start, itemCount)
            }
            previousAsItem && !currentAsItem -> {
                adapter.notifyItemRangeRemoved(start, itemCount)
            }
            previousAsItem && currentAsItem -> {
                adapter.notifyItemRangeChanged(start, itemCount, payload)
            }
        }
        previousAsItem = currentAsItem
    }

    fun onContentRangeChanged(start: Int, count: Int, payload: Any?, lastItemCount: Int) {
    }

    fun onContentRangeInserted(start: Int, count: Int, lastItemCount: Int) {
        when {
            start < 0 -> return
            lastItemCount == 0 -> if (count > 1) updateItem()
            isHeader && start == lastItemCount -> {
                updateItem()
                updateAnchor()
            }
            !isHeader && start == 0 -> {
                updateItem()
                updateAnchor()
            }
        }
        // TODO: 头插和尾插需要更新锚点
    }

    fun onContentRangeRemoved(start: Int, count: Int, lastItemCount: Int) {
    }

    fun onContentRangeMoved(from: Int, to: Int, count: Int, lastItemCount: Int) {
    }
}


