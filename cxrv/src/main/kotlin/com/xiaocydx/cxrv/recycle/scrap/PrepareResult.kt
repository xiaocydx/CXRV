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

@file:JvmName("PrepareResultInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.util.SparseIntArray
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.xiaocydx.cxrv.internal.assertMainThread
import kotlinx.coroutines.channels.Channel

/**
 * [RecyclerView.prepareScrap]的结果，提供数量查询函数，可用于数据统计或者单元测试
 *
 * @author xcc
 * @date 2023/11/8
 */
@MainThread
class PrepareResult internal constructor(
    initialCapacity: Int,
    private val recycledViewPool: RecycledViewPool
) {
    private val preparedScrapCount: SparseIntArray?
    internal val channelCapacity: Int

    init {
        @Suppress("INVISIBLE_MEMBER")
        var capacity = Channel.CHANNEL_DEFAULT_CAPACITY
        if (initialCapacity > capacity) capacity = Channel.UNLIMITED
        channelCapacity = capacity
        preparedScrapCount = if (initialCapacity > 0) SparseIntArray() else null
    }

    /**
     * 获取[viewType]在[RecycledViewPool]的回收数量
     *
     * 回收数量不一定等于[getPreparedScrapCount]，可能布局流程已取走一部分进行填充，
     * 又或者在执行预创建流程之前，[RecyclerView]已存在ViewHolder，例如共享池场景。
     */
    fun getRecycledScrapCount(viewType: Int): Int {
        assertMainThread()
        return recycledViewPool.getRecycledViewCount(viewType)
    }

    /**
     * 获取[viewType]在[RecycledViewPool]的预创建数量
     */
    fun getPreparedScrapCount(viewType: Int): Int {
        assertMainThread()
        return preparedScrapCount?.get(viewType) ?: 0
    }

    internal fun putScrapToRecycledViewPool(scrap: RecyclerView.ViewHolder) {
        assertMainThread()
        recycledViewPool.putScrap(scrap)
        preparedScrapCount?.apply {
            val count = get(scrap.itemViewType)
            put(scrap.itemViewType, count + 1)
        }
    }

    private fun RecycledViewPool.putScrap(scrap: RecyclerView.ViewHolder) {
        val viewType = scrap.itemViewType
        val scrapData = mScrap[viewType] ?: kotlin.run {
            // 触发内部逻辑创建scrapData
            getRecycledViewCount(viewType)
            mScrap.get(viewType)!!
        }
        scrapData.mScrapHeap.add(scrap)
    }
}