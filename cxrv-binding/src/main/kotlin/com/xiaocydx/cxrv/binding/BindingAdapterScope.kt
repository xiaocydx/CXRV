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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.binding

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewbinding.ViewBinding
import com.xiaocydx.cxrv.internal.RvDslMarker
import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.setItem
import com.xiaocydx.cxrv.list.setItems

/**
 * [BindingAdapter]的构建函数，适用于简单列表场景
 *
 * ```
 * bindingAdapter(
 *     inflate = ItemFooBinding::inflate,
 *     areItemsTheSame { oldItem: Foo, newItem: Foo ->
 *         oldItem.id == newItem.id
 *     }
 * ) {
 *     onCreateView {...}
 *     onBindView { item -> ... }
 * }
 * ```
 * @param inflate         函数引用`VB::inflate`
 * @param areItemsTheSame 对应[DiffUtil.ItemCallback.areItemsTheSame]
 */
inline fun <ITEM : Any, VB : ViewBinding> bindingAdapter(
    noinline inflate: Inflate<VB>,
    noinline areItemsTheSame: (oldItem: ITEM, newItem: ITEM) -> Boolean,
    block: BindingAdapterScope<ITEM, VB>.() -> Unit
): BindingAdapter<ITEM, VB> = BindingAdapterScope(inflate, areItemsTheSame).apply(block)

/**
 * [BindingAdapter]的构建函数，适用于简单列表场景
 *
 * ```
 * bindingAdapter(
 *     uniqueId = Foo::id,
 *     inflate = ItemFooBinding::inflate
 * ) {
 *     onCreateView {...}
 *     onBindView { item -> ... }
 * }
 * ```
 * @param inflate  函数引用`VB::inflate`
 * @param uniqueId item唯一id，是[DiffUtil.ItemCallback.areItemsTheSame]的简化函数
 */
inline fun <ITEM : Any, VB : ViewBinding> bindingAdapter(
    noinline inflate: Inflate<VB>,
    crossinline uniqueId: (item: ITEM) -> Any?,
    block: BindingAdapterScope<ITEM, VB>.() -> Unit
): BindingAdapter<ITEM, VB> = bindingAdapter(
    inflate = inflate,
    areItemsTheSame = { oldItem: ITEM, newItem: ITEM ->
        uniqueId(oldItem) == uniqueId(newItem)
    },
    block = block
)

/**
 * 构建并设置[BindingAdapter]，适用于简单列表场景
 *
 * ```
 * recyclerView.binding(
 *     inflate = ItemFooBinding::inflate,
 *     areItemsTheSame { oldItem: Foo, newItem: Foo ->
 *         oldItem.id == newItem.id
 *     }
 * ) {
 *     onCreateView {...}
 *     onBindView { item -> ... }
 * }
 * ```
 * @param inflate         函数引用`VB::inflate`
 * @param areItemsTheSame 对应[DiffUtil.ItemCallback.areItemsTheSame]
 */
inline fun <RV : RecyclerView, ITEM : Any, VB : ViewBinding> RV.binding(
    noinline inflate: Inflate<VB>,
    noinline areItemsTheSame: (oldItem: ITEM, newItem: ITEM) -> Boolean,
    block: BindingAdapterScope<ITEM, VB>.() -> Unit
): RV = adapter(bindingAdapter(inflate, areItemsTheSame, block))

/**
 * 构建并设置[BindingAdapter]，适用于简单列表场景
 *
 * ```
 * recyclerView.binding(
 *     uniqueId = Foo::id,
 *     inflate = ItemFooBinding::inflate
 * ) {
 *     onCreateView {...}
 *     onBindView { item -> ... }
 * }
 * ```
 * @param inflate  函数引用`VB::inflate`
 * @param uniqueId item唯一id，是[DiffUtil.ItemCallback.areItemsTheSame]的简化函数
 */
inline fun <RV : RecyclerView, ITEM : Any, VB : ViewBinding> RV.binding(
    noinline inflate: Inflate<VB>,
    crossinline uniqueId: (item: ITEM) -> Any?,
    block: BindingAdapterScope<ITEM, VB>.() -> Unit
): RV = adapter(bindingAdapter(inflate, uniqueId, block))

/**
 * [BindingAdapter]的构建作用域
 */
@RvDslMarker
class BindingAdapterScope<ITEM : Any, VB : ViewBinding>
@PublishedApi internal constructor(
    private val inflate: Inflate<VB>,
    private val areItemsTheSame: (oldItem: ITEM, newItem: ITEM) -> Boolean
) : BindingAdapter<ITEM, VB>() {
    private var areContentsTheSame: ((oldItem: ITEM, newItem: ITEM) -> Boolean)? = null
    private var getChangePayload: ((oldItem: ITEM, newItem: ITEM) -> Any?)? = null
    private var onCreateView: (VB.() -> Unit)? = null
    private var onBindView: (VB.(item: ITEM, payloads: List<Any>) -> Unit)? = null
    private var onViewRecycled: (VB.() -> Unit)? = null
    private var onFailedToRecycleView: (VB.() -> Boolean)? = null
    private var onViewAttachedToWindow: (VB.() -> Unit)? = null
    private var onViewDetachedFromWindow: (VB.() -> Unit)? = null
    private var onAttachedToRecyclerView: ((rv: RecyclerView) -> Unit)? = null
    private var onDetachedFromRecyclerView: ((rv: RecyclerView) -> Unit)? = null

    /**
     * 对应[DiffUtil.ItemCallback.areContentsTheSame]
     *
     * 1. [areItemsTheSame]返回true -> 调用[areContentsTheSame]。
     * 2. [ListOwner.setItem]和[ListOwner.setItems]会复用[areContentsTheSame]进行差异对比。
     */
    fun areContentsTheSame(block: (oldItem: ITEM, newItem: ITEM) -> Boolean) {
        areContentsTheSame = block
    }

    /**
     * 对应[DiffUtil.ItemCallback.getChangePayload]
     *
     * 1. [areItemsTheSame]返回true -> [areContentsTheSame]返回false -> 调用[getChangePayload]。
     * 2. [ListOwner.setItem]和[ListOwner.setItems]会复用[getChangePayload]进行差异对比。
     */
    fun getChangePayload(block: (oldItem: ITEM, newItem: ITEM) -> Any?) {
        getChangePayload = block
    }

    /**
     * 对应[Adapter.onCreateViewHolder]
     *
     * 可以在[block]中完成初始化工作，例如设置点击监听。
     */
    fun onCreateView(block: VB.() -> Unit) {
        onCreateView = block
    }

    /**
     * 对应Adapter.onBindViewHolder(holder, position, payloads)
     */
    fun onBindViewPayloads(block: VB.(item: ITEM, payloads: List<Any>) -> Unit) {
        onBindView = block
    }

    /**
     * 对应[Adapter.onBindViewHolder]
     */
    inline fun onBindView(crossinline block: VB.(item: ITEM) -> Unit) {
        onBindViewPayloads { item, _ -> block(this, item) }
    }

    /**
     * 对应[Adapter.onViewRecycled]
     */
    fun onViewRecycled(block: VB.() -> Unit) {
        onViewRecycled = block
    }

    /**
     * 对应[Adapter.onFailedToRecycleView]
     */
    fun onFailedToRecycleView(block: VB.() -> Boolean) {
        onFailedToRecycleView = block
    }

    /**
     * 对应[Adapter.onViewAttachedToWindow]
     */
    fun onViewAttachedToWindow(block: VB.() -> Unit) {
        onViewAttachedToWindow = block
    }

    /**
     * 对应[Adapter.onViewDetachedFromWindow]
     */
    fun onViewDetachedFromWindow(block: VB.() -> Unit) {
        onViewDetachedFromWindow = block
    }

    /**
     * 对应[Adapter.onAttachedToRecyclerView]
     */
    fun onAttachedToRecyclerView(block: (rv: RecyclerView) -> Unit) {
        onAttachedToRecyclerView = block
    }

    /**
     * 对应[Adapter.onDetachedFromRecyclerView]
     */
    fun onDetachedFromRecyclerView(block: (rv: RecyclerView) -> Unit) {
        onDetachedFromRecyclerView = block
    }

    override fun inflate(): Inflate<VB> = inflate

    override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
        return areItemsTheSame.invoke(oldItem, newItem)
    }

    override fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
        return areContentsTheSame?.invoke(oldItem, newItem) ?: (oldItem == newItem)
    }

    override fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? {
        return getChangePayload?.invoke(oldItem, newItem)
    }

    override fun VB.onCreateView() {
        onCreateView?.invoke(this)
    }

    override fun VB.onBindView(item: ITEM) {
        onBindView?.invoke(this, item, emptyList())
    }

    override fun VB.onBindView(item: ITEM, payloads: List<Any>) {
        onBindView?.invoke(this, item, payloads)
    }

    override fun VB.onViewRecycled() {
        onViewRecycled?.invoke(this)
    }

    override fun VB.onFailedToRecycleView(): Boolean {
        return onFailedToRecycleView?.invoke(this) ?: false
    }

    override fun VB.onViewAttachedToWindow() {
        onViewAttachedToWindow?.invoke(this)
    }

    override fun VB.onViewDetachedFromWindow() {
        onViewDetachedFromWindow?.invoke(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        onAttachedToRecyclerView?.invoke(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        onDetachedFromRecyclerView?.invoke(recyclerView)
    }
}