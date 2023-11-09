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

package com.xiaocydx.cxrv.recycle.scrap

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.PrepareDeadline
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.Scrap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@CheckResult
fun <VH : ViewHolder> RecyclerView.prepareScrap(
    adapter: Adapter<VH>
) = PrepareScrap(this, adapter)

class PrepareScrap<VH : ViewHolder> internal constructor(
    internal val rv: RecyclerView,
    internal val adapter: Adapter<VH>,
) {
    internal var deadline: PrepareDeadline = PrepareDeadline.FOREVER_NS
    internal var dispatcher: CoroutineDispatcher = Dispatchers.IO
    internal var inflaterProvider: (Context) -> LayoutInflater = ::PrepareLayoutInflater

    /**
     * 预创建的的截止时间，默认为[PrepareDeadline.FOREVER_NS]，
     * 预创建流程会按[deadline]进行停止，尽可能避免创建多余的ViewHolder。
     */
    @CheckResult
    fun deadline(deadline: PrepareDeadline) = apply { this.deadline = deadline }

    /**
     * 用于预创建的协程调度器，默认在[Dispatchers.IO]调度的线程创建ViewHolder，
     * 可以通过`Dispatchers.IO.limitedParallelism()`创建一个并行度受限的调度器，
     * 供多处调用该函数的地方使用。
     */
    @CheckResult
    fun dispatcher(dispatcher: CoroutineDispatcher) = apply { this.dispatcher = dispatcher }

    /**
     * 提供用于预创建的[LayoutInflater]，默认提供[PrepareLayoutInflater]，
     * 在不能确保支持多线程的情况下，[provider]不能返回`LayoutInflater.from(context)`，
     * 该函数主要是支持设置[LayoutInflater.Factory]和[LayoutInflater.Factory2]的场景。
     */
    @CheckResult
    fun inflater(provider: (Context) -> LayoutInflater) = apply { inflaterProvider = provider }
}

//region View
@CheckResult
fun PrepareScrap<*>.view(
    viewType: Int, count: Int, @LayoutRes resId: Int
) = view(viewType, count) { it.inflate(resId) }

@CheckResult
fun PrepareScrap<*>.view(
    viewType: Int, count: Int, provider: ScrapProvider<View>
) = PrepareScrapFlow<View>(rv, adapter, deadline, dispatcher, inflaterProvider)
    .fusion(viewType, count, provider.tPrepareScrapProvider(count))

fun PrepareScrapFlow<View>.view(
    viewType: Int, count: Int, @LayoutRes resId: Int
) = view(viewType, count) { it.inflate(resId) }

fun PrepareScrapFlow<View>.view(
    viewType: Int, count: Int, provider: ScrapProvider<View>
) = fusion(viewType, count, provider)
//endregion

//region ViewHolder
@CheckResult
fun <VH : ViewHolder> PrepareScrap<VH>.viewHolder(
    viewType: Int, count: Int, provider: ScrapProvider<VH>
) = PrepareScrapFlow<VH>(rv, adapter, deadline, dispatcher, inflaterProvider)
    .fusion(viewType, count, provider.tPrepareScrapProvider(count))

fun <VH : ViewHolder> PrepareScrapFlow<VH>.viewHolder(
    viewType: Int, count: Int, provider: ScrapProvider<VH>
) = fusion(viewType, count, provider)
//endregion

private fun <T : Any> ScrapProvider<T>.tPrepareScrapProvider(count: Int): PrepareScrapProvider<T> =
        { inflater, num -> Scrap(onCreateScrap(inflater), inflater.viewType, num, count, inflater.pool) }

private fun <T : Any> PrepareScrapFlow<T>.fusion(viewType: Int, count: Int, provider: ScrapProvider<T>) =
        fusion(viewType, count, provider.tPrepareScrapProvider(count))