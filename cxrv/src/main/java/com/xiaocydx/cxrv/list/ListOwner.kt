package com.xiaocydx.cxrv.list

/**
 * 列表所有者
 *
 * @author xcc
 * @date 2021/9/11
 */
interface ListOwner<T : Any> {

    /**
     * 当前列表
     *
     * 通过[getItem]、[getItemOrNull]等扩展函数可以访问列表。
     */
    val currentList: List<T>

    /**
     * 更新列表
     *
     * 通过[submitList]、[setItem]等扩展函数可以更新列表。
     */
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
fun <T : Any> ListOwner<out T>.getItem(position: Int): T = currentList[position]

/**
 * 通过[position]获取item
 *
 * @param position 取值范围[0, size)，若超过则返回`null`
 */
fun <T : Any> ListOwner<out T>.getItemOrNull(position: Int): T? = currentList.getOrNull(position)

/**
 * 返回与[predicate]匹配的第一个item，如果未找到item，则返回null。
 */
inline fun <T : Any> ListOwner<out T>.getItemOrNull(
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
 * 提交新列表
 *
 * 通过[submitChange]、[submitTransform]可以方便的更改列表。
 *
 * @param newList 需要是新的列表对象，若传入旧的列表对象，则不会更改。
 */
fun <T : Any> ListOwner<T>.submitList(newList: List<T>) {
    updateList(UpdateOp.SubmitList(newList))
}

/**
 * 设置item
 *
 * **注意**：当[item]为新对象时，才能跟旧对象进行内容对比。
 *
 * @param position 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作
 */
fun <T : Any> ListOwner<T>.setItem(position: Int, item: T) {
    updateList(UpdateOp.SetItem(position, item))
}

/**
 * 添加item
 *
 * @param position 取值范围[0, size]，越界时不会抛出异常，仅作为无效操作。
 */
fun <T : Any> ListOwner<T>.addItem(position: Int, item: T) {
    updateList(UpdateOp.AddItem(position, item))
}

/**
 * 添加items
 *
 * @param position 取值范围[0, size]，越界时不会抛出异常，仅作为无效操作。
 */
fun <T : Any> ListOwner<T>.addItems(position: Int, items: List<T>) {
    updateList(UpdateOp.AddItems(position, items))
}

/**
 * 移除下标为[position]的item
 *
 * @param position 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 */
fun ListOwner<*>.removeItemAt(position: Int) {
    updateList(UpdateOp.RemoveItemAt(position))
}

/**
 * 交换下标为[fromPosition]和[toPosition]的item
 *
 * @param fromPosition 取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 * @param toPosition   取值范围[0, size)，越界时不会抛出异常，仅作为无效操作。
 */
fun ListOwner<*>.swapItem(fromPosition: Int, toPosition: Int) {
    updateList(UpdateOp.SwapItem(fromPosition, toPosition))
}

/**
 * 提交更改的列表
 *
 * ```
 * listOwner.submitChange {
 *     removeFirst()
 * }
 * ```
 */
inline fun <T : Any> ListOwner<T>.submitChange(
    change: MutableList<T>.() -> Unit
) {
    newList().apply(change).also(::submitList)
}

/**
 * 提交转换的列表
 *
 * ```
 * listOwner.submitTransform {
 *     filter{...}.map{...}
 * }
 * ```
 */
inline fun <T : Any> ListOwner<T>.submitTransform(
    transform: MutableList<T>.() -> List<T>
) {
    newList().transform().also(::submitList)
}

/**
 * 插入item
 */
fun <T : Any> ListOwner<T>.insertItem(item: T) {
    addItem(0, item)
}

/**
 * 移除item
 */
fun <T : Any> ListOwner<T>.removeItem(item: T) {
    removeItemAt(currentList.indexOfFirst { it === item })
}

/**
 * 遍历[ListOwner.currentList]，设置[block]返回的第一个不空的item
 */
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
 * 反向遍历[ListOwner.currentList]，设置[block]返回的最后一个不空的item
 */
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
 * 清空列表
 */
fun ListOwner<*>.clear() {
    submitList(emptyList())
}

internal fun <T> List<T>.ensureMutable(): MutableList<T> =
        if (this is ArrayList<T>) this else ArrayList(this)

/**
 * 创建当前列表的可变对象
 */
@PublishedApi
internal fun <T : Any> ListOwner<T>.newList(): MutableList<T> {
    return currentList.toMutableList()
}