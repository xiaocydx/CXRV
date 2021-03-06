package com.xiaocydx.cxrv.multitype

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.*
import androidx.annotation.IntRange
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.concat.SpanSizeProvider
import com.xiaocydx.cxrv.internal.accessEach
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
    private var callbacks: ArrayList<AdapterAttachCallback>? = null

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
    @Suppress("UNCHECKED_CAST")
    val VH.item: ITEM
        get() = adapter.getItem(bindingAdapterPosition) as ITEM

    /**
     * 通过[VH]设置item，该函数必须在主线程调用
     *
     * **注意**：当[item]为新对象时，才能跟旧对象进行内容对比。
     */
    @MainThread
    fun VH.setItem(item: ITEM) {
        adapter.setItem(bindingAdapterPosition, item)
    }

    /**
     * 通过[VH]设置item，该函数必须在主线程调用
     *
     * 若[ITEM]是data class，则通过`oldItem.copy()`返回`newItem`：
     * ```
     * holder.setItem { it.copy(name = "newName") }
     * ```
     */
    @MainThread
    inline fun VH.setItem(newItem: (oldItem: ITEM) -> ITEM) {
        setItem(newItem(item))
    }

    /**
     * 通过[VH]移除item，该函数必须在主线程调用
     */
    @MainThread
    fun VH.removeItem() {
        adapter.removeItemAt(bindingAdapterPosition)
    }

    @CallSuper
    @Suppress("UNCHECKED_CAST")
    open fun attachAdapter(adapter: ListAdapter<*, *>) {
        _adapter = adapter as ListAdapter<Any, *>
        callbacks?.accessEach { adapter.addAdapterAttachCallback(it) }
        callbacks = null
    }

    @MainThread
    fun addAdapterAttachCallback(callback: AdapterAttachCallback) {
        assertMainThread()
        if (_adapter != null) {
            _adapter!!.addAdapterAttachCallback(callback)
            return
        }
        if (callbacks == null) {
            callbacks = ArrayList(2)
        }
        if (!callbacks!!.contains(callback)) {
            callbacks!!.add(callback)
        }
    }

    @MainThread
    fun removeAdapterAttachCallback(callback: AdapterAttachCallback) {
        assertMainThread()
        _adapter?.removeAdapterAttachCallback(callback)
        callbacks?.remove(callback)
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
     * 对应[ItemCallback.areItemsTheSame]
     */
    @AnyThread
    abstract fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean

    /**
     * 对应[ItemCallback.areContentsTheSame]
     *
     * 仅当[areItemsTheSame]返回true时才调用此函数。
     */
    @AnyThread
    open fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = oldItem == newItem

    /**
     * 对应[ItemCallback.getChangePayload]
     *
     * 仅当[areItemsTheSame]返回true、[areContentsTheSame]返回false时才调用此函数。
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
}