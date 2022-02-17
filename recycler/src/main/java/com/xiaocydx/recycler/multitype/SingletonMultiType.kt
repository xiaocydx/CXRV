@file:Suppress("UNCHECKED_CAST")

package com.xiaocydx.recycler.multitype

/**
 * 单个元素的多类型容器
 *
 * @author xcc
 * @date 2021/10/18
 */
@PublishedApi
internal class SingletonMultiType<T : Any>(
    clazz: Class<T>,
    delegate: ViewTypeDelegate<T, *>
) : MultiType<T> {
    private val type: Type<out T> = Type(clazz, delegate)
    override val size: Int = 1

    override fun keyAt(viewType: Int): Type<out T> = type

    override fun valueAt(index: Int): Type<out T> = type

    override fun itemAt(item: T): Type<out T> = type
}