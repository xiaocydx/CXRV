package com.xiaocydx.cxrv.multitype

import com.xiaocydx.cxrv.list.ListAdapter

/**
 * 多类型作用域
 */
abstract class MultiTypeScope<T : Any> : MutableMultiType<T>() {

    /**
     * 列表适配器，仅用于在作用域下构建初始化逻辑
     */
    abstract val listAdapter: ListAdapter<T, *>
}

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
    block: MultiTypeScope<Any>.() -> Unit
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
    block: MultiTypeScope<T>.() -> Unit
): ListAdapter<T, *> = MultiTypeScopeImpl<T>().apply(block).complete()

/**
 * 转换成item类型为[T]的[ListAdapter]
 */
inline fun <reified T : Any> ViewTypeDelegate<T, *>.toListAdapter(): ListAdapter<T, *> =
        MultiTypeAdapter(SingletonMultiType(T::class.java, this))