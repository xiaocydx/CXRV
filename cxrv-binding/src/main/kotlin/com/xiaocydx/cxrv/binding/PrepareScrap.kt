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

import androidx.annotation.AnyThread
import androidx.annotation.CheckResult
import androidx.annotation.WorkerThread
import androidx.viewbinding.ViewBinding
import com.xiaocydx.cxrv.internal.InternalVisibleApi
import com.xiaocydx.cxrv.recycle.prepare.PrepareScrap
import com.xiaocydx.cxrv.recycle.prepare.PrepareScrapFlow
import com.xiaocydx.cxrv.recycle.prepare.ScrapInflater
import com.xiaocydx.cxrv.recycle.prepare.ScrapProvider
import com.xiaocydx.cxrv.recycle.prepare.holder

@CheckResult
@WorkerThread
fun <VB : ViewBinding> ScrapInflater.binding(
    inflate: Inflate<VB>
) = inflate.invoke(real, parent, false)

@CheckResult
@OptIn(InternalVisibleApi::class)
inline fun <VB : ViewBinding> PrepareScrap<BindingHolder<VB>>.bindingHolder(
    provider: BindingScrapProvider<VB>,
    count: Int,
    @WorkerThread crossinline onCreateView: VB.() -> Unit = {}
) = holder(provider.scrapType, count) {
    provider.onCreateScrap(it).apply { onCreateView(binding) }
}

@CheckResult
@OptIn(InternalVisibleApi::class)
inline fun <VB : ViewBinding> PrepareScrapFlow<BindingHolder<VB>>.bindingHolder(
    provider: BindingScrapProvider<VB>,
    count: Int,
    @WorkerThread crossinline onCreateView: VB.() -> Unit = {}
) = holder(provider.scrapType, count) {
    provider.onCreateScrap(it).apply { onCreateView(binding) }
}

interface BindingScrapProvider<VB : ViewBinding> : ScrapProvider<BindingHolder<VB>> {
    @get:AnyThread
    @property:InternalVisibleApi
    val scrapType: Int

    @get:WorkerThread
    @property:InternalVisibleApi
    val scrapInflate: Inflate<VB>

    @OptIn(InternalVisibleApi::class)
    override fun onCreateScrap(inflater: ScrapInflater) = BindingHolder(inflater.binding(scrapInflate))
}