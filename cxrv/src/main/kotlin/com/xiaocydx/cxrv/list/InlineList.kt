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

import androidx.annotation.CheckResult

/**
 * @author xcc
 * @date 2023/6/20
 */
@JvmInline
@Suppress("UNCHECKED_CAST")
internal value class InlineList<E : Any> private constructor(private val holder: Any?) {

    constructor() : this(null)

    val size: Int
        get() = when (holder) {
            null -> 0
            is ArrayList<*> -> (holder as ArrayList<E>).size
            else -> 1
        }

    operator fun get(index: Int): E = when (holder) {
        null -> throwIndexOutOfBoundsException(index)
        is ArrayList<*> -> (holder as ArrayList<E>)[index]
        else -> if (index == 0) holder as E else throwIndexOutOfBoundsException(index)
    }

    operator fun plus(element: E): InlineList<E> = when (holder) {
        null, element -> InlineList(element)
        is ArrayList<*> -> {
            holder as ArrayList<E>
            if (!holder.contains(element)) holder.add(element)
            InlineList(holder)
        }
        else -> {
            val list = ArrayList<E>(4)
            list.add(holder as E)
            list.add(element)
            InlineList(list)
        }
    }

    operator fun minus(element: E): InlineList<E> = when (holder) {
        null -> InlineList(null)
        is ArrayList<*> -> {
            holder as ArrayList<E>
            holder.remove(element)
            InlineList(holder)
        }
        else -> InlineList(if (holder == element) null else holder)
    }

    @CheckResult
    fun clear(): InlineList<E> = when (val holder = holder) {
        null -> InlineList(null)
        is ArrayList<*> -> {
            holder as ArrayList<E>
            if (holder.isNotEmpty()) holder.clear()
            InlineList(holder)
        }
        else -> InlineList(null)
    }

    fun contains(element: E): Boolean = when (holder) {
        null -> false
        is ArrayList<*> -> (holder as ArrayList<E>).contains(element)
        else -> holder == element
    }

    private fun throwIndexOutOfBoundsException(index: Int): Nothing {
        throw IndexOutOfBoundsException("Index: $index, Size: $size")
    }
}

internal inline fun <E : Any> InlineList<E>.accessEach(action: (E) -> Unit) {
    for (index in 0 until size) action(get(index))
}

internal inline fun <E : Any> InlineList<E>.reverseAccessEach(action: (E) -> Unit) {
    for (index in size - 1 downTo 0) action(get(index))
}

internal fun <E : Any> InlineList<E>.toList(): List<E> = when (size) {
    0 -> emptyList()
    1 -> listOf(get(0))
    else -> ArrayList<E>(size).apply { accessEach(::add) }
}