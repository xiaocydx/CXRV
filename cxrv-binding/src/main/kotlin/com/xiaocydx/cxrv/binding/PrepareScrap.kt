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
import com.xiaocydx.cxrv.recycle.scrap.PrepareScrap
import com.xiaocydx.cxrv.recycle.scrap.PrepareScrapFlow
import com.xiaocydx.cxrv.recycle.scrap.ScrapInflater
import com.xiaocydx.cxrv.recycle.scrap.ScrapProvider
import com.xiaocydx.cxrv.recycle.scrap.viewHolder

interface BindingScrapProvider<VB : ViewBinding> : ScrapProvider<BindingHolder<VB>> {
    @get:AnyThread
    val scrapType: Int

    @get:WorkerThread
    val scrapInflate: Inflate<VB>

    override fun onCreateScrap(inflater: ScrapInflater): BindingHolder<VB> {
        return BindingHolder(scrapInflate.invoke(inflater.real, inflater.parent, false))
    }
}

@CheckResult
fun <VB : ViewBinding> PrepareScrap<BindingHolder<VB>>.bindingHolder(
    provider: BindingScrapProvider<VB>, count: Int
) = viewHolder(provider.scrapType, count, provider)

@CheckResult
fun <VB : ViewBinding> PrepareScrapFlow<BindingHolder<VB>>.bindingHolder(
    provider: BindingScrapProvider<VB>, count: Int
) = viewHolder(provider.scrapType, count, provider)