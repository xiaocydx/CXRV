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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.concat.ViewAdapter

/**
 * 设置[Adapter]，可用于链式调用场景
 *
 * ```
 * val adapter: Adapter<*> = ...
 * recyclerView.adapter(adapter)
 * ```
 */
infix fun <T : RecyclerView> T.adapter(adapter: Adapter<*>): T {
    this.adapter = adapter
    return this
}

/**
 * 获取[child]在绑定适配器中的position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildBindingAdapterPosition(child: View): Int {
    return getChildViewHolder(child)?.bindingAdapterPosition ?: RecyclerView.NO_POSITION
}

/**
 * 获取[child]的绑定适配器的最后position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildLastBindingAdapterPosition(child: View): Int {
    return getChildBindingAdapterItemCount(child) - 1
}

/**
 * 获取[child]的绑定适配器的itemCount
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildBindingAdapterItemCount(child: View): Int {
    return getChildBindingAdapter(child)?.itemCount ?: 0
}

/**
 * 获取[child]的绑定适配器
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.getChildBindingAdapter(child: View): Adapter<*>? {
    return getChildViewHolder(child)?.bindingAdapter
}

/**
 * [child]在绑定适配器中的position是否为起始position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.isFirstChildBindingAdapterPosition(child: View): Boolean {
    return getChildBindingAdapterPosition(child) == 0
}

/**
 * [child]在绑定适配器中的position是否为最后position
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.isLastChildBindingAdapterPosition(child: View): Boolean {
    return getChildBindingAdapterPosition(child) == getChildLastBindingAdapterPosition(child)
}

/**
 * [child]是否为Header或Footer
 *
 * 若[child]不是RecyclerView的子View，则抛出[IllegalArgumentException]。
 */
fun RecyclerView.isHeaderOrFooter(child: View): Boolean {
    return getChildViewHolder(child)?.bindingAdapter is ViewAdapter<*>
}