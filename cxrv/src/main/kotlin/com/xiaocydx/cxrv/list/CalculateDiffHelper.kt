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

import androidx.recyclerview.widget.DiffUtil

/**
 * 计算两个列表的差异
 *
 * @author xcc
 * @date 2021/9/17
 */
internal fun <T> List<T>.calculateDiff(
    newList: List<T>,
    diffCallback: DiffUtil.ItemCallback<T>
): DiffUtil.DiffResult {
    val oldList = this
    return DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return if (oldItem != null && newItem != null) {
                diffCallback.areItemsTheSame(oldItem, newItem)
            } else {
                oldItem == null && newItem == null
            }
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return when {
                oldItem != null && newItem != null -> {
                    diffCallback.areContentsTheSame(oldItem, newItem)
                }
                oldItem == null && newItem == null -> true
                else -> {
                    // 仅当areItemsTheSame()返回true时，才会调用areContentsTheSame()，
                    // 若areContentsTheSame()执行到该分支，则表示出现断言异常。
                    throw AssertionError("areContentsTheSame()出现断言异常")
                }
            }
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return if (oldItem != null && newItem != null) {
                diffCallback.getChangePayload(oldItem, newItem)
            } else {
                // 仅当areItemsTheSame()返回true，areContentsTheSame()返回false时，
                // 才会调用getChangePayload()，若getChangePayload()执行到该分支，则表示出现断言异常。
                throw AssertionError("getChangePayload()出现断言异常")
            }
        }
    })
}