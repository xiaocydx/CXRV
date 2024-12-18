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

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.DiffUtil

/**
 * 配置差异计算的作用域
 *
 * @author xcc
 * @date 2024/12/18
 */
interface DiffScope<ITEM : Any> {

    /**
     * 对应[DiffUtil.ItemCallback.areItemsTheSame]
     *
     * [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     *
     * 确定局部更新的类型，通常对比item的`key`即可，如果[oldItem]和[newItem]的`key`不一样，
     * 函数返回`false`，那么[oldItem]是remove更新，[newItem]是insert更新，不会是change更新或move更新。
     */
    @MainThread
    @WorkerThread
    fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean

    /**
     * 对应[DiffUtil.ItemCallback.areContentsTheSame]
     *
     * 1. [areItemsTheSame]返回true -> 调用[areContentsTheSame]。
     * 2. [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     *
     * 确定不是remove和insert更新后，再确定是否为change更新，返回`false`表示change更新，
     * 默认实现是[oldItem]和[newItem]进行`equals()`对比，推荐数据实体使用data class。
     */
    @MainThread
    @WorkerThread
    fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = oldItem == newItem

    /**
     * 对应[DiffUtil.ItemCallback.getChangePayload]
     *
     * 1. [areItemsTheSame]返回true -> [areContentsTheSame]返回false -> 调用[getChangePayload]。
     * 2. [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     *
     * 确定是change更新后，再获取Payload对象，默认实现是返回`null`。
     */
    @MainThread
    fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? = null
}

internal fun <ITEM : Any> DiffScope<ITEM>.asDiffItemCallback(): DiffUtil.ItemCallback<ITEM> {
    return object : DiffUtil.ItemCallback<ITEM>() {
        override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@asDiffItemCallback.areItemsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@asDiffItemCallback.areContentsTheSame(oldItem, newItem)
        }

        override fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? {
            return this@asDiffItemCallback.getChangePayload(oldItem, newItem)
        }
    }
}