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
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter

/**
 * 预创建的结果提供者
 *
 * 其中一种实现方式是让[Adapter]实现该接口，抽离出共同的初始化逻辑，例如：
 * 1.实现[ScrapProvider]
 * ```
 * class FooAdapter : RecyclerView.Adapter<ViewHolder>(), ScrapProvider<ViewHolder> {
 *      // 共同的viewType转resId逻辑
 *      private fun Int.toResId() = ...
 *
 *      // 共同的ViewHolder初始化逻辑
 *      private fun ViewHolder.init() = apply {...}
 *
 *      override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
 *          val resId = viewType.toResId()
 *          val itemView = LayoutInflater.from(parent.context).inflate(resId, parent, false)
 *          return ViewHolder(itemView).init()
 *      }
 *
 *      override fun onCreateScrap(inflater: ScrapInflater): ViewHolder {
 *          val resId = inflater.viewType.toResId()
 *          val itemView = inflater.inflate(resId)
 *          return ViewHolder(itemView).init()
 *      }
 * }
 * ```
 *
 * 2.初始化预创建
 * ```
 * val fooAdapter: FooAdapter = ...
 * recyclerView.prepareHolder()
 *     .holder(viewType, count, fooAdapter)
 *     .putToRecycledViewPool()
 *     .launchIn(lifecycleScope)
 * ```
 *
 * @author xcc
 * @date 2023/11/9
 */
fun interface ScrapProvider<out T : Any> {

    /**
     * 该函数在工作线程下调用，通过[ScrapInflater.inflate]创建View或ViewHolder
     *
     * @param inflater 仅提供[ScrapInflater.context]和[ScrapInflater.inflate]
     */
    @WorkerThread
    fun onCreateScrap(inflater: ScrapInflater): T
}

/**
 * [real]的默认实现是[ScrapInflater]，确保只在一个线程访问[LayoutInflater]
 */
@WorkerThread
class ScrapInflater internal constructor(
    internal val parent: RecyclerView,
    internal val real: LayoutInflater,
    val viewType: Int
) {
    val context: Context
        get() = parent.context

    @CheckResult
    fun inflate(@LayoutRes resId: Int): View {
        return real.inflate(resId, parent, false)
    }
}