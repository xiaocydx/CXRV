package com.xiaocydx.cxrv.list

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AnyThread
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.concat.SpanSizeProvider
import com.xiaocydx.cxrv.concat.onAttachedToRecyclerView
import com.xiaocydx.cxrv.concat.spanSizeProvider
import com.xiaocydx.cxrv.helper.InvalidateHelper
import com.xiaocydx.cxrv.helper.ScrollHelper
import com.xiaocydx.cxrv.internal.accessEach
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.reverseAccessEach
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
    private val scrollHelper = ScrollHelper()
    private val invalidateHelper = InvalidateHelper()
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
    fun VH.setItem(item: ITEM) {
        setItem(bindingAdapterPosition, item)
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
        removeItemAt(bindingAdapterPosition)
    }

    /**
     * 对应[ItemCallback.areItemsTheSame]
     */
    @AnyThread
    protected abstract fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean

    /**
     * 对应[ItemCallback.areContentsTheSame]
     *
     * 仅当[areItemsTheSame]返回true时才调用此函数。
     */
    @AnyThread
    protected open fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean = oldItem == newItem

    /**
     * 对应[ItemCallback.getChangePayload]
     *
     * 仅当[areItemsTheSame]返回true、[areContentsTheSame]返回false时才调用此函数。
     */
    @MainThread
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

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    @MainThread
    final override fun updateList(op: UpdateOp<ITEM>) {
        assertMainThread()
        differ.updateList(op, dispatch = true)
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
        if (callbacks == null) {
            callbacks = arrayListOf()
        }
        if (!callbacks!!.contains(callback)) {
            callbacks!!.add(callback)
            recyclerView?.let(callback::onAttachedToRecyclerView)
        }
    }

    @MainThread
    fun removeAdapterAttachCallback(callback: AdapterAttachCallback) {
        assertMainThread()
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

    @CallSuper
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        registerHelper(recyclerView)
        callbacks?.reverseAccessEach { it.onAttachedToRecyclerView(recyclerView) }
        spanSizeProvider.onAttachedToRecyclerView(recyclerView)
    }

    @CallSuper
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.recyclerView = null
        unregisterHelper(recyclerView)
        callbacks?.reverseAccessEach { it.onDetachedFromRecyclerView(recyclerView) }
        differ.cancelChildren()
    }

    @MainThread
    internal fun addViewHolderListener(listener: ViewHolderListener<in VH>) {
        assertMainThread()
        if (listeners == null) {
            listeners = arrayListOf()
        }
        if (!listeners!!.contains(listener)) {
            listeners!!.add(listener)
        }
    }

    @MainThread
    internal fun removeViewHolderListener(listener: ViewHolderListener<in VH>) {
        assertMainThread()
        listeners?.remove(listener)
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

    internal suspend fun awaitUpdateList(op: UpdateOp<ITEM>, dispatch: Boolean = true) {
        differ.awaitUpdateList(op, dispatch)
    }

    private fun registerHelper(recyclerView: RecyclerView) {
        scrollHelper.register(recyclerView, this)
        invalidateHelper.register(recyclerView, this)
    }

    private fun unregisterHelper(recyclerView: RecyclerView) {
        scrollHelper.unregister(recyclerView, this)
        invalidateHelper.unregister(recyclerView, this)
    }

    private inner class InternalDiffItemCallback : DiffUtil.ItemCallback<ITEM>() {
        @AnyThread
        override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@ListAdapter.areItemsTheSame(oldItem, newItem)
        }

        @AnyThread
        override fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
            return this@ListAdapter.areContentsTheSame(oldItem, newItem)
        }

        @MainThread
        override fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? {
            return this@ListAdapter.getChangePayload(oldItem, newItem)
        }
    }
}