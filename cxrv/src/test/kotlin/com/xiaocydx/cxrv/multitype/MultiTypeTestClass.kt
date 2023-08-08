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

package com.xiaocydx.cxrv.multitype

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder

internal open class TestItem
internal open class TypeTestItem(
    val type: TestType
)

internal enum class TestType {
    TYPE_A, TYPE_B
}

internal fun <T : Any> mutableMultiTypeOf() = MutableMultiTypeImpl<T>()

internal abstract class AbsTestDelegate<ITEM : Any> : ViewTypeDelegate<ITEM, ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = error("onCreateViewHolder")
    override fun onBindViewHolder(holder: ViewHolder, item: ITEM) = Unit
    override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = true
}

internal open class TestDelegate : ViewTypeDelegate<TestItem, ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = error("onCreateViewHolder")
    override fun onBindViewHolder(holder: ViewHolder, item: TestItem) = Unit
    override fun areItemsTheSame(oldItem: TestItem, newItem: TestItem): Boolean = true
}

internal open class TypeADelegate : ViewTypeDelegate<TypeTestItem, ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = error("onCreateViewHolder")
    override fun onBindViewHolder(holder: ViewHolder, item: TypeTestItem) = Unit
    override fun areItemsTheSame(oldItem: TypeTestItem, newItem: TypeTestItem) = oldItem.type == newItem.type
}

internal open class TypeBDelegate : ViewTypeDelegate<TypeTestItem, ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = error("onCreateViewHolder")
    override fun onBindViewHolder(holder: ViewHolder, item: TypeTestItem) = Unit
    override fun areItemsTheSame(oldItem: TypeTestItem, newItem: TypeTestItem) = oldItem.type == newItem.type
}