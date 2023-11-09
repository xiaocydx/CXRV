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

@file:JvmName("ScrapInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.internal.assertMainThread

/**
 * @author xcc
 * @date 2023/11/9
 */
class Scrap<T : Any> internal constructor(
    val value: T,
    val viewType: Int,
    val num: Int,
    val count: Int
) {

    @MainThread
    internal fun tryPutToRecycledViewPool(pool: RecycledViewPool) {
        assertMainThread()
        if (value !is ViewHolder) return
        require(value.itemView.parent == null) {
            "创建ViewHolder.itemView时，不能添加到parent中"
        }
        value.mItemViewType = viewType
        val scrapData = pool.mScrap[viewType] ?: run {
            // 触发内部逻辑创建scrapData
            pool.getRecycledViewCount(viewType)
            pool.mScrap.get(viewType)!!
        }
        scrapData.mScrapHeap.takeIf { !it.contains(value) }?.add(value)
    }
}