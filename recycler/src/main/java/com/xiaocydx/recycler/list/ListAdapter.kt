package com.xiaocydx.recycler.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.recycler.extension.accessEach
import com.xiaocydx.recycler.extension.assertMainThread
import com.xiaocydx.recycler.extension.reverseAccessEach
import com.xiaocydx.recycler.helper.ListAdapterHelper
import com.xiaocydx.recycler.widget.SpanSizeProvider
import com.xiaocydx.recycler.widget.onAttachedToRecyclerView
import com.xiaocydx.recycler.widget.onViewAttachedToWindow
import com.xiaocydx.recycler.widget.spanSizeProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 列表适配器
 *
 * @author xcc
 * @date 2021/9/9
 */
@Suppress("LeakingThis")
abstract class ListAdapter<ITEM : Any, VH : ViewHolder>(
    workDispatcher: CoroutineDispatcher = Dispatchers.Default
) : Adapter<VH>(), ListOwner<ITEM>, SpanSizeProvider {
    private var tags: HashMap<String, Any?>? = null
    private var callbacks: ArrayList<AdapterAttachCallback>? = null
    private var listeners: ArrayList<ViewHolderListener<in VH>>? = null
    private val differ: CoroutineListDiffer<ITEM> = CoroutineListDiffer(
        diffCallback = InternalDiffItemCallback(),
        updateCallback = AdapterListUpdateCallback(this),
        workDispatcher = workDispatcher
    )
    private val helper = ListAdapterHelper(this)
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
     * 可用于[onViewRecycled]中清除itemView及其子View的点击、长按监听
     */
    fun View.clearClickListener() {
        setOnClickListener(null)
        setOnLongClickListener(null)
    }

    /**
     * 通过[VH]获取item
     */
    val VH.item: ITEM
        get() = getItem(bindingAdapterPosition)

    /**
     * 通过[VH]设置item
     *
     * **注意**：当[item]为新对象时，才能跟旧对象进行内容对比。
     */
    fun VH.setItem(item: ITEM) {
        setItem(bindingAdapterPosition, item)
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
        removeItemAt(bindingAdapterPosition)
    }

    /**
     * 对应[ItemCallback.areItemsTheSame]
     */
    @WorkerThread
    protected abstract fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean

    /**
     * 对应[ItemCallback.areContentsTheSame]
     *
     * 仅当[areItemsTheSame]返回true时才调用此函数。
     */
    @WorkerThread
    protected open fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = oldItem == newItem

    /**
     * 对应[ItemCallback.getChangePayload]
     *
     * 仅当[areItemsTheSame]返回true、[areContentsTheSame]返回false时才调用此函数。
     */
    @WorkerThread
    protected open fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? = null

    final override fun onBindViewHolder(holder: VH, position: Int, payloads: List<Any>) {
        listeners?.accessEach { it.onBindViewHolder(holder, position, payloads) }
        onBindViewHolder(holder, getItem(position), payloads)
    }

    final override fun onBindViewHolder(holder: VH, position: Int) {
        listeners?.accessEach { it.onBindViewHolder(holder, position, emptyList()) }
        onBindViewHolder(holder, getItem(position))
    }

    protected open fun onBindViewHolder(holder: VH, item: ITEM, payloads: List<Any>) {
        onBindViewHolder(holder, item)
    }

    protected open fun onBindViewHolder(holder: VH, item: ITEM): Unit = Unit

    final override fun updateList(op: UpdateOp<ITEM>) {
        differ.updateList(op, dispatch = true)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    /**
     * 添加列表已更改的监听
     *
     * [ListChangedListener.onListChanged]中可以调用[removeListChangedListener]。
     */
    fun addListChangedListener(listener: ListChangedListener<ITEM>) {
        differ.addListChangedListener(listener)
    }

    /**
     * 移除列表已更改的监听
     */
    fun removeListChangedListener(listener: ListChangedListener<ITEM>) {
        differ.removeListChangedListener(listener)
    }

    fun addAdapterAttachCallback(callback: AdapterAttachCallback) {
        if (callbacks == null) {
            callbacks = arrayListOf()
        }
        if (!callbacks!!.contains(callback)) {
            callbacks!!.add(callback)
            recyclerView?.let(callback::onAttachedToRecyclerView)
        }
    }

    fun removeAdapterAttachCallback(callback: AdapterAttachCallback) {
        callbacks?.remove(callback)
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

    internal fun addViewHolderListener(listener: ViewHolderListener<in VH>) {
        if (listeners == null) {
            listeners = arrayListOf()
        }
        if (!listeners!!.contains(listener)) {
            listeners!!.add(listener)
        }
    }

    internal fun removeViewHolderListener(listener: ViewHolderListener<in VH>) {
        listeners?.remove(listener)
    }

    internal suspend fun awaitUpdateList(op: UpdateOp<ITEM>, dispatch: Boolean = true) {
        differ.awaitUpdateList(op, dispatch)
    }

    internal fun addListExecuteListener(listener: ListExecuteListener<ITEM>) {
        differ.addListExecuteListener(listener)
    }

    internal fun removeListExecuteListener(listener: ListExecuteListener<ITEM>) {
        differ.removeListExecuteListener(listener)
    }

    internal fun invalidateItemDecorations() {
        helper.invalidateItemDecorations()
    }

    @CallSuper
    override fun onViewAttachedToWindow(holder: VH) {
        spanSizeProvider.onViewAttachedToWindow(holder)
    }

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        callbacks?.reverseAccessEach { it.onAttachedToRecyclerView(recyclerView) }
        spanSizeProvider.onAttachedToRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        callbacks?.reverseAccessEach { it.onDetachedFromRecyclerView(recyclerView) }
        differ.cancelChildren()
    }

    private inner class InternalDiffItemCallback : DiffUtil.ItemCallback<ITEM>() {
        @WorkerThread
        override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@ListAdapter.areItemsTheSame(oldItem, newItem)
        }

        @WorkerThread
        override fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@ListAdapter.areContentsTheSame(oldItem, newItem)
        }

        @WorkerThread
        override fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? {
            return this@ListAdapter.getChangePayload(oldItem, newItem)
        }
    }
}