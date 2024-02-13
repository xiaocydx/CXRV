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

@file:OptIn(InternalizationApi::class)

package com.xiaocydx.cxrv.list

import androidx.annotation.MainThread
import com.xiaocydx.cxrv.internal.InternalizationApi

/**
 * [MutableStateList]是[ListState]的包装类，通过复用[MutableList]的扩展函数简化代码，
 * [MutableStateList]的函数表现跟[mutableListOf]基本一致，因此传入`index`参数的函数，
 * 会检查边界，超出边界抛出[IndexOutOfBoundsException]，这一点处理跟[ListState]不同。
 *
 * 1.在ViewModel下创建[MutableStateList]，对外提供`flow`
 * ```
 * class FooViewModel : ViewModel() {
 *     private val list = MutableStateList<Foo>()
 *     val flow = list.asStateFlow()
 * }
 * ```
 *
 * 2.在视图控制器下收集`viewModel.flow`
 * ```
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          viewModel.flow
 *                 .onEach(adapter.listCollector)
 *                 .launchIn(lifecycleScope)
 *     }
 * }
 * ```
 *
 * 3.仅在视图控制器活跃期间收集`viewModel.flow`
 * ```
 * class FooActivity : AppCompatActivity() {
 *     private val viewModel: FooViewModel by viewModels()
 *     private val adapter: ListAdapter<Foo, *> = ...
 *
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *          super.onCreate(savedInstanceState)
 *          // 注意：flowWithLifecycle()在onEach(adapter.listCollector)之后调用
 *          viewModel.flow
 *               .onEach(adapter.listCollector)
 *               .flowWithLifecycle(lifecycle)
 *               .launchIn(lifecycleScope)
 *
 *          // 或者直接通过repeatOnLifecycle()进行收集，选中其中一种写法即可
 *          lifecycleScope.launch {
 *              repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                  viewModel.flow.onEach(adapter.listCollector).collect()
 *              }
 *          }
 *     }
 * }
 * ```
 *
 * @author xcc
 * @date 2023/11/20
 */
@MainThread
class MutableStateList<T : Any> constructor() : MutableList<T> {
    @PublishedApi internal val state: ListState<T> = ListState()
    internal var modification = 0; private set
    override val size: Int
        get() = state.size

    init {
        state.addUpdatedListener { modification++ }
    }

    constructor(elements: Collection<T> = emptyList()) : this() {
        if (elements.isNotEmpty()) addAll(0, elements)
    }

    override fun isEmpty() = size == 0

    override fun get(index: Int): T {
        validateRange(index, size)
        return state.getItem(index)
    }

    fun submit(newList: List<T>): Boolean {
        return state.submitList(newList).getOrThrow()
    }

    fun moveAt(fromIndex: Int, toIndex: Int): Boolean {
        validateRange(fromIndex, size)
        validateRange(toIndex, size)
        return state.moveItem(fromIndex, toIndex).getOrThrow()
    }

    override fun add(element: T): Boolean {
        add(size, element)
        return true
    }

    override fun add(index: Int, element: T) {
        validateRange(index, size, closed = true)
        state.addItem(index, element)
    }

    override fun addAll(elements: Collection<T>) = addAll(size, elements)

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        validateRange(index, size, closed = true)
        return state.addItems(index, elements.toList()).getOrThrow()
    }

    override fun set(index: Int, element: T): T {
        validateRange(index, size)
        val previous = get(index)
        state.setItem(index, element)
        return previous
    }

    fun setAll(index: Int, elements: Collection<T>): Boolean {
        validateRange(index, size)
        return state.setItems(index, elements.toList()).getOrThrow()
    }

    override fun remove(element: T): Boolean {
        return state.removeItem(element).getOrThrow()
    }

    override fun removeAt(index: Int): T {
        val removed = get(index)
        state.removeItemAt(index)
        return removed
    }

    inline fun removeIf(predicate: (T) -> Boolean): Boolean {
        return indexOfFirst(predicate).takeIf { it != -1 }?.also(::removeAt) != null
    }

    fun removeAll(index: Int, count: Int): Boolean {
        validateRange(index, size)
        return state.removeItems(index, count).getOrThrow()
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        var removed = false
        for (element in elements) {
            removed = remove(element) || removed
        }
        return removed
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        var removed = false
        for (index in indices.reversed()) {
            val element = get(index)
            if (!elements.contains(element)) {
                removeAt(index)
                removed = true
            }
        }
        return removed
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

    override fun contains(element: T): Boolean {
        return state.currentList.contains(element)
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return state.currentList.containsAll(elements)
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

    private fun UpdateResult.getOrThrow() = requireNotNull(get())
}

/**
 * 将[MutableStateList]转换为列表状态数据流
 */
fun <T : Any> MutableStateList<T>.asStateFlow() = state.asFlow()

/**
 * 将[MutableStateList]转换为只读列表
 */
fun <T : Any> MutableStateList<T>.toReadOnlyList() = state.currentList

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
        validateRange(newIndex, list.size)
        return list[newIndex].also { index = newIndex }
    }

    override fun hasPrevious() = index >= 0

    override fun nextIndex() = index + 1

    override fun previous(): T {
        validateModification()
        validateRange(index, list.size)
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
        if (list.modification == modification) return
        throw ConcurrentModificationException()
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
        validateRange(index, size)
        return parentList[offset + index]
    }

    override fun add(element: T): Boolean {
        add(size, element)
        return true
    }

    override fun add(index: Int, element: T) {
        validateModification()
        validateRange(index, size, closed = true)
        parentList.add(offset + index, element)
        size++
        modification = parentList.modification
    }

    override fun addAll(elements: Collection<T>) = addAll(size, elements)

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        validateModification()
        validateRange(index, size, closed = true)
        val result = parentList.addAll(index + offset, elements)
        if (result) size += elements.size
        modification = parentList.modification
        return result
    }

    override fun set(index: Int, element: T): T {
        validateModification()
        validateRange(index, size)
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
        validateRange(index, size)
        val removed = parentList.removeAt(offset + index)
        size--
        modification = parentList.modification
        return removed
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        validateModification()
        var removed = false
        for (element in elements) {
            removed = remove(element) || removed
        }
        return removed
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        validateModification()
        var removed = false
        for (index in indices.reversed()) {
            val element = get(index)
            if (!elements.contains(element)) {
                removeAt(index)
                removed = true
            }
        }
        return removed
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
                validateRange(oldCurrent, size)
                current = oldCurrent - 1
                return this@MutableStateSubList[oldCurrent]
            }

            override fun previousIndex() = current
            override fun add(element: T) = modificationError()
            override fun hasNext() = current < size - 1
            override fun next(): T {
                val newCurrent = current + 1
                validateRange(newCurrent, size)
                current = newCurrent
                return this@MutableStateSubList[newCurrent]
            }

            override fun remove() = modificationError()
            override fun set(element: T) = modificationError()
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> {
        validateModification()
        require(fromIndex in 0..toIndex && toIndex <= size)
        return MutableStateSubList(parentList, fromIndex + offset, toIndex + offset)
    }

    private fun validateModification() {
        if (parentList.modification == modification) return
        throw ConcurrentModificationException()
    }

    private fun modificationError(): Nothing {
        throw UnsupportedOperationException()
    }
}

private fun validateRange(index: Int, size: Int, closed: Boolean = false) {
    if (!closed) {
        if (index in 0 until size) return
        throw IndexOutOfBoundsException("index ($index) is out of bound of [0, $size)")
    } else {
        if (index in 0..size) return
        throw IndexOutOfBoundsException("index ($index) is out of bound of [0, $size]")
    }
}