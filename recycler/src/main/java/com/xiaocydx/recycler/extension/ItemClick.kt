package com.xiaocydx.recycler.extension

import android.view.MotionEvent
import android.view.View
import android.view.View.*
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.isTouched
import com.xiaocydx.recycler.click.itemClickDispatcher
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.doOnAttach
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.multitype.ViewTypeDelegate
import java.util.*

/**
 * 若`itemView`触发点击，则调用[block]
 *
 * ```
 * recyclerView.doOnItemClick { holder, position ->
 *     ...
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 函数的实现会在合适的时机清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <RV : RecyclerView> RV.doOnItemClick(
    crossinline block: (holder: ViewHolder, position: Int) -> Unit
): RV {
    itemClickDispatcher.addItemClick(
        targetView = { itemView, _ -> itemView },
        clickHandler = { itemView ->
            itemView.holder(this)?.let { block(it, it.absoluteAdapterPosition) }
        }
    )
    return this
}

/**
 * 若`itemView`触发长按，则调用[block]
 *
 * [block]返回`true`表示消费了长按，松手时不会触发点击
 * ```
 * recyclerView.doOnLongItemClick { holder, position ->
 *     ...
 *     true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 函数的实现会在合适的时机清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <RV : RecyclerView> RV.doOnLongItemClick(
    crossinline block: (holder: ViewHolder, position: Int) -> Boolean
): RV {
    itemClickDispatcher.addLongItemClick(
        targetView = { itemView, _ -> itemView },
        longClickHandler = { itemView ->
            itemView.holder(this)?.let { block(it, it.absoluteAdapterPosition) } ?: false
        }
    )
    return this
}

/**
 * 若触发点击的[target]的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[block]
 *
 * [block]的Receiver为[adapter]，当[block]被调用时，可以根据[adapter]自身特性获取`item`：
 * ```
 * recyclerView.doOnItemClick(
 *     adapter = listAdapter,
 *     target = { targetView }
 * ) { holder, position ->
 *     val item = getItem(position)
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发点击：
 * ```
 * recyclerView.doOnItemClick(adapter) { holder, position ->
 *     val item = getItem(position)
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 * 函数的实现会在合适的时机清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <AdapterT, VH, RV> RV.doOnItemClick(
    adapter: AdapterT,
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: AdapterT.(holder: VH, position: Int) -> Unit
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    itemClickDispatcher.addItemClick(
        targetView = { itemView, event ->
            itemView.holder(adapter)?.let { it.optimizeTarget(it.target(), event) }
        },
        clickHandler = { itemView ->
            itemView.holder(adapter)?.let { adapter.block(it, it.bindingAdapterPosition) }
        }
    )
    return this
}

/**
 * 若触发长按的[target]的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[block]
 *
 * * [block]返回`true`表示消费了长按，松手时不会触发点击。
 * * [block]的Receiver为[adapter]，当[block]被调用时，可以根据[adapter]自身特性获取`item`：
 * ```
 * recyclerView.doOnLongItemClick(
 *     adapter = listAdapter,
 *     target = { targetView }
 * ) { holder, position ->
 *     val item = getItem(position)
 *     true
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发长按：
 * ```
 * recyclerView.doOnLongItemClick(adapter) { holder, position ->
 *     val item = getItem(position)
 *     true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 * 函数的实现会在合适的时机清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <AdapterT, VH, RV> RV.doOnLongItemClick(
    adapter: AdapterT,
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: AdapterT.(holder: VH, position: Int) -> Boolean
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    itemClickDispatcher.addLongItemClick(
        targetView = { itemView, event ->
            itemView.holder(adapter)?.let { it.optimizeTarget(it.target(), event) }
        },
        longClickHandler = { itemView ->
            itemView.holder(adapter)?.let { adapter.block(it, it.bindingAdapterPosition) } ?: false
        }
    )
    return this
}

/**
 * 若触发了[target]的点击，则调用[block]
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发点击：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnItemClick { holder, item ->
 *     ...
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 * 函数的实现会在合适的时机清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnItemClick(adapter = this, target) { holder, position ->
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
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 函数的实现会在合适的时机清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick { _, item -> block(item) }
}

/**
 * 若触发了[target]的长按，则调用[block]
 *
 * [block]返回`true`表示消费了长按，松手时不会触发点击
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnLongItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 *     true
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发长按：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnLongItemClick { holder, item ->
 *     ...
 *     true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 * 函数的实现会在合适的时机清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnLongItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnLongItemClick(adapter = this, target) { holder, position ->
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
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 函数的实现会在合适的时机清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}

/**
 * 若触发了[target]的点击，则调用[block]
 *
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发点击：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnItemClick { holder, item ->
 *     ...
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 * 函数的实现会在合适的时机清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
@Suppress("UNCHECKED_CAST")
inline fun <DelegateT, ITEM, VH> DelegateT.doOnItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttachAdapter { adapter ->
        adapter.doOnItemClick(
            target = {
                this.ifViewTypeSame(viewType)?.let { target(it as VH) }
            },
            block = { holder, item ->
                holder.ifViewTypeSame(viewType)?.let { block(it as VH, item as ITEM) }
            }
        )
    }
    return this
}

/**
 * [ViewTypeDelegate.doOnItemClick]的[block]简洁版本
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
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 函数的实现会在合适的时机清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick { _, item -> block(item) }
}

/**
 * 若触发了[target]的长按，则调用[block]
 *
 * [block]返回`true`表示消费了长按，松手时不会触发点击
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnLongItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 *     true
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发长按：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnLongItemClick { holder, item ->
 *     ...
 *     true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 * 函数的实现会在合适的时机清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
@Suppress("UNCHECKED_CAST")
inline fun <DelegateT, ITEM, VH> DelegateT.doOnLongItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Boolean
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttachAdapter { adapter ->
        adapter.doOnLongItemClick(
            target = {
                this.ifViewTypeSame(viewType)?.let { target(it as VH) }
            },
            block = block@{ holder, item ->
                holder.ifViewTypeSame(viewType)?.let { block(it as VH, item as ITEM) } ?: false
            }
        )
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
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 函数的实现会在合适的时机清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}

@CheckResult
@PublishedApi
internal fun View.holder(rv: RecyclerView): ViewHolder? {
    return rv.getChildViewHolder(this)
}

@CheckResult
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <VH : ViewHolder> View.holder(adapter: Adapter<VH>): VH? {
    val parent = parent as? RecyclerView ?: return null
    val holder = parent.getChildViewHolder(this) ?: return null
    return if (holder.bindingAdapter == adapter) holder as VH else null
}

@CheckResult
@PublishedApi
internal fun ViewHolder.optimizeTarget(targetView: View?, event: MotionEvent): View? = when {
    targetView == itemView -> targetView
    targetView?.isTouched(event.rawX, event.rawY) == true -> targetView
    else -> null
}

@CheckResult
@PublishedApi
internal fun ViewHolder.ifViewTypeSame(viewType: Int): ViewHolder? {
    return if (itemViewType == viewType) this else null
}