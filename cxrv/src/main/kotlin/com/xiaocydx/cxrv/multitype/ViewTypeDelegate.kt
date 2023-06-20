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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.concat.SpanSizeProvider
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.list.*

/**
 * ViewType委托类
 *
 * [ITEM]是[adapter]的item类型或者item的子类型
 *
 * @author xcc
 * @date 2021/10/8
 */
abstract class ViewTypeDelegate<ITEM : Any, VH : ViewHolder> : SpanSizeProvider {
    private var maxScrap: Int = 0
    private var callbacks = InlineList<AdapterAttachCallback>()

    @PublishedApi
    @Suppress("PropertyName")
    internal var _adapter: ListAdapter<Any, *>? = null
        private set
    internal var typeLinker: ((item: ITEM) -> Boolean)? = null
        private set
    val viewType = this::class.java.hashCode()

    /**
     * 通过[adapter]修改item时，例如[ListAdapter.setItem]，会对item做校验，
     * 若item的Class未注册过，则抛出[IllegalArgumentException]异常。
     */
    val adapter: ListAdapter<Any, *>
        get() = requireNotNull(_adapter) {
            "请先调用ViewTypeDelegate.attachAdapter()，关联ListAdapter。"
        }

    /**
     * 可用于[onCreateViewHolder]中创建itemView
     */
    protected val ViewGroup.inflater: LayoutInflater
        get() = LayoutInflater.from(context)

    /**
     * 可用于[onCreateViewHolder]中创建itemView
     */
    protected fun ViewGroup.inflate(@LayoutRes resource: Int): View {
        return inflater.inflate(resource, this, false)
    }

    /**
     * 可用于[onViewRecycled]中清除itemView及其子View的点击、长按监听
     */
    fun View.clearClickListener() {
        setOnClickListener(null)
        setOnLongClickListener(null)
    }

    /**
     * 通过[VH]获取item
     *
     * [ViewTypeDelegate]的函数被调用传入[VH]之前，会先校验[VH]和[ITEM]的对应关系，
     * 因此通过`bindingAdapterPosition`获取的item是类型安全的，将item类型转换为[ITEM]返回即可。
     */
    @Deprecated(
        message = "该属性无法确保类型安全，以一对一关系更改viewType的局部更新场景为例，" +
                "onViewRecycled()传入的holder其实际viewType跟当前viewType已经不一致，" +
                "此时将获取的item转换为ITEM会抛出ClassCastException异常。",
        replaceWith = ReplaceWith("getItemOrNull()")
    )
    @Suppress("UNCHECKED_CAST")
    val VH.item: ITEM
        get() = adapter.getItem(bindingAdapterPosition) as ITEM

    /**
     * 通过[VH]获取item
     *
     * 若通过`bindingAdapterPosition`获取不到item，或者item类型验证失败，则返回`null`。
     * 以一对一关系更改`viewType`的局部更新场景为例，[onViewRecycled]传入的`holder`，
     * 其实际`viewType`跟当前[viewType]已经不一致，此时item类型验证失败。
     */
    @Suppress("UNCHECKED_CAST")
    fun VH.getItemOrNull(): ITEM? {
        val item = adapter.getItemOrNull(bindingAdapterPosition) ?: return null
        val adapter = adapter as? MultiTypeAdapter ?: return null
        val clazz = adapter.getType(viewType)?.clazz ?: return null
        // 大部分场景clazz.isAssignableFrom(item.javaClass)直接进入if (this == cls)分支返回true
        return if (clazz.isAssignableFrom(item.javaClass)) item as ITEM else null
    }

    /**
     * 通过[VH]设置item，该函数必须在主线程调用
     *
     * **注意**：当[item]为新对象时，才能跟旧对象进行内容对比。
     */
    @MainThread
    fun VH.setItem(item: ITEM) = adapter.setItem(bindingAdapterPosition, item)

    /**
     * 通过[VH]设置item，该函数必须在主线程调用
     *
     * 若[ITEM]是data class，则通过`oldItem.copy()`返回`newItem`：
     * ```
     * holder.setItem { copy(name = "newName") }
     * ```
     */
    @MainThread
    inline fun VH.setItem(newItem: ITEM.() -> ITEM) {
        getItemOrNull()?.newItem()?.let { setItem(it) }
    }

    /**
     * 通过[VH]移除item，该函数必须在主线程调用
     */
    @MainThread
    fun VH.removeItem() = adapter.removeItemAt(bindingAdapterPosition)

    @CallSuper
    @Suppress("UNCHECKED_CAST")
    open fun attachAdapter(adapter: ListAdapter<*, *>) {
        _adapter = adapter as ListAdapter<Any, *>
        callbacks.accessEach { adapter.addAdapterAttachCallback(it) }
        callbacks = InlineList()
    }

    @MainThread
    fun addAdapterAttachCallback(callback: AdapterAttachCallback) {
        assertMainThread()
        if (_adapter != null) {
            _adapter!!.addAdapterAttachCallback(callback)
            return
        }
        callbacks += callback
    }

    @MainThread
    fun removeAdapterAttachCallback(callback: AdapterAttachCallback) {
        assertMainThread()
        _adapter?.removeAdapterAttachCallback(callback)
        callbacks -= callback
    }

    /**
     * 设置[viewType]类型在[RecycledViewPool]中的回收上限，
     * 当[onViewRecycled]被调用时，[maxScrap]就会被消费掉，该函数必须在主线程调用。
     */
    @MainThread
    fun setMaxScrap(@IntRange(from = 1) maxScrap: Int): ViewTypeDelegate<ITEM, VH> {
        assertMainThread()
        require(maxScrap > 0) { "maxScrap = ${maxScrap}，需要大于0" }
        this.maxScrap = maxScrap
        return this
    }

    /**
     * 设置类型链接器，用于一对多映射类型场景，该函数必须在主线程调用
     */
    @MainThread
    fun typeLinker(linker: (item: ITEM) -> Boolean): ViewTypeDelegate<ITEM, VH> {
        assertMainThread()
        typeLinker = linker
        return this
    }

    /**
     * 消费并返回[maxScrap]，该函数用于避免多次设置回收上限
     */
    internal fun consumeMaxScrap(): Int {
        return maxScrap.also { maxScrap = 0 }
    }

    /**
     * 对应[DiffUtil.ItemCallback.areItemsTheSame]
     *
     * [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     */
    @MainThread
    @WorkerThread
    abstract fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean

    /**
     * 对应[DiffUtil.ItemCallback.areContentsTheSame]
     *
     * 1. [areItemsTheSame]返回true -> 调用[areContentsTheSame]。
     * 2. [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     */
    @MainThread
    @WorkerThread
    open fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = oldItem == newItem

    /**
     * 对应[DiffUtil.ItemCallback.getChangePayload]
     *
     * 1. [areItemsTheSame]返回true -> [areContentsTheSame]返回false -> 调用[getChangePayload]。
     * 2. [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     */
    @MainThread
    open fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? = null

    /**
     * 对应[Adapter.onCreateViewHolder]
     */
    abstract fun onCreateViewHolder(parent: ViewGroup): VH

    /**
     * 对应Adapter.onBindViewHolder(holder, position, payloads)
     */
    open fun onBindViewHolder(holder: VH, item: ITEM, payloads: List<Any>) {
        onBindViewHolder(holder, item)
    }

    /**
     * 对应[Adapter.onBindViewHolder]
     */
    open fun onBindViewHolder(holder: VH, item: ITEM) = Unit

    /**
     * 对应[Adapter.onViewRecycled]
     */
    open fun onViewRecycled(holder: VH) = Unit

    /**
     * 对应[Adapter.onFailedToRecycleView]
     */
    open fun onFailedToRecycleView(holder: VH): Boolean = false

    /**
     * 对应[Adapter.onViewAttachedToWindow]
     */
    open fun onViewAttachedToWindow(holder: VH) = Unit

    /**
     * 对应[Adapter.onViewDetachedFromWindow]
     */
    open fun onViewDetachedFromWindow(holder: VH) = Unit

    /**
     * 对应[Adapter.onAttachedToRecyclerView]
     */
    open fun onAttachedToRecyclerView(recyclerView: RecyclerView) = Unit

    /**
     * 对应[Adapter.onDetachedFromRecyclerView]
     */
    open fun onDetachedFromRecyclerView(recyclerView: RecyclerView) = Unit
}