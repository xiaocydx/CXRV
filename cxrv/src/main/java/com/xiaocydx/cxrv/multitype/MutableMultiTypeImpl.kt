package com.xiaocydx.cxrv.multitype

import android.util.SparseArray
import com.xiaocydx.cxrv.internal.accessEach

/**
 * 可变的多类型容器实现类
 *
 * @author xcc
 * @date 2021/10/8
 */
@PublishedApi
internal class MutableMultiTypeImpl<T : Any> : MutableMultiType<T>() {
    private var isComplete = false
    private val types = SparseArray<Type<out T>>()
    private val typeGroups: MutableMap<Class<out T>, Any> = mutableMapOf()
    override val size: Int
        get() = types.size()

    override fun register(type: Type<out T>) {
        check(!isComplete) { "已完成多类型注册" }
        val viewType = type.delegate.viewType
        if (types.indexOfKey(viewType) < 0) {
            types.put(viewType, type)
            addToGroup(type)
        }
    }

    override fun keyAt(viewType: Int): Type<out T>? {
        return types[viewType]
    }

    override fun valueAt(index: Int): Type<out T> {
        return types.valueAt(index)
    }

    @Suppress("UNCHECKED_CAST")
    override fun itemAt(item: T): Type<out T>? {
        return when (val group = typeGroups[item.javaClass]) {
            is Type<*> -> group as Type<out T>
            is ArrayList<*> -> (group as ArrayList<Type<out T>>)
                .firstOrNull { it.delegate.typeLinker?.invoke(item) == true }
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun addToGroup(type: Type<out T>) {
        val clazz = type.clazz
        when (val group = typeGroups[clazz]) {
            null -> typeGroups[clazz] = type
            is Type<*> -> arrayListOf<Type<out T>>().also {
                typeGroups[clazz] = it
                it.add(group as Type<out T>)
                it.add(type)
            }
            is ArrayList<*> -> (group as ArrayList<Type<out T>>).add(type)
        }
    }

    /**
     * 检查一对多类型分组的[ViewTypeDelegate]是否都设置了typeLinker，
     * 若有[ViewTypeDelegate]未设置typeLinker，则抛出[IllegalArgumentException]异常。
     */
    @Suppress("UNCHECKED_CAST")
    private fun checkTypeGroups() {
        typeGroups.values.forEach { group ->
            (group as? ArrayList<Type<out T>>)
                ?.accessEach { checkTypeLinker(it, group) }
        }
    }

    private fun checkTypeLinker(type: Type<out T>, group: ArrayList<Type<out T>>) {
        requireNotNull(type.delegate.typeLinker) {
            "对class = ${type.clazz.canonicalName}" +
                    "注册了${group.map { it.delegate.javaClass.simpleName }}，" +
                    "属于一对多关系，请对[${type.delegate.javaClass.simpleName}]设置typeLinker。"
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun ArrayList<Type<out T>>.firstOrNull(
        predicate: (Type<T>) -> Boolean
    ): Type<out T>? {
        accessEach { if (predicate(it as Type<T>)) return it }
        return null
    }

    fun complete(): MultiType<T> {
        if (!isComplete) {
            isComplete = true
            checkTypeGroups()
        }
        return this
    }
}