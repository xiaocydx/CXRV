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

package com.xiaocydx.cxrv.binding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.viewbinding.ViewBinding

typealias Inflate<VB> = (LayoutInflater, ViewGroup, Boolean) -> VB

class BindingHolder<VB : ViewBinding>(val binding: VB) : ViewHolder(binding.root) {
    init {
        setHolder(binding, this)
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun <VB : ViewBinding> getHolder(binding: VB) = requireNotNull(
            value = binding.root.getTag(R.id.tag_view_holder) as? BindingHolder<VB>,
            lazyMessage = { "root还未关联ViewHolder" }
        )

        private fun <VB : ViewBinding> setHolder(binding: VB, holder: BindingHolder<VB>) {
            binding.root.setTag(R.id.tag_view_holder, holder)
        }
    }
}