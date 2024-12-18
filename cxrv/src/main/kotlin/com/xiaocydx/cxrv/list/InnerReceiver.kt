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

package com.xiaocydx.cxrv.list

import android.annotation.SuppressLint
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.clearPendingUpdates

/**
 * 在OutRecyclerView嵌套InnerRecyclerView场景下，将[ListAdapter]作为Inner使用
 *
 * ```
 * innerRecyclerView = innerListAdapter
 * innerListAdapter.asInner().replaceList(newList)
 * ```
 */
fun <T : Any> ListAdapter<T, *>.asInner() = InnerReceiver(this)

@JvmInline
value class InnerReceiver<ITEM : Any>
@PublishedApi internal constructor(
    private val adapter: ListAdapter<ITEM, *>
) {

    /**
     * 在`OutRecyclerView.adapter`的`onBindViewHolder()`下替换列表：
     * ```
     * fun onBindViewHolder(holder: ViewHolder, position: Int) {
     *     val innerList = getItem(position).innerList
     *     innerListAdapter.asInner().replaceList(innerList)
     * }
     * ```
     *
     * 该函数对替换列表做了兼容，使得InnerRecyclerView的[LayoutManager.collectInitialPrefetchPositions]生效。
     * 可以调用[LinearLayoutManager.setInitialPrefetchItemCount]设置InnerRecyclerView的预取数量，默认数量为2。
     */
    @SuppressLint("NotifyDataSetChanged")
    fun replaceList(newList: List<ITEM>) = with(adapter) {
        // 取消正在进行的差异计算
        adapter.cancel()
        // 清除oldList，同时添加remove更新操作
        adapter.clear()
        // 添加newList，同时添加add更新操作
        adapter.submitList(newList)

        val rv = recyclerView ?: return
        // 清除remove和add更新操作，使得GapWorker判断if(!hasPendingUpdates())通过：
        /*
           void collectPrefetchPositionsFromView(RecyclerView view, boolean nested) {
                ...
                if (!view.mAdapterHelper.hasPendingUpdates()) {
                    layout.collectInitialPrefetchPositions(view.mAdapter.getItemCount(), this);
                }
                ...
           }
         */
        rv.clearPendingUpdates()
        // 已清除remove和add更新操作，补充全量更新
        notifyDataSetChanged()
    }
}