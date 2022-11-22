package com.xiaocydx.cxrv.payload

import androidx.annotation.IntRange

/**
 * 构建保存多个`value`的Payload对象
 *
 * ```
 * class FooAdapter : RecyclerView.Adapter<ViewHolder>() {
 *
 *     // 第一步：调用Payload.value()生成value
 *     companion object {
 *         val AVATAR = Payload.value(1)
 *         val USERNAME = Payload.value(2)
 *     }
 *
 *     // 第二步：构建Payload对象通知更新
 *     fun setItem(position: Int, item: Foo) {
 *         items[position] = item
 *         val payload = Payload {
 *             add(AVATAR)
 *             add(USERNAME)
 *         }
 *         notifyItemChanged(position, payload)
 *     }
 *
 *     // 第三步：遍历Payload对象的value执行更新
 *     override fun onBindViewHolder(
 *         holder: ViewHolder,
 *         position: Int,
 *         payloads: List<Any>
 *     ) = Payload.takeOrEmpty(payloads) { value ->
 *         when(value) {
 *             AVATAR -> updateAvatar()
 *             USERNAME -> updateUsername()
 *             else -> updateAvatar().updateUsername()
 *         }
 *     }
 * }
 * ```
 */
inline fun Payload(block: Payload.() -> Unit): Payload {
    return Payload.obtain().apply(block).complete()
}

/**
 * 构建比较[oldItem]和[newItem]，保存多个`value`的Payload对象
 *
 * ```
 * class FooAdapter : RecyclerView.Adapter<ViewHolder>() {
 *
 *     // 第一步：调用Payload.value()生成value
 *     companion object {
 *         val AVATAR = Payload.value(1)
 *         val USERNAME = Payload.value(2)
 *     }
 *
 *     // 第二步：构建Payload对象通知更新
 *     private val diffItemCallback = object : DiffUtil.ItemCallback<Foo>() {
 *         override fun getChangePayload(
 *             oldItem: CountItem,
 *             newItem: CountItem
 *         ): Any = Payload(oldItem, newItem) {
 *             ifNotEquals { avatar }.add(AVATAR)
 *             ifNotEquals { username }.add(USERNAME)
 *         }
 *     }
 *
 *     // 第三步：遍历Payload对象的value执行更新
 *     override fun onBindViewHolder(
 *         holder: ViewHolder,
 *         position: Int,
 *         payloads: List<Any>
 *     ) = Payload.takeOrEmpty(payloads) { value ->
 *         when(value) {
 *             AVATAR -> updateAvatar()
 *             USERNAME -> updateUsername()
 *             else -> updateAvatar().updateUsername()
 *         }
 *     }
 * }
 * ```
 */
inline fun <T : Any> Payload(oldItem: T, newItem: T, block: DiffPayload<T>.() -> Unit): Payload {
    return Payload.obtainDiff(oldItem, newItem).apply(block).complete()
}

/**
 * 若`oldItem`和`newItem`的指定属性不相等，则调用[Payload.add]添加`value`
 *
 * ```
 * val PROPERTY1 = Payload.value(1)
 * val PROPERTY2 = Payload.value(2)
 *
 * Payload(oldItem, newItem) {
 *     ifNotEquals { property1 }.add(PROPERTY1)
 *     ifNotEquals { property2 }.add(PROPERTY2)
 * }
 * ```
 *
 * **注意**：按照注释说明调用该函数，[IfNotEquals]不会产生装箱开销。
 */
inline fun <T : Any, P> DiffPayload<T>.ifNotEquals(property: T.() -> P): IfNotEquals {
    return IfNotEquals(if (oldItem().property() != newItem().property()) this else null)
}

/**
 * 基于[Payload.BASE]生成`value`
 *
 * ```
 * val VALUE1 = Payload.value(1)
 * val VALUE2 = Payload.value(2)
 * val VALUE3 = Payload.value(3)
 * ```
 */
fun Payload.Companion.value(@IntRange(from = 1, to = 31) bitCount: Int) = BASE shl bitCount

/**
 * 提取[payloads]中的[Payload]，对每个`value`执行[action]，
 * 若没有提取出[Payload]，或者提取的[Payload]都为空，则按[Payload.EMPTY]执行一次[action]。
 *
 * **注意**：调用该函数后，[payloads]中的[Payload]会被回收，因此[payloads]不能再作为该函数的实参。
 */
inline fun Payload.Companion.takeOrEmpty(payloads: List<Any>, action: (value: Int) -> Unit) {
    merge(payloads).apply { if (isEmpty()) action(EMPTY) else forEach(action) }.recycle()
}