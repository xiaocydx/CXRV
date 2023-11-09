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

import android.view.LayoutInflater
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView

/**
 * @author xcc
 * @date 2023/11/9
 */
@WorkerThread
fun interface ScrapProvider<T : Any> {

    /**
     * 通过[ScrapInflater.inflate]创建`itemView`
     * // TODO: 2023/11/8 更容易的实现创建逻辑聚集
     */
    fun onCreateScrap(inflater: ScrapInflater): T
}

@WorkerThread
class ScrapInflater internal constructor(
    internal val parent: RecyclerView,
    internal val real: LayoutInflater,
    val viewType: Int
) {

    @CheckResult
    fun inflate(@LayoutRes resId: Int): View {
        return real.inflate(resId, parent, false)
    }
}