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

import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * [ListAdapter.doOnItemClick]的简易版本
 *
 * 1. [action]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 */
@Deprecated(
    message = "统一扩展函数命名，去除冗余重载函数，函数引用结合具名参数使用",
    replaceWith = ReplaceWith(
        expression = "doOnItemClick(action = action)",
        imports = arrayOf("com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick")
    )
)
inline fun <ITEM : Any, VH : ViewHolder> ListAdapter<out ITEM, out VH>.doOnSimpleItemClick(
    crossinline action: (item: ITEM) -> Unit
): Disposable = doOnSimpleItemClick(NO_INTERVAL, action)

/**
 * [ListAdapter.doOnItemClick]的简易版本
 *
 * 1. [action]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 *
 * @param intervalMs 执行[action]的间隔时间
 */
@Deprecated(
    message = "统一扩展函数命名，去除冗余重载函数，函数引用结合具名参数使用",
    replaceWith = ReplaceWith(
        expression = "doOnItemClick(intervalMs, action = action)",
        imports = arrayOf("com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick")
    )
)
inline fun <ITEM : Any, VH : ViewHolder> ListAdapter<out ITEM, out VH>.doOnSimpleItemClick(
    intervalMs: Long,
    crossinline action: (item: ITEM) -> Unit
): Disposable = doOnItemClick(intervalMs) { _, item -> action(item) }

/**
 * [ListAdapter.doOnLongItemClick]的简易版本
 *
 * 1. [action]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [action]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 4. 调用场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用。
 */
@Deprecated(
    message = "统一扩展函数命名，去除冗余重载函数，函数引用结合具名参数使用",
    replaceWith = ReplaceWith(
        expression = "doOnLongItemClick(action = action)",
        imports = arrayOf("com.xiaocydx.cxrv.itemclick.reduce.doOnLongItemClick")
    )
)
inline fun <ITEM : Any, VH : ViewHolder> ListAdapter<out ITEM, out VH>.doOnSimpleLongItemClick(
    crossinline action: (item: ITEM) -> Boolean
): Disposable = doOnLongItemClick { _, item -> action(item) }

/**
 * [ViewTypeDelegate.doOnItemClick]的简易版本
 *
 * 1. [action]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 */
@Deprecated(
    message = "统一扩展函数命名，去除冗余重载函数，函数引用结合具名参数使用",
    replaceWith = ReplaceWith(
        expression = "doOnItemClick(action = action)",
        imports = arrayOf("com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick")
    )
)
inline fun <ITEM : Any, VH : ViewHolder> ViewTypeDelegate<out ITEM, out VH>.doOnSimpleItemClick(
    crossinline action: (item: ITEM) -> Unit
): Disposable = doOnSimpleItemClick(NO_INTERVAL, action)

/**
 * [ViewTypeDelegate.doOnItemClick]的简易版本
 *
 * 1. [action]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 * 2. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 3. 调用场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用。
 *
 * @param intervalMs 执行[action]的间隔时间
 */
@Deprecated(
    message = "统一扩展函数命名，去除冗余重载函数，函数引用结合具名参数使用",
    replaceWith = ReplaceWith(
        expression = "doOnItemClick(intervalMs, action = action)",
        imports = arrayOf("com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick")
    )
)
inline fun <ITEM : Any, VH : ViewHolder> ViewTypeDelegate<out ITEM, out VH>.doOnSimpleItemClick(
    intervalMs: Long,
    crossinline action: (item: ITEM) -> Unit
): Disposable = doOnItemClick(intervalMs) { _, item -> action(item) }

/**
 * [ViewTypeDelegate.doOnLongItemClick]的简洁版本
 *
 * 1. [action]返回`true`表示消费长按，松手时不会触发点击。
 * 2. [action]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 * 3. 在合适的时机会清除`itemView`的状态，避免共享[RecycledViewPool]场景出现内存泄漏问题。
 * 4. 调用场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用。
 */
@Deprecated(
    message = "统一扩展函数命名，去除冗余重载函数，函数引用结合具名参数使用",
    replaceWith = ReplaceWith(
        expression = "doOnLongItemClick(action = action)",
        imports = arrayOf("com.xiaocydx.cxrv.itemclick.reduce.doOnLongItemClick")
    )
)
inline fun <ITEM : Any, VH : ViewHolder> ViewTypeDelegate<out ITEM, out VH>.doOnSimpleLongItemClick(
    crossinline action: (item: ITEM) -> Boolean
): Disposable = doOnLongItemClick { _, item -> action(item) }