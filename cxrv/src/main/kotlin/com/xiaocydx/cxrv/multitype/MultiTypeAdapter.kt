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

package com.xiaocydx.cxrv.multitype

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.getItem

/**
 * 多类型适配器
 *
 * @author xcc
 * @date 2021/10/8
 */
open class MultiTypeAdapter<T : Any>
@PublishedApi internal constructor(
    private var multiType: MultiType<T> = unregistered()
) : ListAdapter<T, ViewHolder>() {

    init {
        setMultiType(multiType)
    }

    fun setMultiType(multiType: MultiType<T>) {
        if (multiType === unregistered<T>()) return
        this.multiType = multiType
        multiType.forEach { it.delegate.attachAdapter(this) }
    }

    fun getType(viewType: Int) = multiType.keyAt(viewType)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return multiType.getViewTypeDelegate(viewType).onCreateViewHolder(parent)
    }

    override fun getItemViewType(position: Int): Int {
        return multiType.getItemViewType(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, item: T, payloads: List<Any>) {
        multiType.getViewTypeDelegate(holder).onBindViewHolder(holder, item, payloads)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: T) {
        multiType.getViewTypeDelegate(holder).onBindViewHolder(holder, item)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        val delegate = multiType.getViewTypeDelegate(holder)
        val maxScrap = delegate.consumeMaxScrap()
        if (maxScrap > 0) {
            // getViewTypeDelegate()的查找过程未抛出异常，
            // delegate.viewType等于holder.itemViewType。
            val pool = recyclerView?.recycledViewPool
            pool?.setMaxRecycledViews(delegate.viewType, maxScrap)
        }
        delegate.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        return multiType.getViewTypeDelegate(holder).onFailedToRecycleView(holder)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        multiType.getViewTypeDelegate(holder).onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        multiType.getViewTypeDelegate(holder).onViewDetachedFromWindow(holder)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        multiType.forEach { it.delegate.onAttachedToRecyclerView(recyclerView) }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        multiType.forEach { it.delegate.onDetachedFromRecyclerView(recyclerView) }
    }

    override fun fullSpan(position: Int, holder: ViewHolder): Boolean {
        return multiType.getViewTypeDelegate(holder).fullSpan(position, holder)
    }

    override fun getSpanSize(position: Int, spanCount: Int): Int {
        val viewType = getItemViewType(position)
        return multiType.getViewTypeDelegate(viewType).getSpanSize(position, spanCount)
    }

    override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = with(multiType) {
        // 当两个item的Class不同时，viewType也不同，属于一对一关系,
        // 以替换item为例，此时会以removeAndInsert的方式更新列表。
        if (oldItem.javaClass != newItem.javaClass) return false

        // 以下是对一对多关系的处理
        val oldViewType = getItemViewType(oldItem)
        val newViewType = getItemViewType(newItem)

        // 当两个item的viewType相同时，不需要再调用一次areItemsTheSame()
        val oldTheSame = getViewTypeDelegate(oldViewType).areItemsTheSame(oldItem, newItem)
        if (oldViewType == newViewType) return oldTheSame

        // 当两个item的viewType不同时，支持不同的areItemsTheSame()实现
        oldTheSame && getViewTypeDelegate(newViewType).areItemsTheSame(oldItem, newItem)
    }

    override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = with(multiType) {
        val oldViewType = getItemViewType(oldItem)
        val newViewType = getItemViewType(newItem)
        // 当两个item的viewType不同时，不需要调用areContentsTheSame()和getChangePayload()，
        // 因为Recycler.tryGetViewHolderForPositionByDeadline()会回收oldViewType的holder，
        // 填充newViewType的holder，这是一个全量更新的过程，不会因为payload重用holder而改变。
        oldViewType == newViewType && getViewTypeDelegate(oldViewType).areContentsTheSame(oldItem, newItem)
    }

    override fun getChangePayload(oldItem: T, newItem: T): Any? = with(multiType) {
        val viewType = getItemViewType(oldItem)
        getViewTypeDelegate(viewType).getChangePayload(oldItem, newItem)
    }
}