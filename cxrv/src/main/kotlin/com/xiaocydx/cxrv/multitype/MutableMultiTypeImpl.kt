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
    private val typeGroups = mutableMapOf<Class<*>, Any>()
    override val size: Int
        get() = types.size()

    override fun register(type: Type<out T>) {
        check(!isComplete) { "已完成多类型注册" }
        checkType(type)
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

    override fun itemAt(item: T): Type<out T>? {
        return itemAtInternal(item, item.javaClass)
    }

    private fun checkType(type: Type<out T>) {
        val clazz = type.clazz
        val errorType = when {
            clazz.isArray -> "Array"
            clazz.isAnnotation -> "annotation"
            clazz.isInterface -> "interface"
            else -> {
                // inline得来的class，不会是ParameterizedType，
                // 因此无法直接确定泛型实参为ParameterizedType，
                // 虽然可以通过判断class.typeParameters不为空，
                // 来间接确定泛型实参是ParameterizedType，
                // 但是这个属性的访问代价较高，不值得这么做。
                null
            }
        }
        require(errorType.isNullOrEmpty()) { "${clazz.canonicalName}是$errorType，不支持注册" }
    }

    @Suppress("UNCHECKED_CAST")
    private fun itemAtInternal(item: T, clazz: Class<*>): Type<out T>? {
        val type = when (val group = typeGroups[clazz]) {
            is Type<*> -> group as Type<out T>
            is ArrayList<*> -> (group as ArrayList<Type<out T>>)
                .firstOrNull { it.delegate.typeLinker?.invoke(item) == true }
            else -> null
        }
        return type ?: clazz.superclass?.let { itemAtInternal(item, it) }
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