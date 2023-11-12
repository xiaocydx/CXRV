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

@file:Suppress("INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.binding

import android.os.Looper
import androidx.annotation.CheckResult
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.recycle.prepare.PrepareFusible
import com.xiaocydx.cxrv.recycle.prepare.ScrapInflater
import com.xiaocydx.cxrv.recycle.prepare.holder

/**
 * 该函数在工作线程下调用，支持[ScrapInflater]创建[VB]
 *
 * ```
 * recyclerView.prepareView()
 *     .view(viewType, 10) { inflater ->
 *         ViewTypeLayout(inflater.binding(ItemViewType::inflate))
 *     }
 *     .launchIn(lifecycleScope)
 * ```
 */
@CheckResult
@WorkerThread
fun <VB : ViewBinding> ScrapInflater.binding(
    inflate: Inflate<VB>
) = inflate.invoke(inflater, parent, false)

/**
 * 配置[BindingHolder]的预创建数据
 *
 * ```
 * recyclerView.prepareHolder()
 *     .binding(count, provider) {
 *         // 可执行初始化逻辑
 *         root
 *     }
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
 * **注意**：工作线程开始创建之前，会被临时设置主线程[Looper]，
 * 不会出现View的创建过程因获取不到主线程[Looper]而崩溃的问题。
 *
 * @param count    [BindingHolder]的数量
 * @param provider [BindingHolder]的提供者
 * @param onCreateView 工作线程创建[BindingHolder]后，同步调用的函数
 */
@CheckResult
fun <VB : ViewBinding> PrepareFusible<ViewHolder>.binding(
    count: Int,
    provider: BindingAdapter<*, VB>,
    @WorkerThread onCreateView: (VB.() -> Unit)? = null
) = run {
    val inflate = provider.ensureInflate()
    holder(provider.getItemViewType(0), count) {
        val binding = it.binding(inflate)
        val holder = BindingHolder(binding)
        onCreateView?.invoke(binding)
        holder
    }
}

/**
 * 配置[BindingHolder]的预创建数据
 *
 * ```
 * recyclerView.prepareHolder()
 *     .binding(count, provider) {
 *         // 可执行初始化逻辑
 *         root
 *     }
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
 * **注意**：工作线程开始创建之前，会被临时设置主线程[Looper]，
 * 不会出现View的创建过程因获取不到主线程[Looper]而崩溃的问题。
 *
 * @param count    [BindingHolder]的数量
 * @param provider [BindingHolder]的提供者
 * @param onCreateView 工作线程创建[BindingHolder]后，同步调用的函数
 */
fun <VB : ViewBinding> PrepareFusible<ViewHolder>.binding(
    count: Int,
    provider: BindingDelegate<*, VB>,
    @WorkerThread onCreateView: (VB.() -> Unit)? = null
) = run {
    val inflate = provider.ensureInflate()
    holder(provider.viewType, count) {
        val binding = it.binding(inflate)
        val holder = BindingHolder(binding)
        onCreateView?.invoke(binding)
        holder
    }
}