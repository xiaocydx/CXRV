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

import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.recycle.prepare.PrepareFlow.ScrapInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 预创建View的入口，可调用[dispatcher]、[inflater]、[frameTimeDeadline]等配置函数
 *
 * 调用者自行决定如何利用预创建结果，例如放入自定义池：
 * ```
 * val customPool: CustomPool = ...
 * recyclerView.prepareView()
 *     .dispatcher(Dispatchers.IO) // 默认为Dispatchers.IO
 *     .inflater(inflater) // 默认为ScrapLayoutInflater
 *     .frameTimeDeadline(adapter) // 将Vsync时间作为预创建的截止时间
 *     .view(viewType1, count, R.layout.item_viewType1) // 构建PrepareFlow<Scrap<View>>
 *     .view(viewType2, count, R.layout.item_viewType2) // 再次构建PrepareFlow<Scrap<View>>
 *     .onEach { scrap -> customPool.put(scrap.viewType, scrap.value) } // 放入自定义池
 *     .launchIn(lifecycleScope)
 * ```
 *
 * **注意**：工作线程开始创建之前，会被临时设置主线程[Looper]，
 * 不会出现View的创建过程因获取不到主线程[Looper]而崩溃的问题。
 */
@CheckResult
fun RecyclerView.prepareView() = PrepareScrap<View>(this)

/**
 * 预创建ViewHolder的入口，可调用[dispatcher]、[inflater]、[frameTimeDeadline]等配置函数
 *
 * 调用者自行决定如何利用预创建结果，例如放入[RecycledViewPool]：
 * ```
 * recyclerView.prepareHolder()
 *     .dispatcher(Dispatchers.IO) // 默认为Dispatchers.IO
 *     .inflater(inflater) // 默认为ScrapLayoutInflater
 *     .frameTimeDeadline(adapter) // 将Vsync时间作为预创建的截止时间
 *     .holder(viewType1, count) {...} // 构建PrepareFlow<Scrap<ViewHolder>>
 *     .holder(viewType2, count) {...} // 再次构建PrepareFlow<Scrap<ViewHolder>>
 *     .putToRecycledViewPool() // 放入RecycledViewPool，该函数更多的描述可以看注释
 *     .launchIn(lifecycleScope)
 * ```
 *
 * **注意**：工作线程开始创建之前，会被临时设置主线程[Looper]，
 * 不会出现View的创建过程因获取不到主线程[Looper]而崩溃的问题。
 */
@CheckResult
fun RecyclerView.prepareHolder() = PrepareScrap<ViewHolder>(this)

/**
 * 预创建的入口，提供配置属性和[PrepareFlow]的构建函数
 */
class PrepareScrap<T : Any> internal constructor(
    private val rv: RecyclerView,
    internal var deadline: PrepareDeadline? = null,
    internal var dispatcher: CoroutineDispatcher = Dispatchers.IO,
    internal var inflaterProvider: (Context) -> LayoutInflater = ::ScrapLayoutInflater
) : PrepareFusible<T>() {

    override fun fusion(scrapInfo: ScrapInfo<T>) = PrepareFlow(
        rv, deadline, inflaterProvider, dispatcher,
        scrapInfoList = listOf(scrapInfo)
    )
}

/**
 * 用于预创建的协程调度器，默认在[Dispatchers.IO]调度的线程创建对象，
 * 通过`Dispatchers.IO.limitedParallelism()`创建并行度受限的调度器，
 * 可以供多处预创建使用。
 */
@CheckResult
fun <T : Any> PrepareScrap<T>.dispatcher(
    dispatcher: CoroutineDispatcher
) = apply { this.dispatcher = dispatcher }

/**
 * 提供用于预创建的[LayoutInflater]，默认实现提供[ScrapLayoutInflater]，
 * 在不能确保支持多线程的情况下，[provider]不该返回`LayoutInflater.from(context)`，
 * 该函数主要是支持设置[LayoutInflater.Factory]和[LayoutInflater.Factory2]的场景。
 */
@CheckResult
fun <T : Any> PrepareScrap<T>.inflater(
    provider: (Context) -> LayoutInflater
) = apply { inflaterProvider = provider }

/**
 * 将Vsync时间作为预创建的截止时间
 *
 * 当截止时间到达时，取消预创建流程，避免创建冗余对象：
 * 1. `adapter.itemCount`初始大于0，将视图树首帧Vsync时间作为预创建的截止时间。
 * 2. `adapter.itemCount`后续大于0，例如数据加载完插入item，将下一帧Vsync时间作为预创建的截止时间。
 */
@CheckResult
fun <T : Any> PrepareScrap<T>.frameTimeDeadline(
    adapter: RecyclerView.Adapter<*>
) = apply { deadline = FrameTimeDeadline(adapter) }