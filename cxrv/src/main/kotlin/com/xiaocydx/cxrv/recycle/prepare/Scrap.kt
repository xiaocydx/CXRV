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

import android.view.View
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.internal.assertMainThread
import java.lang.ref.WeakReference

/**
 * 预创建的结果
 *
 * [viewType]和[count]是初始化阶段配置的预创建数据，
 * [num]表示[value]第几个创建，取值范围为`[1, count]`。
 *
 * @author xcc
 * @date 2023/11/9
 */
class Scrap<T : Any> internal constructor(
    pool: RecycledViewPool,
    val value: T,
    val viewType: Int,
    val count: Int,
    val num: Int
) {
    private val poolRef = WeakReference(pool)

    init {
        ensureAvailable()
    }

    @MainThread
    internal fun putToRecycledViewPool(): Boolean {
        assertMainThread()
        ensureAvailable()
        val pool = poolRef.get() ?: return false
        if (value !is ViewHolder) return false
        val scrapData = pool.mScrap[viewType] ?: run {
            // 触发内部逻辑创建scrapData
            pool.getRecycledViewCount(viewType)
            pool.mScrap.get(viewType)!!
        }
        val scrapHeap = scrapData.mScrapHeap
        return !scrapHeap.contains(value) && scrapHeap.add(value)
    }

    @AnyThread
    private fun ensureAvailable() {
        val hasParent = when (value) {
            is View -> value.parent != null
            is ViewHolder -> value.itemView.parent != null
            else -> false
        }
        require(!hasParent) { "创建的itemView，不能添加到parent中" }
        if (value is ViewHolder) value.mItemViewType = viewType
    }
}