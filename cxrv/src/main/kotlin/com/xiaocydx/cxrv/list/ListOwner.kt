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

package com.xiaocydx.cxrv.list

import androidx.annotation.MainThread
import com.xiaocydx.cxrv.internal.InternalizationApi

/**
 * 列表所有者
 *
 * [ListOwner]的主要实现类有[ListState]和[ListAdapter]，
 * [ListState]和[ListAdapter]建立基于[ListOwner]的双向通信，
 * 通常[ListState]位于ViewModel，[ListAdapter]位于视图控制器。
 *
 * **注意**：虽然支持[ListState]和[ListAdapter]之间的双向通信，
 * 但是建议仅通过[ListState]更新列表，这能提高代码的可读性和可维护性。
 *
 * @author xcc
 * @date 2021/9/11
 */
interface ListOwner<T : Any> {

    /**
     * 当前列表
     *
     * 实现类可以替换该属性的对象，通过[getItem]、[getItemOrNull]等扩展函数可以访问列表。
     *
     * **注意**：需要确保item对象不可变，item对象不可变才能确保视图控制器恢复活跃状态时，
     * 支持[UpdateOp.SubmitList]进行差异计算的实现类（例如[ListAdapter]）能正确更新列表。
     */
    val currentList: List<T>

    /**
     * 更新列表，该函数必须在主线程调用
     *
     * 通过[submitList]、[setItem]等扩展函数可以更新列表。
     *
     * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
     * 1. 当实现类是[ListState]时，会立即执行[op]更新列表，[updateList]执行完成后，
     * [currentList]就是最新列表，[UpdateResult.await]不会挂起，而是立即返回结果。
     *
     * 2. 当实现类是[ListAdapter]时，若当前没有[UpdateOp.SubmitList]进行差异计算，
     * 则立即执行[op]更新列表，[updateList]执行完成后，[currentList]就是最新列表，
     * [UpdateResult.await]不会挂起，而是立即返回结果。
     * 若当前有[UpdateOp.SubmitList]进行差异计算，则等待[UpdateOp.SubmitList]完成后再执行[op]，
     * [updateList]执行完成后，[currentList]不是最新列表，[UpdateResult.await]会挂起等待结果。
     */
    @MainThread
    fun updateList(op: UpdateOp<T>): UpdateResult
}

/**
 * 当前列表的item数量
 */
val ListOwner<*>.size: Int
    get() = currentList.size

/**
 * 通过[position]获取item
 */
fun <T : Any> ListOwner<T>.getItem(position: Int): T = currentList[position]

/**
 * 通过[position]获取item
 *
 * @param position 取值范围[0, size)，若超过则返回`null`
 */
fun <T : Any> ListOwner<T>.getItemOrNull(position: Int): T? = currentList.getOrNull(position)

/**
 * 返回与[predicate]匹配的第一个item，如果未找到item，则返回null。
 */
inline fun <T : Any> ListOwner<T>.getItemOrNull(
    predicate: (T) -> Boolean
): T? = currentList.firstOrNull(predicate)

/**
 * [position]是否为第一个item
 */
fun ListOwner<*>.isFirstItem(position: Int): Boolean {
    return currentList.isNotEmpty() && position == 0
}

/**
 * [position]是否为最后一个item
 */
fun ListOwner<*>.isLastItem(position: Int): Boolean {
    return position == currentList.lastIndex
}

/**
 * 提交新列表，该函数必须在主线程调用
 *
 * 通过[submitChange]、[submitTransform]可以方便的更改列表。
 *
 * 当知道列表的具体更新操作时，尽量使用[setItem]、[addItem]、[removeItemAt]、[moveItem]等函数更新列表，
 * 因为[submitList]在必要时会进行差异计算，应当在合适的场景使用它，例如列表下拉刷新和页面恢复活跃状态。
 *
 * @param newList 新的列表对象，若传入旧的列表对象，则不会更改，
 * 当[ListOwner]的实现类是[ListAdapter]时，该函数会进行差异计算。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.submitList(newList: List<T>): UpdateResult {
    return updateList(UpdateOp.SubmitList(newList))
}

/**
 * 设置item，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作，
 * @param item     若是新的对象，则跟旧对象进行差异对比，否则是全量更新。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.setItem(position: Int, item: T): UpdateResult {
    return updateList(UpdateOp.SetItem(position, item))
}

/**
 * 设置items，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @param items    设置范围[position, size)，item数量越界时不会抛出异常，
 * 若item是新的对象，则跟旧对象进行差异对比，否则是全量更新。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.setItems(position: Int, items: List<T>): UpdateResult {
    return updateList(UpdateOp.SetItems(position, items))
}

/**
 * 添加item，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size]，越界时不会抛出异常，仅作为无效操作。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.addItem(position: Int, item: T): UpdateResult {
    return updateList(UpdateOp.AddItem(position, item))
}

/**
 * 添加items，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size]，越界时不会抛出异常，仅作为无效操作。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.addItems(position: Int, items: List<T>): UpdateResult {
    return updateList(UpdateOp.AddItems(position, items))
}

/**
 * 移除起始下标为[position]的[itemCount]个item，该函数必须在主线程调用
 *
 * @param position  取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @param itemCount 小于1不会抛出异常，仅作为无效操作。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun ListOwner<*>.removeItems(position: Int, itemCount: Int): UpdateResult {
    return updateList(UpdateOp.RemoveItems(position, itemCount))
}

/**
 * 下标为[fromPosition]的item移动至[toPosition]，该函数必须在主线程调用
 *
 * @param fromPosition 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @param toPosition   取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun ListOwner<*>.moveItem(fromPosition: Int, toPosition: Int): UpdateResult {
    return updateList(UpdateOp.MoveItem(fromPosition, toPosition))
}

/**
 * 交换下标为[fromPosition]和[toPosition]的item，该函数必须在主线程调用
 *
 * @param fromPosition 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @param toPosition   取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
@Deprecated(
    message = "早期理解错误，以为局部更新的move等同于swap",
    replaceWith = ReplaceWith("moveItem(fromPosition, toPosition)")
)
fun ListOwner<*>.swapItem(fromPosition: Int, toPosition: Int): UpdateResult {
    return moveItem(fromPosition, toPosition)
}

/**
 * 往首位插入item，该函数必须在主线程调用
 *
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.insertItem(item: T) = addItem(0, item)

/**
 * 往首位插入items，该函数必须在主线程调用
 *
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.insertItems(items: List<T>) = addItems(0, items)

/**
 * 移除下标为[position]的item，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun ListOwner<*>.removeItemAt(position: Int) = removeItems(position, itemCount = 1)

/**
 * 移除item，该函数必须在主线程调用
 *
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun <T : Any> ListOwner<T>.removeItem(item: T) = removeItemAt(currentList.indexOfFirst { it === item })

/**
 * 清空列表，该函数必须在主线程调用
 *
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
fun ListOwner<*>.clear() = submitList(emptyList())

/**
 * 提交更改的列表，该函数必须在主线程调用
 *
 * ```
 * listOwner.submitChange {
 *     removeFirst()
 * }
 * ```
 * 当[ListOwner]的实现类是[ListAdapter]时，该函数会进行差异计算。
 *
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
inline fun <T : Any> ListOwner<T>.submitChange(
    change: MutableList<T>.() -> Unit
): UpdateResult = currentList.toMutableList().apply(change).let(this::submitList)

/**
 * 提交转换的列表，该函数必须在主线程调用
 *
 * ```
 * listOwner.submitTransform {
 *     filter{...}.map{...}
 * }
 * ```
 * 当[ListOwner]的实现类是[ListAdapter]时，该函数会进行差异计算。
 *
 * @return 返回的[UpdateResult]，可用于等待更新完成，判断结果是成功还是失败。
 */
@MainThread
inline fun <T : Any> ListOwner<T>.submitTransform(
    transform: MutableList<T>.() -> List<T>
): UpdateResult = currentList.toMutableList().transform().let(this::submitList)

/**
 * 遍历[ListOwner.currentList]，设置[block]返回的第一个不空的item，该函数必须在主线程调用
 *
 * @param block 若返回新的对象，则跟旧对象进行差异对比，否则是全量更新。
 */
@MainThread
inline fun <T : Any> ListOwner<T>.setFirstNotNull(block: (item: T) -> T?) {
    for (position in currentList.indices) {
        val item = block(getItem(position))
        if (item != null) setItem(position, item).let { return }
    }
}

/**
 * 反向遍历[ListOwner.currentList]，设置[block]返回的最后一个不空的item，该函数必须在主线程调用
 *
 * @param block 若返回新的对象，则跟旧对象进行差异对比，否则是全量更新。
 */
@MainThread
inline fun <T : Any> ListOwner<T>.setLastNotNull(block: (item: T) -> T?) {
    for (position in currentList.indices.reversed()) {
        val item = block(getItem(position))
        if (item != null) setItem(position, item).let { return }
    }
}

/**
 * 返回安全的可变列表
 *
 * 这是和调用者之间的约定，返回的列表对[ListOwner.submitList]提交后，
 * 不会再被其它地方修改，用于[ListOwner]的实现类减少列表copy次数。
 */
@InternalizationApi
fun <T> Collection<T>.toSafeMutableList() = SafeMutableList(this)

/**
 * 安全的可变列表
 *
 * 这是和调用者之间的约定，该列表对[ListOwner.submitList]提交后，
 * 不会再被其它地方修改，用于[ListOwner]的实现类减少列表copy次数。
 */
@InternalizationApi
open class SafeMutableList<T> : ArrayList<T> {
    constructor() : super()
    constructor(initialCapacity: Int) : super(initialCapacity)
    constructor(collection: Collection<T>) : super(collection)
}