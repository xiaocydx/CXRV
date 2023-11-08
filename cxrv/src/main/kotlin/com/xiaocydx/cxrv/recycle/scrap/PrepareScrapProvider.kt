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
@file:JvmName("PrepareScrapProviderInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.annotation.WorkerThread
import androidx.core.os.TraceCompat
import androidx.recyclerview.widget.RecyclerView.TRACE_CREATE_VIEW_TAG
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * [RecyclerView.prepareScrap]的ViewHolder提供者
 *
 * @author xcc
 * @date 2023/11/8
 */
@WorkerThread
fun interface PrepareScrapProvider {

    /**
     * 通过[Inflater.inflate]创建`itemView`，进而创建ViewHolder
     * // TODO: 2023/11/8 更容易的实现创建逻辑聚集
     */
    fun onCreateViewHolder(inflater: Inflater, viewType: Int): ViewHolder
}

/**
 * 约束在工作线程中对[parent]和[inflater]的函数调用
 */
@WorkerThread
class Inflater internal constructor(
    internal val parent: ViewGroup,
    internal val inflater: LayoutInflater,
) {
    fun inflate(@LayoutRes resId: Int): View {
        return inflater.inflate(resId, parent, false)
    }
}

@WorkerThread
internal fun PrepareScrapProvider.createViewHolder(inflater: Inflater, viewType: Int): ViewHolder {
    return try {
        TraceCompat.beginSection(TRACE_CREATE_VIEW_TAG)
        val holder = onCreateViewHolder(inflater, viewType)
        check(holder.itemView.parent == null) {
            ("ViewHolder views must not be attached when"
                    + " created. Ensure that you are not passing 'true' to the attachToRoot"
                    + " parameter of LayoutInflater.inflate(..., boolean attachToRoot)")
        }
        holder.mItemViewType = viewType
        holder
    } finally {
        TraceCompat.endSection()
    }
}