package com.xiaocydx.cxrv.itemclick

import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.isTouched

/**
 * 若`itemView`触发点击，则调用[block]
 *
 * 1. [block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 */
inline fun <RV : RecyclerView> RV.doOnItemClick(
    crossinline block: (holder: ViewHolder, position: Int) -> Unit
): RV = doOnItemClick(NO_INTERVAL, block)

/**
 * 若`itemView`触发点击，则调用[block]
 *
 * 1. [block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param intervalMs 执行[block]的间隔时间
 */
inline fun <RV : RecyclerView> RV.doOnItemClick(
    intervalMs: Long,
    crossinline block: (holder: ViewHolder, position: Int) -> Unit
): RV {
    itemClickDispatcher.addItemClick(
        intervalMs = intervalMs,
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
 * 1. [block]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
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
 * 1. [block]的Receiver为[adapter]，可以根据[adapter]自身特性获取`item`。
 * 2. [block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 * 3. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param adapter    绑定[ViewHolder]的Adapter
 * @param intervalMs 执行[block]的间隔时间
 * @param target     需要触发点击的目标视图，若返回`null`则表示不触发点击
 */
inline fun <AdapterT, VH, RV> RV.doOnItemClick(
    adapter: AdapterT,
    intervalMs: Long = NO_INTERVAL,
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: AdapterT.(holder: VH, position: Int) -> Unit
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    itemClickDispatcher.addItemClick(
        intervalMs = intervalMs,
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
 * 1. [block]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [block]的Receiver为[adapter]，可以根据[adapter]自身特性获取`item`。
 * 3. [block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 * 4. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param adapter 绑定[ViewHolder]的Adapter
 * @param target  需要触发点击的目标视图，若返回`null`则表示不触发长按
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

@PublishedApi
internal const val NO_INTERVAL = 0L

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