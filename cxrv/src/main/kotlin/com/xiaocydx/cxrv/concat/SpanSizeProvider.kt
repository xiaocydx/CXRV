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

package com.xiaocydx.cxrv.concat

import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.R
import com.xiaocydx.cxrv.internal.doOnAttach
import androidx.recyclerview.widget.StaggeredGridLayoutManager.LayoutParams as StaggeredGridLayoutParams

/**
 * item所占用的跨度空间数提供者
 *
 * [SpanSizeProvider]的实现方式可参考[ViewAdapter]。
 *
 * @author xcc
 * @date 2021/9/27
 */
interface SpanSizeProvider {

    /**
     * item是否占用所有跨度空间
     *
     * 仅当[RecyclerView.getLayoutManager]为[StaggeredGridLayoutManager]时有效,
     * 实现方式对应[StaggeredGridLayoutManager.LayoutParams.setFullSpan]。
     *
     * **注意**：在[Adapter.onAttachedToRecyclerView]下
     * 调用了[SpanSizeProvider.onAttachedToRecyclerView]，该函数才会生效。
     *
     * @return item是否占用所有跨度空间，返回true表示占用所有跨度空间。
     */
    fun fullSpan(position: Int, holder: ViewHolder): Boolean = false

    /**
     * 获取item所占用的跨度空间数
     *
     * 仅当[RecyclerView.getLayoutManager]为[GridLayoutManager]时有效,
     * 实现方式对应[GridLayoutManager.SpanSizeLookup.getSpanSize]。
     *
     * **注意**：在[Adapter.onAttachedToRecyclerView]下
     * 调用了[SpanSizeProvider.onAttachedToRecyclerView]，该函数才会生效。
     *
     * @param spanCount 对应[GridLayoutManager.getSpanCount]
     * @return item所占用的跨度空间数，返回[spanCount]表示占用所有跨度空间。
     */
    fun getSpanSize(position: Int, spanCount: Int): Int = 1
}

/**
 * 显式Receiver，方便实现类调用[SpanSizeProvider.onAttachedToRecyclerView]
 */
val SpanSizeProvider.spanSizeProvider: SpanSizeProvider
    get() = this

/**
 * 在[Adapter.onAttachedToRecyclerView]下调用
 */
@Suppress("UnusedReceiverParameter")
fun SpanSizeProvider.onAttachedToRecyclerView(rv: RecyclerView) = with(rv) {
    if (getTag(R.id.tag_span_size_lookup) == true) {
        // 避免重复执行rv.trySetSpanSizeLookup()，重复执行的原因可能是：
        // 1.对rv多次设置实现了SpanSizeProvider的adapter。
        // 2.ConcatAdapter包含多个实现了SpanSizeProvider的adapter。
        return
    }

    if (layoutManager != null) {
        initSpanSizeLookup()
    } else {
        doOnAttach { initSpanSizeLookup() }
    }
    setTag(R.id.tag_span_size_lookup, true)
}

private fun RecyclerView.initSpanSizeLookup() {
    when (val lm = layoutManager) {
        is StaggeredGridLayoutManager -> {
            addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(view: View) {
                    val lp = view.layoutParams as? StaggeredGridLayoutParams ?: return
                    val holder = getChildViewHolder(view) ?: return
                    val adapter = holder.bindingAdapter as? SpanSizeProvider ?: return
                    lp.isFullSpan = adapter.fullSpan(holder.bindingAdapterPosition, holder)
                }

                override fun onChildViewDetachedFromWindow(view: View) = Unit
            })
        }
        is GridLayoutManager -> if (lm.spanSizeLookup !is ConcatSpanSizeLookup) {
            lm.spanSizeLookup = ConcatSpanSizeLookup(this, lm)
        }
    }
}