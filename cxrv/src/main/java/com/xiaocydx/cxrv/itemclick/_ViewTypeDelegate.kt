package com.xiaocydx.cxrv.itemclick

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * 若触发了[target]的点击，则调用[block]
 *
 * 1. [block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 *
 * @param intervalMs 执行[block]的间隔时间
 * @param target     需要触发点击的目标视图，若返回`null`则表示不触发点击
 */
@Suppress("UNCHECKED_CAST")
inline fun <DelegateT, ITEM, VH> DelegateT.doOnItemClick(
    intervalMs: Long = NO_INTERVAL,
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttachAdapter { adapter ->
        adapter.doOnItemClick(
            intervalMs = intervalMs,
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
 * [ViewTypeDelegate.doOnItemClick]的简易版本
 *
 * 1. [block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnSimpleItemClick(NO_INTERVAL, block)
}

/**
 * [ViewTypeDelegate.doOnItemClick]的简易版本
 *
 * 1. [block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 *
 * @param intervalMs 执行[block]的间隔时间
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleItemClick(
    intervalMs: Long,
    crossinline block: (item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick(intervalMs) { _, item -> block(item) }
}

/**
 * 若触发了[target]的长按，则调用[block]
 *
 * 1. [block]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 *
 * @param target 需要触发点击的目标视图，若返回`null`则表示不触发长按
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
 * 1. [block]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏的情况。
 * 4. 调用场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}

@CheckResult
@PublishedApi
internal fun ViewHolder.ifViewTypeSame(viewType: Int): ViewHolder? {
    return if (itemViewType == viewType) this else null
}