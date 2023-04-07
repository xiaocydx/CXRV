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

package com.xiaocydx.cxrv.layout.callback

import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*

/**
 * [LayoutManager]的部分函数回调
 *
 * @author xcc
 * @date 2022/8/11
 */
internal interface LayoutManagerCallback {

    /**
     * 对应[LayoutManager.onAttachedToWindow]
     *
     * 该函数应当在`super.onAttachedToWindow(view)`之前被调用。
     */
    fun onAttachedToWindow(view: RecyclerView) = Unit

    /**
     * 对应[LayoutManager.onDetachedFromWindow]
     *
     * 该函数在`super.onDetachedFromWindow(view, recycler)`之前被调用。
     */
    fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler) = Unit

    /**
     * 对应[LayoutManager.onAdapterChanged]
     */
    fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) = Unit

    /**
     * 对应[LayoutManager.onLayoutChildren]
     *
     * 该函数在`super.onLayoutChildren(recycler, state)`之前被调用。
     */
    fun onLayoutChildren(recycler: Recycler, state: State) = Unit

    /**
     * 对应[LayoutManager.requestSimpleAnimationsInNextLayout]
     *
     * 该函数在`super.requestSimpleAnimationsInNextLayout()`之前被调用。
     */
    fun requestSimpleAnimationsInNextLayout() = Unit

    /**
     * 对应[LayoutManager.onLayoutCompleted]
     */
    fun onLayoutCompleted(layout: LayoutManager, state: State) = Unit

    /**
     * 从[CompositeLayoutManagerCallback]移除时，该函数会被调用
     */
    fun onCleared() = Unit
}