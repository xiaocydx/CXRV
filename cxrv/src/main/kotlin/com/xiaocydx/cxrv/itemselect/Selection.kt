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

package com.xiaocydx.cxrv.itemselect

import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.payloads
import com.xiaocydx.cxrv.itemselect.Selection.Companion.Payload

/**
 * 列表选择功能，实现类有[SingleSelection]和[MultiSelection]
 *
 * @author xcc
 * @date 2022/4/11
 */
sealed class Selection<ITEM : Any, K : Any>(
    protected val adapter: Adapter<*>,
    protected val itemKey: (item: ITEM) -> K?,
    protected val itemAccess: Adapter<*>.(position: Int) -> ITEM
) {
    protected var onSelect: ((ITEM) -> Unit)? = null
    protected var onUnselect: ((ITEM) -> Unit)? = null

    /**
     * 通过[itemKey]获取`key`
     */
    internal val ITEM.key: K?
        get() = itemKey(this)

    /**
     * 通过[ViewHolder]获取`itemKey`
     */
    internal val ViewHolder.itemKey: K?
        get() = item?.key

    /**
     * 通过[itemAccess]获取`item`
     */
    internal val ViewHolder.item: ITEM?
        get() {
            if (bindingAdapter !== adapter) return null
            return adapter.itemAccess(bindingAdapterPosition)
        }

    /**
     * 通过[item]判断是否已选择
     */
    abstract fun isSelected(item: ITEM): Boolean

    /**
     * 通过[holder]判断是否已选择
     */
    fun isSelected(holder: ViewHolder): Boolean {
        return holder.item?.let(::isSelected) ?: false
    }

    /**
     * 通过[item]选择
     *
     * @return `true`表示选择成功，`false`表示没有`itemKey`或者选择过。
     */
    internal abstract fun select(item: ITEM, position: Int): Boolean

    /**
     * 通过[item]取消选择
     *
     * @return  `true`表示取消成功，`false`表示没有`itemKey`或者未选择过。
     */
    internal abstract fun unselect(item: ITEM, position: Int): Boolean

    /**
     * 获取[viewModel]的选择状态作为初始状态，并将后续的选择状态保存至[viewModel]
     */
    abstract fun initSelected(viewModel: ViewModel): Selection<ITEM, K>

    /**
     * 清除[viewModel]的选择状态
     */
    abstract fun clearSelected(viewModel: ViewModel): Selection<ITEM, K>

    /**
     * 调用[select]返回`true`时，执行[block]
     */
    open fun onSelect(block: (item: ITEM) -> Unit): Selection<ITEM, K> {
        onSelect = block
        return this
    }

    /**
     * 调用[unselect]返回`true`时，执行[block]
     */
    open fun onUnselect(block: (item: ITEM) -> Unit): Selection<ITEM, K> {
        onUnselect = block
        return this
    }

    private fun checkPosition(position: Int): Boolean {
        return position in 0 until adapter.itemCount
    }

    protected fun notifySelectChanged(position: Int) {
        if (!checkPosition(position)) return
        adapter.notifyItemChanged(position, Payload)
    }

    protected fun notifySelectRangeChanged(startPosition: Int, endPosition: Int) {
        if (!checkPosition(startPosition) || !checkPosition(endPosition)) return
        val itemCount = endPosition - startPosition + 1
        adapter.notifyItemRangeChanged(startPosition, itemCount, Payload)
    }

    internal fun findItemByKey(itemKey: K): ITEM? {
        for (position in 0 until adapter.itemCount) {
            val item = adapter.itemAccess(position)
            if (itemKey(item) == itemKey) return item
        }
        return null
    }

    internal fun findPositionByKey(itemKey: K): Int {
        for (position in 0 until adapter.itemCount) {
            val item = adapter.itemAccess(position)
            if (itemKey(item) == itemKey) return position
        }
        return NO_POSITION
    }

    companion object {
        const val Payload = "com.xiaocydx.cxrv.selection.Selection"
    }
}

/**
 * [holder]中是否包含[Payload]，用于[Adapter.onBindViewHolder]进行payload更新
 */
@Suppress("unused")
fun Selection<*, *>.hasPayload(holder: ViewHolder): Boolean {
    return holder.payloads.contains(Payload)
}

/**
 * 通过[holder]判断是否已选择
 */
@Deprecated(
    message = "当ITEM类型为Any时，无法推断为调用该扩展函数，因此改为成员函数",
    replaceWith = ReplaceWith("isSelected(holder)")
)
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
fun Selection<*, *>.isSelected(holder: ViewHolder): Boolean = isSelected(holder)

/**
 * 通过[item]选择
 *
 * 该函数执行效率低于`select(ViewHolder)`，用于只能通过[item]选择的情况。
 *
 * @return `true`表示选择成功，`false`表示没有`itemKey`或者选择过。
 */
fun <ITEM : Any, K : Any> Selection<ITEM, K>.select(item: ITEM): Boolean {
    val itemKey = item.key ?: return false
    if (isSelected(item)) return false
    return select(item, findPositionByKey(itemKey))
}

/**
 * 通过[holder]选择
 *
 * @return `true`表示选择成功，`false`表示没有`itemKey`或者选择过。
 */
fun Selection<*, *>.select(holder: ViewHolder): Boolean {
    val item = holder.item ?: return false
    @Suppress("UNCHECKED_CAST")
    return (this as Selection<Any, *>).select(item, holder.bindingAdapterPosition)
}

/**
 * 通过[item]取消选择
 *
 * 该函数执行效率低于`unselect(ViewHolder)`，用于只能通过[item]取消选择的情况。
 *
 * @return `true`表示取消成功，`false`表示没有`itemKey`或者未选择过。
 */
fun <ITEM : Any, K : Any> Selection<ITEM, K>.unselect(item: ITEM): Boolean {
    val itemKey = item.key ?: return false
    if (!isSelected(item)) return false
    return unselect(item, findPositionByKey(itemKey))
}

/**
 * 通过[holder]取消选择
 *
 * @return `true`表示取消成功，`false`表示没有`itemKey`或者未选择过。
 */
fun Selection<*, *>.unselect(holder: ViewHolder): Boolean {
    val item = holder.item ?: return false
    @Suppress("UNCHECKED_CAST")
    return (this as Selection<Any, *>).unselect(item, holder.bindingAdapterPosition)
}

/**
 * 通过[item]切换选择/取消选择
 *
 * 该函数执行效率低于`toggleSelect(ViewHolder)`，用于只能通过[item]切换选择/取消选择的情况。
 *
 * @return `true`表示切换成功，`false`表示切换失败。
 */
fun <ITEM : Any> Selection<ITEM, *>.toggleSelect(item: ITEM): Boolean {
    return if (!isSelected(item)) select(item) else unselect(item)
}

/**
 * 通过[holder]切换选择/取消选择
 *
 * @return `true`表示切换成功，`false`表示切换失败。
 */
fun Selection<*, *>.toggleSelect(holder: ViewHolder): Boolean {
    return if (!isSelected(holder)) select(holder) else unselect(holder)
}