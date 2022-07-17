package com.xiaocydx.cxrv.itemclick

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.doOnAttach
import com.xiaocydx.cxrv.list.getItemOrNull

/**
 * 若触发了[target]的点击，则调用[block]
 *
 * 1. [block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param intervalMs 执行[block]的间隔时间
 * @param target     需要触发点击的目标视图，若返回`null`则表示不触发点击
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnItemClick(
    intervalMs: Long = NO_INTERVAL,
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnItemClick(adapter = this, intervalMs, target) { holder, position ->
            getItemOrNull(position)?.let { block(holder, it) }
        }
    }
    return this
}

/**
 * [ListAdapter.doOnItemClick]的简易版本
 *
 * 1. [block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnSimpleItemClick(NO_INTERVAL, block)
}

/**
 * [ListAdapter.doOnItemClick]的简易版本
 *
 * 1. [block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 *
 * @param intervalMs 执行[block]的间隔时间
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleItemClick(
    intervalMs: Long,
    crossinline block: (item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick(intervalMs) { _, item -> block(item) }
}

/**
 * 若触发了[target]的长按，则调用[block]
 *
 * 1. [block]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param target 需要触发点击的目标视图，若返回`null`则表示不触发长按
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnLongItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnLongItemClick(adapter = this, target) { holder, position ->
            getItemOrNull(position)?.let { block(holder, it) } ?: false
        }
    }
    return this
}

/**
 * [ListAdapter.doOnLongItemClick]的简易版本
 *
 * 1. [block]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 4. 调用场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}