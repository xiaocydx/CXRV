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

@file:Suppress("UnusedReceiverParameter")

package com.xiaocydx.accompanist.transition.transform

import android.view.View
import android.widget.ImageView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.optimizeNextFrameScroll
import com.xiaocydx.accompanist.view.setRoundRectOutlineProvider
import com.xiaocydx.cxrv.itemvisible.findFirstCompletelyVisibleItemPosition
import com.xiaocydx.cxrv.itemvisible.findLastCompletelyVisibleItemPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

/**
 * 设置Sender页面的视图
 *
 * 圆角值的捕获规则：
 * 1. 若[image]调用了[View.setRoundRectOutlineProvider]，则捕获设置的圆角值。
 * 2. 若[root]调用了[View.setRoundRectOutlineProvider]，并且[root]和[image]的尺寸一致，则捕获设置的圆角值。
 *
 * @param root  [image]的父级，当过渡动画开始时，`root.alpha = 0f`
 * @param image 进行`ImageView.imageMatrix`变换的[ImageView]
 */
fun Transform.setSenderViews(activity: FragmentActivity, root: View?, image: ImageView?) {
    activity.transformState.setSenderViews(root, image)
}

/**
 * Receiver页面发送的事件
 */
fun Transform.receiverEvent(activity: FragmentActivity, token: String): Flow<ReceiverEvent> {
    return activity.transformState.receiverEvent.filter { it.token == token }
}

/**
 * 非平滑滚动至[adapter]的[position]
 */
fun Transform.scrollToPosition(rv: RecyclerView, adapter: Adapter<*>, position: Int): Boolean {
    return rv.scrollToPosition(adapter, position)
}

/**
 * 查找[adapter]的[position]对应的ViewHolder
 */
fun Transform.findViewHolder(rv: RecyclerView, adapter: Adapter<*>, position: Int): ViewHolder? {
    return rv.findViewHolder(adapter, position)
}

/**
 * 非平滑滚动到匹配[adapter]和[position]的位置
 *
 * **注意**：该函数以[ConcatAdapter]实现HeaderFooter为前提，匹配位置。
 *
 * @param adapter  目标`holder.bindingAdapter`
 * @param position 目标`holder.bindingAdapterPosition`
 */
private fun RecyclerView.scrollToPosition(adapter: Adapter<*>, position: Int): Boolean {
    var offset = 0
    val concatAdapter = this.adapter as? ConcatAdapter
    if (concatAdapter != null) {
        // 先尝试快路径，尽可能避免调用concatAdapter.adapters创建集合对象
        val holder = findViewHolder(adapter, position)
        if (holder != null) {
            offset = holder.layoutPosition - holder.bindingAdapterPosition
        } else {
            val adapters = concatAdapter.adapters
            for (i in adapters.indices) {
                if (adapters[i] === adapter) break
                offset += adapters[i].itemCount
            }
        }
    }

    val layoutPosition = offset + position
    val firstPosition = findFirstCompletelyVisibleItemPosition()
    val lastPosition = findLastCompletelyVisibleItemPosition()
    if (layoutPosition in firstPosition..lastPosition) return false

    scrollToPosition(layoutPosition)
    // 非平滑滚动布局流程的优化方案，可用于替代增加缓存上限的方案
    optimizeNextFrameScroll()
    return true
}

/**
 * 查找匹配[adapter]和[position]的[ViewHolder]，若查找不到，则返回`null`
 *
 * **注意**：该函数以[ConcatAdapter]实现HeaderFooter为前提，匹配[ViewHolder]。
 *
 * @param adapter  目标`holder.bindingAdapter`
 * @param position 目标`holder.bindingAdapterPosition`
 */
private fun RecyclerView.findViewHolder(adapter: Adapter<*>, position: Int): ViewHolder? {
    val lm = layoutManager ?: return null
    for (i in 0 until lm.childCount) {
        val holder = lm.getChildAt(i)?.let(::getChildViewHolder)
        if (holder != null && holder.bindingAdapter === adapter
                && holder.bindingAdapterPosition == position) {
            return holder
        }
    }
    return null
}