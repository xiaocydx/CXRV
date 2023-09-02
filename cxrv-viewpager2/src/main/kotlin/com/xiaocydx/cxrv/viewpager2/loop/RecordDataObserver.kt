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

package com.xiaocydx.cxrv.viewpager2.loop

import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver

/**
 * 在每次更新时，记录`adapter.itemCount`为[lastItemCount]
 *
 * @author xcc
 * @date 2023/5/11
 */
internal class RecordDataObserver(private val adapter: Adapter<*>) : AdapterDataObserver() {
    var lastItemCount = adapter.itemCount
        private set

    private fun reset() {
        // 用注释提醒调用者，确保是先修改数据，再调用notifyDataSetChanged()的顺序，
        // 基于这种顺序，才能在notifyDataSetChanged()分发过程得到最新的itemCount。
        lastItemCount = adapter.itemCount
    }

    private fun record(diff: Int = 0) {
        // 对于差异计算，lastItemCount不能直接重置为adapter.itemCount，
        // 因为差异计算完成后，是先替换数据源，再通知RecyclerView局部更新，
        // 此时不是修改一次数据，就通知一次RecyclerView局部更新的理想顺序，
        // 即使不考虑差异计算，让调用者遵守并理解这种调用顺序，也是一种负担，
        // 因此，对于修改lastItemCount的局部更新，靠计算得出新的itemCount，
        // 确保所有局部更新的分发过程，都有previous和current的概念。
        lastItemCount = (lastItemCount + diff).coerceAtLeast(0)
    }

    override fun onChanged() = reset()
    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = record()
    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = record()
    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = record(diff = itemCount)
    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = record(diff = -itemCount)
    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = record()
}