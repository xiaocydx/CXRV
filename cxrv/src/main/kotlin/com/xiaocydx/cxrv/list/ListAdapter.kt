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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.concat.SpanSizeProvider
import com.xiaocydx.cxrv.concat.onAttachedToRecyclerView
import com.xiaocydx.cxrv.concat.spanSizeProvider
import com.xiaocydx.cxrv.internal.assertMainThread
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 列表适配器
 *
 * @author xcc
 * @date 2021/9/9
 */
abstract class ListAdapter<ITEM : Any, VH : ViewHolder> :
        Adapter<VH>(), ListOwner<ITEM>, SpanSizeProvider {
    private var tags: HashMap<String, Any?>? = null
    private var callbacks = InlineList<AdapterAttachCallback>()
    private var listeners = InlineList<ViewHolderListener<in VH>>()
    private val differ: CoroutineListDiffer<ITEM> = CoroutineListDiffer(
        diffCallback = InternalDiffItemCallback(),
        adapter = @Suppress("LeakingThis") this
    )
    var recyclerView: RecyclerView? = null
        private set
    final override val currentList: List<ITEM>
        get() = differ.currentList

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
     * 通过[VH]获取item
     */
    val VH.item: ITEM
        get() = getItem(bindingAdapterPosition)

    /**
     * 通过[VH]设置item，该函数必须在主线程调用
     *
     * **注意**：当[item]为新对象时，才能跟旧对象进行内容对比。
     */
    @MainThread
    fun VH.setItem(item: ITEM) = setItem(bindingAdapterPosition, item)

    /**
     * 通过[VH]设置item，该函数必须在主线程调用
     *
     * 若[ITEM]是data class，则通过`oldItem.copy()`返回`newItem`：
     * ```
     * holder.setItem { copy(name = "newName") }
     * ```
     */
    @MainThread
    inline fun VH.setItem(newItem: ITEM.() -> ITEM) = setItem(item.newItem())

    /**
     * 通过[VH]移除item，该函数必须在主线程调用
     */
    @MainThread
    fun VH.removeItem() = removeItemAt(bindingAdapterPosition)

    /**
     * 对应[DiffUtil.ItemCallback.areItemsTheSame]
     *
     * [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     *
     * 确定局部更新的类型，通常对比item的`key`即可，如果[oldItem]和[newItem]的`key`一样，
     * 函数却返回`false`，那么[oldItem]是remove更新，[newItem]是insert更新，不会是change更新或move更新。
     */
    @MainThread
    @WorkerThread
    protected abstract fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean

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
    protected open fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = oldItem == newItem

    /**
     * 对应[DiffUtil.ItemCallback.getChangePayload]
     *
     * 1. [areItemsTheSame]返回true -> [areContentsTheSame]返回false -> 调用[getChangePayload]。
     * 2. [ListOwner.setItem]和[ListOwner.setItems]会复用该函数进行差异对比。
     *
     * 确定是change更新后，再获取Payload对象，默认实现是返回`null`。
     */
    @MainThread
    protected open fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? = null

    final override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        listeners.accessEach { it.onBindViewHolder(holder, position, payloads) }
        onBindViewHolder(holder, getItem(position), payloads)
    }

    final override fun onBindViewHolder(holder: VH, position: Int) {
        listeners.accessEach { it.onBindViewHolder(holder, position, emptyList()) }
        onBindViewHolder(holder, getItem(position))
    }

    protected open fun onBindViewHolder(holder: VH, item: ITEM, payloads: List<Any>) {
        onBindViewHolder(holder, item)
    }

    protected open fun onBindViewHolder(holder: VH, item: ITEM) = Unit

    override fun getItemCount() = differ.currentList.size

    @MainThread
    final override fun updateList(op: UpdateOp<ITEM>) = updateList(op, dispatch = true)

    /**
     * 启用在主线程执行差异计算
     *
     * 当调度器调度较慢时，会导致差异计算较慢执行（工作线程）、更新列表较慢执行（主线程），
     * 在列表数据量或修改量不大的情况下，可以选择在主线程执行差异计算，不进行任何的调度。
     */
    @MainThread
    fun calculateDiffOnMainThread() {
        setDiffDispatcher(differ.mainDispatcher)
    }

    /**
     * 设置差异计算的工作线程调度器，默认是[Dispatchers.Default]
     */
    @MainThread
    fun setDiffDispatcher(dispatcher: CoroutineDispatcher) {
        assertMainThread()
        differ.setDiffDispatcher(dispatcher)
    }

    /**
     * 添加列表已更改的监听
     *
     * [ListChangedListener.onListChanged]中可以调用[removeListChangedListener]。
     */
    @MainThread
    fun addListChangedListener(listener: ListChangedListener<ITEM>) {
        assertMainThread()
        differ.addListChangedListener(listener)
    }

    /**
     * 移除列表已更改的监听
     */
    @MainThread
    fun removeListChangedListener(listener: ListChangedListener<ITEM>) {
        assertMainThread()
        differ.removeListChangedListener(listener)
    }

    @MainThread
    fun addAdapterAttachCallback(callback: AdapterAttachCallback) {
        assertMainThread()
        if (!callbacks.contains(callback)) {
            callbacks += callback
            recyclerView?.let(callback::onAttachedToRecyclerView)
        }
    }

    @MainThread
    fun removeAdapterAttachCallback(callback: AdapterAttachCallback) {
        assertMainThread()
        callbacks -= callback
    }

    @MainThread
    fun <V> setTag(key: String, value: V) {
        assertMainThread()
        if (tags == null) {
            tags = hashMapOf()
        }
        tags!![key] = value
    }

    @MainThread
    @Suppress("UNCHECKED_CAST")
    fun <V> getTag(key: String): V? {
        assertMainThread()
        return tags?.get(key) as? V
    }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        callbacks.reverseAccessEach { it.onAttachedToRecyclerView(recyclerView) }
        spanSizeProvider.onAttachedToRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        callbacks.reverseAccessEach { it.onDetachedFromRecyclerView(recyclerView) }
        differ.cancel()
    }

    @MainThread
    internal fun addViewHolderListener(listener: ViewHolderListener<in VH>) {
        assertMainThread()
        listeners += listener
    }

    @MainThread
    internal fun removeViewHolderListener(listener: ViewHolderListener<in VH>) {
        assertMainThread()
        listeners -= listener
    }

    @MainThread
    internal fun addListExecuteListener(listener: ListExecuteListener<ITEM>) {
        assertMainThread()
        differ.addListExecuteListener(listener)
    }

    @MainThread
    internal fun removeListExecuteListener(listener: ListExecuteListener<ITEM>) {
        assertMainThread()
        differ.removeListExecuteListener(listener)
    }

    @MainThread
    internal fun updateList(op: UpdateOp<ITEM>, dispatch: Boolean): UpdateResult {
        assertMainThread()
        return differ.updateList(op, dispatch)
    }

    private inner class InternalDiffItemCallback : DiffUtil.ItemCallback<ITEM>() {
        override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@ListAdapter.areItemsTheSame(oldItem, newItem)
        }

        override fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@ListAdapter.areContentsTheSame(oldItem, newItem)
        }

        override fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? {
            return this@ListAdapter.getChangePayload(oldItem, newItem)
        }
    }
}