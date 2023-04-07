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

package com.xiaocydx.cxrv.itemclick

import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.isTouched
import com.xiaocydx.cxrv.list.Disposable

/**
 * 若`itemView`触发点击，则调用[action]
 *
 * 1. [action]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 */
inline fun RecyclerView.doOnItemClick(
    crossinline action: (holder: ViewHolder, position: Int) -> Unit
): Disposable = doOnItemClick(NO_INTERVAL, action)

/**
 * 若`itemView`触发点击，则调用[action]
 *
 * 1. [action]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param intervalMs 执行[action]的间隔时间
 */
inline fun RecyclerView.doOnItemClick(
    intervalMs: Long,
    crossinline action: (holder: ViewHolder, position: Int) -> Unit
): Disposable = itemClickDispatcher.addItemClick(
    intervalMs = intervalMs,
    targetView = { itemView, _ -> itemView },
    clickHandler = { itemView ->
        itemView.holder(this)?.let { action(it, it.absoluteAdapterPosition) }
    }
)

/**
 * 若`itemView`触发长按，则调用[action]
 *
 * 1. [action]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [action]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 */
inline fun RecyclerView.doOnLongItemClick(
    crossinline action: (holder: ViewHolder, position: Int) -> Boolean
): Disposable = itemClickDispatcher.addLongItemClick(
    targetView = { itemView, _ -> itemView },
    longClickHandler = { itemView ->
        itemView.holder(this)?.let { action(it, it.absoluteAdapterPosition) } ?: false
    }
)

/**
 * 若触发点击的[target]的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[action]
 *
 * 1. [action]的Receiver为[adapter]，可以根据[adapter]自身特性获取`item`。
 * 2. [action]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 * 3. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param adapter    绑定[ViewHolder]的Adapter
 * @param intervalMs 执行[action]的间隔时间
 * @param target     需要触发点击的目标视图，若返回`null`则表示不触发点击
 */
inline fun <AdapterT : Adapter<out VH>, VH : ViewHolder> RecyclerView.doOnItemClick(
    adapter: AdapterT,
    intervalMs: Long = NO_INTERVAL,
    crossinline target: VH.() -> View? = { itemView },
    crossinline action: AdapterT.(holder: VH, position: Int) -> Unit
): Disposable = itemClickDispatcher.addItemClick(
    intervalMs = intervalMs,
    targetView = { itemView, event ->
        itemView.holder(adapter)?.let { it.optimizeTarget(it.target(), event) }
    },
    clickHandler = { itemView ->
        itemView.holder(adapter)?.let { adapter.action(it, it.bindingAdapterPosition) }
    }
)

/**
 * 若触发长按的[target]的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[action]
 *
 * 1. [action]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [action]的Receiver为[adapter]，可以根据[adapter]自身特性获取`item`。
 * 3. [action]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 * 4. 在合适的时机会清除[target]的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 *
 * @param adapter 绑定[ViewHolder]的Adapter
 * @param target  需要触发点击的目标视图，若返回`null`则表示不触发长按
 */
inline fun <AdapterT : Adapter<out VH>, VH : ViewHolder> RecyclerView.doOnLongItemClick(
    adapter: AdapterT,
    crossinline target: VH.() -> View? = { itemView },
    crossinline action: AdapterT.(holder: VH, position: Int) -> Boolean
): Disposable = itemClickDispatcher.addLongItemClick(
    targetView = { itemView, event ->
        itemView.holder(adapter)?.let { it.optimizeTarget(it.target(), event) }
    },
    longClickHandler = { itemView ->
        itemView.holder(adapter)?.let { adapter.action(it, it.bindingAdapterPosition) } ?: false
    }
)

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
    return if (holder.bindingAdapter === adapter) holder as VH else null
}

@CheckResult
@PublishedApi
internal fun ViewHolder.optimizeTarget(targetView: View?, event: MotionEvent): View? = when {
    targetView === itemView -> targetView
    targetView?.isTouched(event.rawX, event.rawY) == true -> targetView
    else -> null
}