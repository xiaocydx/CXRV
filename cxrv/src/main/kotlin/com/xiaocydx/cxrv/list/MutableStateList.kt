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

import androidx.annotation.VisibleForTesting

/**
 * @author xcc
 * @date 2023/11/20
 */
class MutableStateList<T : Any>
@VisibleForTesting
internal constructor(
    @PublishedApi
    internal val state: ListState<T>
) : MutableList<T> {
    internal var modification = 0

    override val size: Int
        get() = state.size

    constructor(elements: Collection<T> = emptyList()) : this(ListState()) {
        if (elements.isNotEmpty()) submitList(elements.toList())
    }

    init {
        state.addSucceedListeners { modification++ }
    }

    fun asFlow() = state.asFlow()

    fun submitList(newList: List<T>): Boolean {
        return state.submitList(newList).getOrThrow()
    }

    inline fun submitChange(change: MutableList<T>.() -> Unit): Boolean {
        return state.submitChange(change).getOrThrow()
    }

    inline fun submitTransform(transform: MutableList<T>.() -> List<T>): Boolean {
        return state.submitTransform(transform).getOrThrow()
    }

    fun moveAt(fromIndex: Int, toIndex: Int): Boolean {
        return state.moveItem(fromIndex, toIndex).getOrThrow()
    }

    override fun isEmpty() = size == 0

    override fun get(index: Int): T {
        return state.getItem(index)
    }

    override fun add(element: T): Boolean {
        return state.addItem(size, element).getOrThrow()
    }

    override fun add(index: Int, element: T) {
        state.addItem(index, element)
    }

    override fun addAll(elements: Collection<T>) = addAll(size, elements)

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        return state.addItems(index, elements.toList()).getOrThrow()
    }

    override fun set(index: Int, element: T): T {
        val previous = get(index)
        state.setItem(index, element)
        return previous
    }

    override fun remove(element: T): Boolean {
        return state.removeItem(element).getOrThrow()
    }

    override fun removeAt(index: Int): T {
        val removed = get(index)
        state.removeItemAt(index)
        return removed
    }

    fun removeAll(index: Int, count: Int): Boolean {
        return state.removeItems(index, count).getOrThrow()
    }

    override fun removeAll(elements: Collection<T>) = submitChange { removeAll(elements) }

    override fun retainAll(elements: Collection<T>) = submitChange { retainAll(elements) }

    internal fun removeAllInRange(elements: Collection<T>, start: Int, end: Int): Int {
        val previousSize = size
        state.submitChange { subList(start, end).removeAll(elements) }
        return previousSize - size
    }

    internal fun retainAllInRange(elements: Collection<T>, start: Int, end: Int): Int {
        val previousSize = size
        state.submitChange { subList(start, end).retainAll(elements) }
        return previousSize - size
    }

    override fun clear() {
        state.clear()
    }

    override fun lastIndexOf(element: T): Int {
        return state.currentList.lastIndexOf(element)
    }

    override fun indexOf(element: T): Int {
        return state.currentList.indexOf(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return state.currentList.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return state.currentList.contains(element)
    }

    override fun iterator(): MutableIterator<T> = listIterator()

    override fun listIterator(): MutableListIterator<T> {
        return MutableStateListIterator(this, offset = 0)
    }

    override fun listIterator(index: Int): MutableListIterator<T> {
        return MutableStateListIterator(this, offset = index)
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        require(fromIndex in 0..toIndex && toIndex <= size)
        return MutableStateSubList(this, fromIndex, toIndex)
    }

    @PublishedApi
    internal fun UpdateResult.getOrThrow() = requireNotNull(get())
}

private class MutableStateListIterator<T : Any>(
    private val list: MutableStateList<T>,
    offset: Int
) : MutableListIterator<T> {
    private var index = offset - 1
    private var modification = list.modification

    override fun hasNext() = index < list.size - 1

    override fun next(): T {
        validateModification()
        val newIndex = index + 1
        return list[newIndex].also { index = newIndex }
    }

    override fun hasPrevious() = index >= 0

    override fun nextIndex() = index + 1

    override fun previous(): T {
        validateModification()
        return list[index].also { index-- }
    }

    override fun previousIndex() = index

    override fun remove() {
        validateModification()
        list.removeAt(index)
        index--
        modification = list.modification
    }

    override fun set(element: T) {
        validateModification()
        list[index] = element
        modification = list.modification
    }

    override fun add(element: T) {
        validateModification()
        list.add(index + 1, element)
        index++
        modification = list.modification
    }

    private fun validateModification() {
        if (list.modification != modification) {
            throw ConcurrentModificationException()
        }
    }
}

private class MutableStateSubList<T : Any>(
    private val parentList: MutableStateList<T>,
    fromIndex: Int,
    toIndex: Int
) : MutableList<T> {
    private val offset = fromIndex
    private var modification = parentList.modification
    override var size = toIndex - fromIndex
        private set

    override fun isEmpty() = size == 0

    override fun get(index: Int): T {
        validateModification()
        return parentList[offset + index]
    }

    override fun add(element: T): Boolean {
        add(size, element)
        return true
    }

    override fun add(index: Int, element: T) {
        validateModification()
        parentList.add(offset + index, element)
        size++
        modification = parentList.modification
    }

    override fun addAll(elements: Collection<T>) = addAll(size, elements)

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        validateModification()
        val result = parentList.addAll(index + offset, elements)
        if (result) size += elements.size
        modification = parentList.modification
        return result
    }

    override fun set(index: Int, element: T): T {
        validateModification()
        val result = parentList.set(index + offset, element)
        modification = parentList.modification
        return result
    }

    override fun remove(element: T): Boolean {
        val index = indexOf(element)
        return if (index >= 0) {
            removeAt(index)
            true
        } else false
    }

    override fun removeAt(index: Int): T {
        validateModification()
        val removed = parentList.removeAt(offset + index)
        size--
        modification = parentList.modification
        return removed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        validateModification()
        val removed = parentList.removeAllInRange(elements, offset, offset + size)
        size -= removed
        modification = parentList.modification
        return removed > 0
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        validateModification()
        val removed = parentList.retainAllInRange(elements, offset, offset + size)
        size -= removed
        modification = parentList.modification
        return removed > 0
    }

    override fun clear() {
        if (isEmpty()) return
        validateModification()
        parentList.removeAll(offset, size)
        size = 0
        modification = parentList.modification
    }

    override fun lastIndexOf(element: T): Int {
        validateModification()
        var index = offset + size - 1
        while (index >= offset) {
            if (element == parentList[index]) return index - offset
            index--
        }
        return -1
    }

    override fun indexOf(element: T): Int {
        validateModification()
        (offset until offset + size).forEach {
            if (element == parentList[it]) return it - offset
        }
        return -1
    }

    override fun contains(element: T) = indexOf(element) >= 0

    override fun containsAll(elements: Collection<T>) = elements.all { contains(it) }

    override fun iterator(): MutableIterator<T> = listIterator()

    override fun listIterator(): MutableListIterator<T> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<T> {
        validateModification()
        var current = index - 1
        return object : MutableListIterator<T> {
            override fun hasPrevious() = current >= 0
            override fun nextIndex() = current + 1
            override fun previous(): T {
                val oldCurrent = current
                current = oldCurrent - 1
                return this@MutableStateSubList[oldCurrent]
            }

            override fun previousIndex() = current
            override fun add(element: T) = modificationError()
            override fun hasNext() = current < size - 1
            override fun next(): T {
                val newCurrent = current + 1
                current = newCurrent
                return this@MutableStateSubList[newCurrent]
            }

            override fun remove() = modificationError()
            override fun set(element: T) = modificationError()
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        require(fromIndex in 0..toIndex && toIndex <= size)
        validateModification()
        return MutableStateSubList(parentList, fromIndex + offset, toIndex + offset)
    }

    private fun validateModification() {
        if (parentList.modification != modification) {
            throw ConcurrentModificationException()
        }
    }

    private fun modificationError(): Nothing {
        throw UnsupportedOperationException()
    }
}