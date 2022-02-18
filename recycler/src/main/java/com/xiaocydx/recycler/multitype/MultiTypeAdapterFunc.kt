package com.xiaocydx.recycler.multitype

import com.xiaocydx.recycler.list.ListAdapter

/**
 * 构建item类型为[Any]的多类型[ListAdapter]
 *
 * ```
 * listAdapter {
 *     register(ViewTypeDelegate1())
 *     register(ViewTypeDelegate2())
 * }
 * ```
 * 详细描述可以看[MutableMultiType]。
 */
inline fun listAdapter(
    block: MutableMultiType<Any>.() -> Unit
): ListAdapter<Any, *> = listAdapter<Any>(block)

/**
 * 构建item类型为[T]的多类型[ListAdapter]
 *
 * ```
 * listAdapter<T> {
 *     register(ViewTypeDelegate1())
 *     register(ViewTypeDelegate2())
 * }
 * ```
 * 详细描述可以看[MutableMultiType]。
 */
@JvmName("listAdapterTyped")
inline fun <T : Any> listAdapter(
    block: MutableMultiType<T>.() -> Unit
): ListAdapter<T, *> = MultiTypeAdapter(multiType = MutableMultiTypeImpl<T>().init(block))

/**
 * 转换成item类型为[T]的[ListAdapter]
 */
inline fun <reified T : Any> ViewTypeDelegate<T, *>.toListAdapter(): ListAdapter<T, *> =
        MultiTypeAdapter(multiType = SingletonMultiType(T::class.java, this))