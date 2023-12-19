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

package com.xiaocydx.cxrv.multitype

import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * 不可变的多类型容器
 *
 * @author xcc
 * @date 2021/10/9
 */
interface MultiType<T : Any> {

    /**
     * 类型数量
     */
    val size: Int

    /**
     * 返回指定[viewType]的类型
     */
    fun keyAt(viewType: Int): Type<out T>?

    /**
     * 返回指定[index]的类型
     */
    fun valueAt(index: Int): Type<out T>

    /**
     * 返回指定[item]的类型
     */
    fun itemAt(item: T): Type<out T>?
}

/**
 * ViewType的映射类型
 */
class Type<T : Any>
@PublishedApi internal constructor(
    val clazz: Class<T>,
    val delegate: ViewTypeDelegate<T, *>
)

/**
 * 访问每个类型并执行指定操作
 */
inline fun <T : Any> MultiType<T>.forEach(action: (Type<out T>) -> Unit) {
    for (index in 0 until size) action(valueAt(index))
}

/**
 * 获取[item]对应的ViewType
 *
 * 若获取失败，则抛出[IllegalArgumentException]异常。
 */
fun <T : Any> MultiType<T>.getItemViewType(item: T): Int {
    val type = requireNotNull(itemAt(item)) {
        "获取ViewType失败，请确认是否未注册class = ${item.javaClass.canonicalName}的ViewTypeDelegate，" +
                "若已注册class，并且是一对多关系，请确认设置的typeLinker的判断逻辑是否正确。"
    }
    return type.delegate.viewType
}

/**
 * 获取[viewType]对应的[ViewTypeDelegate]
 *
 * 若获取失败，则抛出[IllegalArgumentException]异常。
 */
@Suppress("UNCHECKED_CAST")
fun <T : Any> MultiType<T>.getViewTypeDelegate(viewType: Int) = requireNotNull(
    value = keyAt(viewType)?.delegate as? ViewTypeDelegate<T, ViewHolder>,
    lazyMessage = {
        "获取ViewTypeDelegate失败，请确认是否未注册ViewType = ${viewType}的Type，" +
                "若使用了ConcatAdapter连接Adapter，请将Config的isolateViewTypes设为false，" +
                "因为注册ViewTypeDelegate时记录的是本地ViewType，获取ViewTypeDelegate时，" +
                "为了提高查找效率，会使用holder.itemViewType，即使用全局ViewType进行查找，" +
                "若全局ViewType跟记录的本地ViewType不一致，则会查找失败。"
    }
)

/**
 * 获取[holder.itemViewType]对应的[ViewTypeDelegate]
 *
 * 若获取失败，则抛出[IllegalArgumentException]异常。
 */
@Suppress("KDocUnresolvedReference")
fun <T : Any> MultiType<T>.getViewTypeDelegate(holder: ViewHolder) = getViewTypeDelegate(holder.itemViewType)