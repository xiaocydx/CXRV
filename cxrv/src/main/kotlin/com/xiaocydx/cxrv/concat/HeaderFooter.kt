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

package com.xiaocydx.cxrv.concat

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.VisibleForTesting
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.SimpleViewHolder

/**
 * 添加[header]
 *
 * 1. 添加[header]之前，若未对RecyclerView设置Adapter，则抛出[IllegalArgumentException]异常。
 * 2. 若在初始化RecyclerView时不添加[header]，而是之后根据情况动态添加[header]，
 * 则可以将Adapter设置为[HeaderFooterConcatAdapter]，之后动态添加[header]的性能更高。
 * ```
 * val contentAdapter: RecyclerView.Adapter<*> = ...
 * recyclerView.adapter = HeaderFooterConcatAdapter(contentAdapter)
 * ```
 *
 * @return 返回[header]转换后的[ViewAdapter]。
 */
fun RecyclerView.addHeader(header: View): ViewAdapter<*> {
    val adapter: Adapter<*> = requireNotNull(adapter) {
        "请先对RecyclerView设置Adapter，然后再添加Header。"
    }
    val headerAdapter: ViewAdapter<*>
    if (adapter is ConcatAdapter) {
        adapter.adapters.firstOrNull {
            it.containView(header)
        }?.let { return it as ViewAdapter<*> }
        headerAdapter = header.toAdapter()
        adapter.addAdapter(0, headerAdapter)
    } else {
        headerAdapter = header.toAdapter()
        swapAdapter(
            HeaderFooterConcatAdapter(adapter, header = headerAdapter),
            /* removeAndRecycleExistingViews */true
        )
    }
    return headerAdapter
}

/**
 * 添加[footer]
 *
 * 1. 添加[footer]之前，若未对RecyclerView设置Adapter，则抛出[IllegalArgumentException]异常。
 * 2. 若在初始化RecyclerView时不添加[footer]，而是之后根据情况动态添加[footer]，
 * 则可以将Adapter设置为[HeaderFooterConcatAdapter]，之后动态添加[footer]的性能更高。
 * ```
 * val contentAdapter: RecyclerView.Adapter<*> = ...
 * recyclerView.adapter = HeaderFooterConcatAdapter(contentAdapter)
 * ```
 *
 * @return 返回[footer]转换后的[ViewAdapter]。
 */
fun RecyclerView.addFooter(footer: View): ViewAdapter<*> {
    val adapter: Adapter<*> = requireNotNull(adapter) {
        "请先对RecyclerView设置Adapter，然后再添加Footer。"
    }
    val footerAdapter: ViewAdapter<*>
    if (adapter is ConcatAdapter) {
        adapter.adapters.lastOrNull {
            it.containView(footer)
        }?.let { return it as ViewAdapter<*> }
        footerAdapter = footer.toAdapter()
        adapter.addAdapter(footerAdapter)
    } else {
        footerAdapter = footer.toAdapter()
        swapAdapter(
            HeaderFooterConcatAdapter(adapter, footer = footerAdapter),
            /* removeAndRecycleExistingViews */true
        )
    }
    return footerAdapter
}

/**
 * 若当前适配器为[ConcatAdapter]且包含[header]，则移除[header]
 *
 * @return 返回[header]转换后的[ViewAdapter]，若[header]未添加或已移除，则返回null。
 */
fun RecyclerView.removeHeader(header: View): ViewAdapter<*>? {
    val concatAdapter = adapter as? ConcatAdapter ?: return null
    val headerAdapter: Adapter<*>? = concatAdapter.adapters
        .firstOrNull { adapter -> adapter.containView(header) }
        ?.also(concatAdapter::removeAdapter)
    return headerAdapter as? ViewAdapter<*>
}

/**
 * 若当前适配器为[ConcatAdapter]且包含[footer]，则移除[footer]
 *
 * @return 返回[footer]转换后的[ViewAdapter]，若[footer]未添加或已移除，则返回null。
 */
fun RecyclerView.removeFooter(footer: View): ViewAdapter<*>? {
    val concatAdapter = adapter as? ConcatAdapter ?: return null
    val footerAdapter: Adapter<*>? = concatAdapter.adapters
        .lastOrNull { adapter -> adapter.containView(footer) }
        ?.also(concatAdapter::removeAdapter)
    return footerAdapter as? ViewAdapter<*>
}

/**
 * 按[header]、当前适配器的顺序连接为[ConcatAdapter]
 */
@CheckResult
infix fun Adapter<*>.withHeader(
    header: ViewAdapter<*>
): ConcatAdapter = when (this) {
    is ConcatAdapter -> apply { addAdapter(0, header) }
    else -> HeaderFooterConcatAdapter(this, header = header)
}

/**
 * 按当前适配器、[footer]的顺序连接为[ConcatAdapter]
 */
@CheckResult
infix fun Adapter<*>.withFooter(
    footer: ViewAdapter<*>
): ConcatAdapter = when (this) {
    is ConcatAdapter -> apply { addAdapter(footer) }
    else -> HeaderFooterConcatAdapter(this, footer = footer)
}

/**
 * 不使用隔离ViewType配置、用于连接Header和Footer的[ConcatAdapter]
 *
 * 若使用隔离ViewType配置，则在共享[RecycledViewPool]的场景下，
 * Header和Footer的隔离ViewType可能会导致获取已回收的ViewHolder抛出异常。
 *
 * ### 场景描述
 * FragmentA和FragmentB共享[RecycledViewPool]，并且内容区Adapter相同。
 *
 * FragmentA有一个Header和内容区Adapter，
 * FragmentA开启了隔离ViewType配置:
 * 1. Header的隔离ViewType = 0。
 * 2. 内容区itemView的隔离ViewType = 1。
 *
 * FragmentB只有内容区Adapter，itemView的ViewType = 0，
 * 从[RecycledViewPool]中可能会取出Header的ViewHolder，
 * Header的ViewHolder和内容区的ViewHolder类型不一致，导致抛出类型转换异常。
 *
 * ### 解决方案
 * 默认不使用隔离ViewType配置，而是将[View.hashCode]作为Header和Footer的ViewType。
 */
@Suppress("FunctionName")
fun HeaderFooterConcatAdapter(
    adapter: Adapter<*>,
    header: ViewAdapter<*>? = null,
    footer: ViewAdapter<*>? = null
): ConcatAdapter {
    val config = ConcatAdapter.Config.Builder()
        .setIsolateViewTypes(false).build()
    return when {
        header == null && footer == null -> ConcatAdapter(config, adapter)
        header == null -> ConcatAdapter(config, adapter, footer)
        footer == null -> ConcatAdapter(config, header, adapter)
        else -> ConcatAdapter(header, adapter, footer)
    }
}

/**
 * 将[View]转换为适配器，用于[ConcatAdapter]的连接场景
 */
fun View.toAdapter(): ViewAdapter<*> = SimpleViewAdapter(this)

private fun Adapter<*>.containView(view: View): Boolean {
    return this is SimpleViewAdapter && this.view === view
}

@VisibleForTesting
internal class SimpleViewAdapter(val view: View) : ViewAdapter<ViewHolder>(currentAsItem = true) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = SimpleViewHolder(view)
        require(!holder.isNotReuseUpdatedViewHolder(parent)) {
            "view转换的ViewAdapter，notifyItemChanged()更新没有重用ViewHolder"
        }
        return holder
    }

    override fun getItemViewType(): Int = view.hashCode()
}