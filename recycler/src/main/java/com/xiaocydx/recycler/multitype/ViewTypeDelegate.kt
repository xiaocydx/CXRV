@file:Suppress("PropertyName")

package com.xiaocydx.recycler.multitype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.list.setItem
import com.xiaocydx.recycler.concat.SpanSizeProvider

/**
 * ViewType委托类
 *
 * [ITEM]是[adapter]的item类型或者item的子类型
 *
 * @author xcc
 * @date 2021/10/8
 */
abstract class ViewTypeDelegate<ITEM : Any, VH : ViewHolder> {
    private var attachActions: MutableList<(ListAdapter<*, *>) -> Unit>? = null

    @VisibleForTesting
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
    @Suppress("UNCHECKED_CAST")
    val VH.item: ITEM
        get() = adapter.getItem(bindingAdapterPosition) as ITEM

    /**
     * 通过[VH]设置item
     *
     * **注意**：当[item]为新对象时，才能跟旧对象进行内容对比。
     */
    fun VH.setItem(item: ITEM) {
        adapter.setItem(bindingAdapterPosition, item)
    }

    /**
     * 通过[VH]设置item
     *
     * 若[ITEM]是data class，则通过`oldItem.copy()`返回`newItem`：
     * ```
     * holder.setItem { it.copy(name = "newName") }
     * ```
     */
    inline fun VH.setItem(newItem: (oldItem: ITEM) -> ITEM) {
        setItem(newItem(item))
    }

    /**
     * 通过[VH]移除item
     */
    fun VH.removeItem() {
        adapter.removeItemAt(bindingAdapterPosition)
    }

    /**
     * 关联[ListAdapter]
     */
    @CallSuper
    @Suppress("UNCHECKED_CAST")
    open fun attachAdapter(adapter: ListAdapter<*, *>) {
        _adapter = adapter as ListAdapter<Any, *>
        attachActions?.forEach { it(adapter) }
        attachActions = null
    }

    /**
     * 关联[ListAdapter]时，同步执行[block]
     */
    fun doOnAttachAdapter(block: (ListAdapter<*, *>) -> Unit) {
        _adapter?.apply(block)?.let { return }
        if (attachActions == null) {
            attachActions = mutableListOf()
        }
        if (!attachActions!!.contains(block)) {
            attachActions!!.add(block)
        }
    }

    /**
     * 设置类型链接器，用于一对多映射类型场景
     */
    fun typeLinker(linker: (item: ITEM) -> Boolean): ViewTypeDelegate<ITEM, VH> {
        typeLinker = linker
        return this
    }

    /**
     * 对应[ItemCallback.areItemsTheSame]
     */
    @WorkerThread
    abstract fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean

    /**
     * 对应[ItemCallback.areContentsTheSame]
     *
     * 仅当[areItemsTheSame]返回true时才调用此函数。
     */
    @WorkerThread
    open fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = oldItem == newItem

    /**
     * 对应[ItemCallback.getChangePayload]
     *
     * 仅当[areItemsTheSame]返回true、[areContentsTheSame]返回false时才调用此函数。
     */
    @WorkerThread
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
    open fun onBindViewHolder(holder: VH, item: ITEM): Unit = Unit

    /**
     * 对应[Adapter.onViewRecycled]
     */
    open fun onViewRecycled(holder: VH): Unit = Unit

    /**
     * 对应[Adapter.onFailedToRecycleView]
     */
    open fun onFailedToRecycleView(holder: VH): Boolean = false

    /**
     * 对应[Adapter.onViewAttachedToWindow]
     */
    open fun onViewAttachedToWindow(holder: VH): Unit = Unit

    /**
     * 对应[Adapter.onViewDetachedFromWindow]
     */
    open fun onViewDetachedFromWindow(holder: VH): Unit = Unit

    /**
     * 对应[Adapter.onAttachedToRecyclerView]
     */
    open fun onAttachedToRecyclerView(recyclerView: RecyclerView): Unit = Unit

    /**
     * 对应[Adapter.onDetachedFromRecyclerView]
     */
    open fun onDetachedFromRecyclerView(recyclerView: RecyclerView): Unit = Unit

    /**
     * 对应[SpanSizeProvider.fullSpan]
     */
    open fun fullSpan(position: Int, holder: VH): Boolean = false

    /**
     * 对应[SpanSizeProvider.getSpanSize]
     */
    open fun getSpanSize(position: Int, spanCount: Int): Int = 1
}