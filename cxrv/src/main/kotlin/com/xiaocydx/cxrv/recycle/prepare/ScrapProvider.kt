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

/**
 * 预创建的结果提供者
 *
 * @author xcc
 * @date 2023/11/9
 */
fun interface ScrapProvider<out T : Any> {

    /**
     * 在工作线程下调用该函数，通过[ScrapInflater.inflate]创建View或ViewHolder
     *
     * @param inflater 提供[ScrapInflater.context]和[ScrapInflater.inflate]
     */
    @WorkerThread
    fun onCreateScrap(inflater: ScrapInflater): T
}

@WorkerThread
class ScrapInflater internal constructor(
    internal val parent: RecyclerView,
    internal val inflater: LayoutInflater,
    internal val scrapContext: ScrapContext,
    internal val scrapParent: Lazy<ScrapParent>,
    val viewType: Int
) {
    val context: Context
        get() = scrapContext

    init {
        assert(inflater.context === scrapContext)
    }

    @CheckResult
    fun inflate(@LayoutRes resId: Int): View {
        return inflater.inflate(resId, parent, false)
    }

    internal inline fun <R> with(block: (ScrapInflater) -> R): R {
        // inflater.context为scrapContext，构建View的四种情况:
        // 1. inflater.inflate() -> View(scrapContext)
        // 2. 代码构建 -> View(context) -> View(scrapContext)
        // 3. LayoutInflater.from(scrapParent.context) -> inflater.inflate() -> View(scrapContext)
        // 4. 代码构建 -> View(scrapParent.context) -> View(scrapContext)
        scrapContext.setInflater(inflater)
        return try {
            block(this)
        } finally {
            scrapContext.clearInflater()
        }
    }
}