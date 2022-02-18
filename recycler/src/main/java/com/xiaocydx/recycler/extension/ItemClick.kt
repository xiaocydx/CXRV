package com.xiaocydx.recycler.extension

import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.recycler.R
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.doOnAttach
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.multitype.ViewTypeDelegate

/**
 * 添加itemView点击监听
 *
 * **注意**：在按下itemView之前，若itemView已经设置了[OnClickListener]，则[listener]不会被触发。
 */
fun RecyclerView.addOnItemClickListener(listener: OnItemClickListener) {
    dispatcher.addOnItemClickListener(listener)
}

/**
 * 移除itemView点击监听
 */
fun RecyclerView.removeOnItemClickListener(listener: OnItemClickListener) {
    dispatcher.removeOnItemClickListener(listener)
}

/**
 * 添加itemView长按监听
 *
 * [OnLongClickListener.onLongClick]返回true表示消费了长按，松手时不会触发点击。
 * **注意**：在按下itemView之前，若itemView已经设置了[OnLongClickListener]，则[listener]不会被触发。
 */
fun RecyclerView.addOnItemLongClickListener(listener: OnItemLongClickListener) {
    dispatcher.addOnItemLongClickListener(listener)
}

/**
 * 移除itemView长按监听
 */
fun RecyclerView.removeOnItemLongClickListener(listener: OnItemLongClickListener) {
    dispatcher.removeOnItemLongClickListener(listener)
}

/**
 * 若触发点击的itemView的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[block]
 *
 * [block]的Receiver为[adapter]，当[block]被调用时，可以根据[adapter]自身特性获取item：
 * ```
 * recyclerView.doOnItemClick(adapter) { holder, position ->
 *     val item = getItem(position)
 * }
 * ```
 * **注意**：在按下itemView之前，若itemView已经设置了[OnClickListener]，则[block]不会被调用。
 */
inline fun <AdapterT, VH, RV> RV.doOnItemClick(
    adapter: AdapterT,
    crossinline block: AdapterT.(holder: VH, position: Int) -> Unit
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    addOnItemClickListener listener@{
        val holder = adapter.getValidViewHolder(it) ?: return@listener
        adapter.block(holder, holder.bindingAdapterPosition)
    }
    return this
}

/**
 * 若触发长按的itemView的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[block]
 *
 * * [block]返回true表示消费了长按，松手时不会触发点击。
 * * [block]的Receiver为[adapter]，当[block]被调用时，可以根据[adapter]自身特性获取item：
 * ```
 * recyclerView.doOnLongItemClick(adapter) { holder, position ->
 *     val item = getItem(position)
 *     true
 * }
 * ```
 * **注意**：在按下itemView之前，若itemView已经设置了[OnLongClickListener]，则[block]不会被调用。
 */
inline fun <AdapterT, VH, RV> RV.doOnLongItemClick(
    adapter: AdapterT,
    crossinline block: AdapterT.(holder: VH, position: Int) -> Boolean
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    addOnItemLongClickListener listener@{
        val holder = adapter.getValidViewHolder(it) ?: return@listener false
        adapter.block(holder, holder.bindingAdapterPosition)
    }
    return this
}

/**
 * 若触发了itemView的点击，则调用[block]
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnItemClick { holder, item ->
 *     ...
 * }
 * ```
 * **注意**：在按下itemView之前，若itemView已经设置了[OnClickListener]，则[block]不会被调用。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnItemClick(
    crossinline block: (holder: VH, item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnItemClick(this) { holder, position ->
            block(holder, getItem(position))
        }
    }
    return this
}

/**
 * [ListAdapter.doOnItemClick]的简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnSimpleItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo) {
 *     ...
 * }
 * ```
 * **注意**：在按下itemView之前，若itemView已经设置了[OnClickListener]，则[block]不会被调用。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick { _, item -> block(item) }
}

/**
 * 若触发了itemView的长按，则调用[block]
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnLongItemClick { holder, item ->
 *     ...
 *     true
 * }
 * ```
 * **注意**：若在按下itemView之前，itemView已经设置了[OnLongClickListener]，则[block]不会被调用。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnLongItemClick(
    crossinline block: (holder: VH, item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnLongItemClick(this) { holder, position ->
            block(holder, getItem(position))
        }
    }
    return this
}

/**
 * [ListAdapter.doOnLongItemClick]的简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnSimpleLongItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo): Boolean {
 *     ...
 *     return true
 * }
 * ```
 * **注意**：若在按下itemView之前，itemView已经设置了[OnLongClickListener]，则[block]不会被调用。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}

/**
 * 若触发了itemView的点击，则调用[block]
 *
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnItemClick { holder, item ->
 *     ...
 * }
 * ```
 * **注意**：在按下itemView之前，若itemView已经设置了[OnClickListener]，则[block]不会被调用。
 */
@Suppress("UNCHECKED_CAST")
inline fun <DelegateT, ITEM, VH> DelegateT.doOnItemClick(
    crossinline block: (holder: VH, item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttachAdapter { adapter ->
        adapter.doOnItemClick { holder, item ->
            if (holder.itemViewType == viewType) {
                block(holder as VH, item as ITEM)
            }
        }
    }
    return this
}

/**
 * [ViewTypeDelegate.doOnItemClick]的简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnSimpleItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo) {
 *     ...
 * }
 * ```
 * **注意**：在按下itemView之前，若itemView已经设置了[OnClickListener]，则[block]不会被调用。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick { _, item -> block(item) }
}

/**
 * 若触发了itemView的长按，则调用[block]
 *
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnLongItemClick { holder, item ->
 *     ...
 *     true
 * }
 * ```
 * **注意**：若在按下itemView之前，itemView已经设置了[OnLongClickListener]，则[block]不会被调用。
 */
@Suppress("UNCHECKED_CAST")
inline fun <DelegateT, ITEM, VH> DelegateT.doOnLongItemClick(
    crossinline block: (holder: VH, item: ITEM) -> Boolean
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttachAdapter { adapter ->
        adapter.doOnLongItemClick click@{ holder, item ->
            if (holder.itemViewType == viewType) {
                return@click block(holder as VH, item as ITEM)
            }
            false
        }
    }
    return this
}

/**
 * [ViewTypeDelegate.doOnLongItemClick]的简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnSimpleLongItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo): Boolean {
 *     ...
 *     return true
 * }
 * ```
 * **注意**：若在按下itemView之前，itemView已经设置了[OnLongClickListener]，则[block]不会被调用。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}

/**
 * itemView点击分发器
 */
private val RecyclerView.dispatcher: ItemClickDispatcher
    get() {
        var dispatcher: ItemClickDispatcher? =
                getTag(R.id.tag_item_click_dispatcher) as? ItemClickDispatcher
        if (dispatcher == null) {
            dispatcher = ItemClickDispatcher(this)
            setTag(R.id.tag_item_click_dispatcher, dispatcher)
        }
        return dispatcher
    }

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <VH : ViewHolder> Adapter<VH>.getValidViewHolder(itemView: View): VH? {
    val parent = itemView.parent as? RecyclerView ?: return null
    val holder = parent.getChildViewHolder(itemView) ?: return null
    return if (holder.bindingAdapter == this) holder as VH else null
}

fun interface OnItemClickListener {
    /**
     * @param itemView 点击的itemView
     */
    fun onItemClick(itemView: View)
}

fun interface OnItemLongClickListener {
    /**
     * @param itemView 长按的itemView
     *
     * @return 返回true表示消费了长按，松手时不会触发点击，
     * 跟[OnLongClickListener.onLongClick]的返回值含义一致。
     */
    fun onItemLongClick(itemView: View): Boolean
}

private class ItemClickDispatcher(
    private val recyclerView: RecyclerView
) : SimpleOnItemTouchListener(), OnClickListener, OnLongClickListener {
    private val initialCapacity = 2
    private var itemClickListeners: ArrayList<OnItemClickListener>? = null
    private var itemLongClickListeners: ArrayList<OnItemLongClickListener>? = null
    private var View.dispatchClick: Boolean
        get() = getTag(R.id.tag_item_click) != null
        set(value) {
            if (value) {
                setTag(R.id.tag_item_click, this)
                setOnClickListener(this@ItemClickDispatcher)
            } else {
                setTag(R.id.tag_item_click, null)
                setOnClickListener(null)
            }
        }

    private var View.dispatchLongClick: Boolean
        get() = getTag(R.id.tag_item_long_click) != null
        set(value) {
            if (value) {
                setTag(R.id.tag_item_long_click, this)
                setOnLongClickListener(this@ItemClickDispatcher)
            } else {
                setTag(R.id.tag_item_long_click, null)
                setOnLongClickListener(null)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    isLongClickable = false
                }
            }
        }

    init {
        recyclerView.addOnItemTouchListener(this)
        recyclerView.addRecyclerListener {
            it.itemView.resetDispatchClick()
        }
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) {
            return false
        }
        val hasClickListener = !itemClickListeners.isNullOrEmpty()
        val hasLongClickListener = !itemLongClickListeners.isNullOrEmpty()
        if (!hasClickListener && !hasLongClickListener) {
            return false
        }
        val itemView = recyclerView
            .findChildViewUnder(event.x, event.y) ?: return false

        // 低版本根据isLongClickable判断在按下前是否已经设置了长按监听，若没有设置，则走长按分发流程，
        // 若后续没有触发长按清除分发状态，则isLongClickable会一直等于true，那么就会影响下一次的长按判断，
        // 认为在按下前已经设置了长按监听，从而跳过长按分发流程，因此在按下时重置上一次的分发状态。
        itemView.resetDispatchClick()
        if (hasClickListener && !itemView.hasOnClickListeners()) {
            itemView.dispatchClick = true
        }
        if (hasLongClickListener && !itemView.hasOnLongClickListenersCompat()) {
            itemView.dispatchLongClick = true
        }
        return false
    }

    private fun View.hasOnLongClickListenersCompat(): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> hasOnLongClickListeners()
        else -> isLongClickable
    }

    private fun View.resetDispatchClick() {
        if (dispatchClick) {
            dispatchClick = false
        }
        if (dispatchLongClick) {
            dispatchLongClick = false
        }
    }

    override fun onClick(itemView: View) {
        itemView.dispatchClick = false
        itemClickListeners?.accessEach { it.onItemClick(itemView) }
    }

    override fun onLongClick(itemView: View): Boolean {
        itemView.dispatchLongClick = false
        itemLongClickListeners?.accessEach {
            if (it.onItemLongClick(itemView)) {
                return true
            }
        }
        return false
    }

    fun addOnItemClickListener(listener: OnItemClickListener) {
        if (itemClickListeners == null) {
            itemClickListeners = ArrayList(initialCapacity)
        }
        if (!itemClickListeners!!.contains(listener)) {
            itemClickListeners!!.add(listener)
        }
    }

    fun removeOnItemClickListener(listener: OnItemClickListener) {
        itemClickListeners?.remove(listener)
    }

    fun addOnItemLongClickListener(listener: OnItemLongClickListener) {
        if (itemLongClickListeners == null) {
            itemLongClickListeners = ArrayList(initialCapacity)
        }
        if (!itemLongClickListeners!!.contains(listener)) {
            itemLongClickListeners!!.add(listener)
        }
    }

    fun removeOnItemLongClickListener(listener: OnItemLongClickListener) {
        itemLongClickListeners?.remove(listener)
    }
}