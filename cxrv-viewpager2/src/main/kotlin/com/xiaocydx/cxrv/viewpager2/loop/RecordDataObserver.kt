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
 * @author xcc
 * @date 2023/5/11
 */
internal abstract class RecordDataObserver(private val adapter: Adapter<*>) : AdapterDataObserver() {
    protected var lastItemCount = adapter.itemCount
        private set

    protected fun recordItemCount() {
        lastItemCount = adapter.itemCount
    }

    final override fun onChanged() {
        changed()
        recordItemCount()
    }

    final override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        if (positionStart >= 0) onItemRangeChanged(positionStart, itemCount, null)
    }

    final override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        if (positionStart >= 0) itemRangeChanged(positionStart, itemCount, payload)
        recordItemCount()
    }

    final override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (positionStart >= 0) itemRangeInserted(positionStart, itemCount)
        recordItemCount()
    }

    final override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (positionStart >= 0) itemRangeRemoved(positionStart, itemCount)
        recordItemCount()
    }

    final override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if (fromPosition >= 0 && toPosition >= 0) {
            itemRangeMoved(fromPosition, toPosition, itemCount)
        }
        recordItemCount()
    }

    protected open fun changed() = Unit
    protected open fun itemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = Unit
    protected open fun itemRangeInserted(positionStart: Int, itemCount: Int) = Unit
    protected open fun itemRangeRemoved(positionStart: Int, itemCount: Int) = Unit
    protected open fun itemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = Unit
}