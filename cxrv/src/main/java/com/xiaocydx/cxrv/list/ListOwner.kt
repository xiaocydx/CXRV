package com.xiaocydx.cxrv.list

import androidx.annotation.MainThread

/**
 * 列表所有者
 *
 * @author xcc
 * @date 2021/9/11
 */
interface ListOwner<out T : Any> {
    /**
     * 当前列表
     *
     * 通过[getItem]、[getItemOrNull]等扩展函数可以访问列表。
     */
    val currentList: List<T>

    /**
     * 更新列表，该函数必须在主线程调用
     *
     * 通过[submitList]、[setItem]等扩展函数可以更新列表。
     */
    @MainThread
    fun updateList(op: UpdateOp<T>)
}

/**
 * item数量
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
 * @param newList 需要是新的列表对象，若传入旧的列表对象，则不会更改。
 * 若[newList]的类型是[SafeMutableList]，则表示可作为内部的可变列表。
 */
@MainThread
fun <T : Any> ListOwner<T>.submitList(newList: List<T>) {
    updateList(UpdateOp.SubmitList(newList))
}

/**
 * 设置item，该函数必须在主线程调用
 *
 * **注意**：当[item]为新对象时，才能跟旧对象进行内容对比。
 *
 * @param position 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作
 */
@MainThread
fun <T : Any> ListOwner<T>.setItem(position: Int, item: T) {
    updateList(UpdateOp.SetItem(position, item))
}

/**
 * 添加item，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size]，越界时不会抛出异常，仅作为无效操作。
 */
@MainThread
fun <T : Any> ListOwner<T>.addItem(position: Int, item: T) {
    updateList(UpdateOp.AddItem(position, item))
}

/**
 * 添加items，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size]，越界时不会抛出异常，仅作为无效操作。
 */
@MainThread
fun <T : Any> ListOwner<T>.addItems(position: Int, items: List<T>) {
    updateList(UpdateOp.AddItems(position, items))
}

/**
 * 移除起始下标为[position]的[itemCount]个item，该函数必须在主线程调用
 *
 * @param position  取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @param itemCount 小于1不会抛出异常，仅作为无效操作。
 */
@MainThread
fun <T : Any> ListOwner<T>.removeItems(position: Int, itemCount: Int) {
    updateList(UpdateOp.RemoveItems(position, itemCount))
}

/**
 * 交换下标为[fromPosition]和[toPosition]的item，该函数必须在主线程调用
 *
 * @param fromPosition 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @param toPosition   取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 */
@MainThread
fun ListOwner<*>.swapItem(fromPosition: Int, toPosition: Int) {
    updateList(UpdateOp.SwapItem(fromPosition, toPosition))
}

/**
 * 往首位插入item，该函数必须在主线程调用
 */
@MainThread
fun <T : Any> ListOwner<T>.insertItem(item: T) {
    addItem(0, item)
}

/**
 * 往首位插入items，该函数必须在主线程调用
 */
@MainThread
fun <T : Any> ListOwner<T>.insertItems(items: List<T>) {
    addItems(0, items)
}

/**
 * 移除下标为[position]的item，该函数必须在主线程调用
 *
 * @param position 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 */
@MainThread
fun ListOwner<*>.removeItemAt(position: Int) {
    removeItems(position, itemCount = 1)
}

/**
 * 移除item，该函数必须在主线程调用
 */
@MainThread
fun <T : Any> ListOwner<T>.removeItem(item: T) {
    removeItemAt(currentList.indexOfFirst { it === item })
}

/**
 * 清空列表，该函数必须在主线程调用
 */
@MainThread
fun ListOwner<*>.clear() {
    submitList(emptyList())
}

/**
 * 提交更改的列表，该函数必须在主线程调用
 *
 * ```
 * listOwner.submitChange {
 *     removeFirst()
 * }
 * ```
 */
@MainThread
inline fun <T : Any> ListOwner<T>.submitChange(change: MutableList<T>.() -> Unit) {
    currentList.toSafeMutableList().apply(change).also(::submitList)
}

/**
 * 提交转换的列表，该函数必须在主线程调用
 *
 * ```
 * listOwner.submitTransform {
 *     filter{...}.map{...}
 * }
 * ```
 */
@MainThread
inline fun <T : Any> ListOwner<T>.submitTransform(transform: MutableList<T>.() -> List<T>) {
    currentList.toSafeMutableList().transform().also(::submitList)
}

/**
 * 遍历[ListOwner.currentList]，设置[block]返回的第一个不空的item，该函数必须在主线程调用
 */
@MainThread
inline fun <T : Any> ListOwner<T>.setFirstNotNull(block: (item: T) -> T?) {
    for (position in currentList.indices) {
        val item = block(getItem(position))
        if (item != null) {
            setItem(position, item)
            return
        }
    }
}

/**
 * 反向遍历[ListOwner.currentList]，设置[block]返回的最后一个不空的item，该函数必须在主线程调用
 */
@MainThread
inline fun <T : Any> ListOwner<T>.setLastNotNull(block: (item: T) -> T?) {
    for (position in currentList.size - 1 downTo 0) {
        val item = block(getItem(position))
        if (item != null) {
            setItem(position, item)
            return
        }
    }
}

/**
 * 返回安全的可变列表
 *
 * 这是和调用者之间的约定，返回的列表对[ListOwner.submitList]提交后，
 * 不会再被其它地方修改，用于[ListOwner]的实现类减少列表copy次数。
 */
fun <T> Collection<T>.toSafeMutableList(): SafeMutableList<T> {
    return SafeMutableList(this)
}

/**
 * 安全的可变列表
 *
 * 这是和调用者之间的约定，该列表对[ListOwner.submitList]提交后，
 * 不会再被其它地方修改，用于[ListOwner]的实现类减少列表copy次数。
 */
open class SafeMutableList<T> : ArrayList<T> {
    constructor() : super()
    constructor(initialCapacity: Int) : super(initialCapacity)
    constructor(collection: Collection<T>) : super(collection)
}