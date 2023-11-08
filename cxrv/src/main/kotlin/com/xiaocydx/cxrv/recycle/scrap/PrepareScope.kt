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

@file:JvmName("PrepareScopeInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.recycle.scrap.PrepareLayoutInflater
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * [RecyclerView.prepareScrap]的初始化作用域
 *
 * @author xcc
 * @date 2023/11/8
 */
class PrepareScope internal constructor(
    private val rv: RecyclerView,
    private val adapter: Adapter<*>
) {
    private val infoList = mutableListOf<ScrapInfo>()
    private var deadline: PrepareDeadline = PrepareDeadline.FOREVER_NS
    private var dispatcher: CoroutineDispatcher = Dispatchers.IO
    private var provider: (Context) -> LayoutInflater = ::PrepareLayoutInflater

    /**
     * 预创建的的截止时间，默认为[PrepareDeadline.FOREVER_NS]，
     * 预创建流程会按[deadline]进行停止，尽可能避免创建多余的ViewHolder。
     */
    fun deadline(deadline: PrepareDeadline) = apply { this.deadline = deadline }

    /**
     * 用于预创建的协程调度器，默认在[Dispatchers.IO]调度的线程创建ViewHolder，
     * 可以通过`Dispatchers.IO.limitedParallelism()`创建一个并行度受限的调度器，
     * 供多处调用该函数的地方使用。
     */
    fun dispatcher(dispatcher: CoroutineDispatcher) = apply { this.dispatcher = dispatcher }

    /**
     * 提供用于预创建的[LayoutInflater]，默认提供[PrepareLayoutInflater]，
     * 在不能确保支持多线程的情况下，[provider]不能返回`LayoutInflater.from(context)`，
     * 该函数主要是支持设置[LayoutInflater.Factory]和[LayoutInflater.Factory2]的场景。
     */
    fun inflater(provider: (Context) -> LayoutInflater) = apply { this.provider = provider }

    /**
     * 调用`adapter.createViewHolder()`，预创建[count]个[viewType]的ViewHolder
     */
    @Deprecated(
        message = "工作线程调用adapter.createViewHolder()创建ViewHolder，" +
                "无法确保不出问题，例如LayoutInflater的注释描述了不支持多线程，" +
                "以及LayoutInflater.Factory和LayoutInflater.Factory2也不一定支持多线程。",
        replaceWith = ReplaceWith("scrap(viewType, count, provider)")
    )
    fun add(viewType: Int, count: Int) {
        scrap(viewType, count) { _, _ -> adapter.createViewHolder(rv, viewType) }
    }

    /**
     * 预创建[count]个[viewType]的ViewHolder
     *
     * ```
     * scrap(viewType, count) { inflater -> ... }
     * ```
     */
    inline fun scrap(
        viewType: Int,
        count: Int,
        crossinline provider: (inflater: Inflater) -> ViewHolder
    ) {
        scrap(viewType, count) { inflater, _ -> provider(inflater) }
    }

    /**
     * 预创建[count]个[viewType]的ViewHolder，[scrap]的简化函数
     *
     * ```
     * scrap(viewType, count, resId, ::ViewHolder)
     * ```
     */
    inline fun scrap(
        viewType: Int,
        count: Int,
        @LayoutRes resId: Int,
        crossinline provider: (View) -> ViewHolder
    ) {
        scrap(viewType, count) { provider(it.inflate(resId)) }
    }

    /**
     * 预创建[count]个[viewType]的ViewHolder，适用于同一个[provider]的场景
     *
     * ```
     * val provider: PrepareScrapProvider = ...
     * scrap(viewType1, count, provider)
     * scrap(viewType2, count, provider)
     * ```
     */
    fun scrap(viewType: Int, count: Int, provider: PrepareScrapProvider) {
        require(count >= 1) { "" }
        infoList.add(ScrapInfo(viewType, count, provider))
    }

    internal fun createConfig() = Config(
        deadline = deadline,
        dispatcher = dispatcher,
        infoList = infoList.toList(),
        inflaterProvider = provider
    )

    internal data class Config(
        val deadline: PrepareDeadline,
        val dispatcher: CoroutineDispatcher,
        val infoList: List<ScrapInfo>,
        val inflaterProvider: (Context) -> LayoutInflater
    )

    internal data class ScrapInfo(
        val viewType: Int,
        val count: Int,
        val provider: PrepareScrapProvider
    )
}

/**
 * 预创建的截止时间
 */
enum class PrepareDeadline {
    /**
     * 没有截止时间
     */
    FOREVER_NS,

    /**
     * 将视图树首帧Vsync时间或者更新时下一帧Vsync时间，作为预创建的截止时间
     */
    FRAME_NS
}