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

package com.xiaocydx.cxrv.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes

/**
 * 创建ViewHolder的作用域
 *
 * @author xcc
 * @date 2024/12/18
 */
interface CreateScope {

    /**
     * 可用于`onCreateViewHolder()`创建itemView
     */
    val ViewGroup.inflater: LayoutInflater
        get() = LayoutInflater.from(context)

    /**
     * 可用于`onCreateViewHolder()`创建itemView
     */
    fun ViewGroup.inflate(@LayoutRes resource: Int): View {
        return inflater.inflate(resource, this, false)
    }

    /**
     * 可用于`onViewRecycled()`清除itemView及其子View的点击、长按监听
     */
    fun View.clearClickListener() {
        setOnClickListener(null)
        setOnLongClickListener(null)
    }
}