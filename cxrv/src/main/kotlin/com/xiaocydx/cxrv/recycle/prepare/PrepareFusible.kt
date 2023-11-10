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

package com.xiaocydx.cxrv.recycle.prepare

import android.os.Looper
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.Scrap
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.recycle.prepare.PrepareFlow.ScrapInfo
import kotlinx.coroutines.flow.Flow

/**
 * 融合[ScrapInfo]构建[PrepareFlow]，实现类有且仅有[PrepareScrap]和[PrepareFlow]
 *
 * @author xcc
 * @date 2023/11/10
 */
sealed class PrepareFusible<T : Any> {
    internal abstract fun fusion(scrapInfo: ScrapInfo<T>): PrepareFlow<T>
}

/**
 * 配置View的预创建数据
 *
 * ```
 * recyclerView.prepareView()
 *     .view(viewType1, count, R.layout.item_viewType1)
 *     .view(viewType2, count, R.layout.item_viewType2)
 *     .launchIn(lifecycleScope)
 * ```
 *
 * 若对RecyclerView设置了[ConcatAdapter]，则不应当使用隔离ViewType配置，
 * 因为隔离ViewType配置可能导致无法从[RecycledViewPool]获取到预创建结果，
 * 或者取到预创建结果，但是抛出类型转换异常，可以看[Concat.concat]的注释。
 * ```
 * // ConcatAdapter不应当使用隔离ViewType配置
 * recyclerView.adapter = concatAdapter
 * ```
 *
 * **注意**：工作线程开始创建之前，会被临时设置主线程[Looper]，
 * 不会出现View的创建过程因获取不到主线程[Looper]而崩溃的问题。
 *
 * @param viewType View的类型
 * @param count    创建的数量
 * @param resId    View的资源id
 */
@CheckResult
fun PrepareFusible<View>.view(
    viewType: Int, count: Int, @LayoutRes resId: Int
) = view(viewType, count) { it.inflate(resId) }

/**
 * 配置View的预创建数据
 *
 * ```
 * recyclerView.prepareView()
 *     .view(viewType1, count) { inflater ->
 *         ViewType1Layout(inflater.context) // 代码创建View
 *     }
 *     .view(viewType2, count) { inflater ->
 *         inflater.inflate(R.layout.item_viewType2) // 解析创建View
 *     }
 *     .launchIn(lifecycleScope)
 * ```
 *
 * 若对RecyclerView设置了[ConcatAdapter]，则不应当使用隔离ViewType配置，
 * 因为隔离ViewType配置可能导致无法从[RecycledViewPool]获取到预创建结果，
 * 或者取到预创建结果，但是抛出类型转换异常，可以看[Concat.concat]的注释。
 * ```
 * // ConcatAdapter不应当使用隔离ViewType配置
 * recyclerView.adapter = concatAdapter
 * ```
 *
 * **注意**：工作线程开始创建之前，会被临时设置主线程[Looper]，
 * 不会出现View的创建过程因获取不到主线程[Looper]而崩溃的问题。
 *
 * @param viewType View的类型
 * @param count    View的数量
 * @param provider View的提供者，[ScrapProvider.onCreateScrap]在工作线程下调用
 */
@CheckResult
fun PrepareFusible<View>.view(
    viewType: Int, count: Int, provider: ScrapProvider<View>
) = fusion(viewType, count, provider)

/**
 * 配置ViewHolder的预创建数据
 *
 * ```
 * recyclerView.prepareHolder()
 *     .holder(viewType1, count) { inflater ->
 *         ViewType1Holder(inflater.inflate(R.layout.item_viewType1))
 *     }
 *     .holder(viewType2, count) { inflater ->
 *         ViewType2Holder(inflater.inflate(R.layout.item_viewType2))
 *     }
 *     .launchIn(lifecycleScope)
 * ```
 *
 * 若对RecyclerView设置了[ConcatAdapter]，则不应当使用隔离ViewType配置，
 * 因为隔离ViewType配置可能导致无法从[RecycledViewPool]获取到预创建结果，
 * 或者取到预创建结果，但是抛出类型转换异常，可以看[Concat.concat]的注释。
 * ```
 * // ConcatAdapter不应当使用隔离ViewType配置
 * recyclerView.adapter = concatAdapter
 * ```
 *
 * **注意**：工作线程开始创建之前，会被临时设置主线程[Looper]，
 * 不会出现View的创建过程因获取不到主线程[Looper]而崩溃的问题。
 *
 * @param viewType ViewHolder的类型
 * @param count    ViewHolder的的数量
 * @param provider ViewHolder的提供者，[ScrapProvider.onCreateScrap]在工作线程下调用
 */
@CheckResult
fun PrepareFusible<ViewHolder>.holder(
    viewType: Int, count: Int, provider: ScrapProvider<ViewHolder>
) = fusion(viewType, count, provider)

/**
 * 将预创建的ViewHolder放入[RecycledViewPool]
 *
 * ```
 * recyclerView.prepareHolder()
 *     .holder(viewType1, count) {...}
 *     .holder(viewType2, count) {...}
 *     .putToRecycledViewPool() // 放入RecycledViewPool
 *     .launchIn(lifecycleScope)
 * ```
 *
 * 若对RecyclerView设置了[ConcatAdapter]，则不应当使用隔离ViewType配置，
 * 因为隔离ViewType配置可能导致无法从[RecycledViewPool]获取到预创建结果，
 * 或者取到预创建结果，但是抛出类型转换异常，可以看[Concat.concat]的注释。
 * ```
 * // ConcatAdapter不应当使用隔离ViewType配置
 * recyclerView.adapter = concatAdapter
 * ```
 *
 * **注意**：内部实现会忽略[RecycledViewPool]的回收上限，确保将全部预创建的ViewHolder放入回收池，
 * 这种处理方式跟预创建本身的意图有关，预创建本意是提前创建一批ViewHolder，随后布局流程取走使用，
 * 因此放入过程不应当被回收上限所限制。
 */
@CheckResult
fun PrepareFlow<ViewHolder>.putToRecycledViewPool(): Flow<Scrap<ViewHolder>> {
    return recreate(putScrapToRecycledViewPool = true)
}

private fun <T : Any> PrepareFusible<T>.fusion(
    viewType: Int, count: Int, provider: ScrapProvider<T>
) = fusion(ScrapInfo(
    viewType = viewType,
    count = count,
    provider = { inflater, num ->
        val value = provider.onCreateScrap(inflater)
        Scrap(value, viewType = inflater.viewType, count = count, num = num)
    }
))