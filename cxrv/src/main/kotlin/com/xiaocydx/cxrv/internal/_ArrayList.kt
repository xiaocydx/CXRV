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

package com.xiaocydx.cxrv.internal

import java.util.*

/**
 * clone当前ArrayList再遍历
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <E> ArrayList<E>.cloneAccessEach(action: (E) -> Unit) {
    (clone() as ArrayList<E>).accessEach(action)
}

/**
 * clone当前ArrayList再遍历
 */
@Suppress("UNCHECKED_CAST")
internal inline fun <E> ArrayList<E>.cloneAccessEachIndexed(action: (index: Int, E) -> Unit) {
    (clone() as ArrayList<E>).accessEachIndexed(action)
}

/**
 * 用于频繁遍历访问元素的场景，减少迭代器对象的创建
 */
internal inline fun <E> ArrayList<E>.accessEach(action: (E) -> Unit) {
    for (index in this.indices) action(get(index))
}

/**
 * 用于频繁遍历访问元素的场景，减少迭代器对象的创建
 */
internal inline fun <E> ArrayList<E>.accessEachIndexed(action: (index: Int, E) -> Unit) {
    for (index in this.indices) action(index, get(index))
}

/**
 * 用于频繁遍历访问元素的场景，减少迭代器对象的创建
 */
internal inline fun <E> ArrayList<E>.reverseAccessEach(action: (E) -> Unit) {
    for (index in this.indices.reversed()) action(get(index))
}

internal fun <E> ArrayList<E>.toUnmodifiableList(): List<E> = Collections.unmodifiableList(this)

internal fun <E> ArrayList<E>.swap(from: Int, to: Int) = Collections.swap(this, from, to)

internal fun <E> Sequence<E>.toArrayList() = ArrayList<E>().also(::toCollection)