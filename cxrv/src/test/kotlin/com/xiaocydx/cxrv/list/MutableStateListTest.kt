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

import android.os.Build
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MutableStateList]的单元测试
 *
 * @author xcc
 * @date 2023/11/20
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class MutableStateListTest {

    @Test
    fun isEmpty() {
        val list = MutableStateList<String>()
        assertThat(list).isEmpty()
    }

    @Test
    fun getThrow() {
        val list = MutableStateList<String>()
        val result = runCatching { list[1] }
        assertThat(result.exceptionOrNull()).isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun init() {
        val list = MutableStateList(listOf("A"))
        assertThat(list.modification).isEqualTo(1)
    }

    @Test
    fun submit() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        list.submit(listOf("D", "F", "G"))
        assertThat(list).isEqualTo(listOf("D", "F", "G"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun addToLast() {
        val list = MutableStateList<String>()
        val modification = list.modification
        assertThat(list.add("A")).isTrue()
        assertThat(list.last()).isEqualTo("A")
        assertThat(list.modification).isEqualTo(modification + 1)
    }

    @Test
    fun addByIndex() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        list.add(1, "D")
        assertThat(list[1]).isEqualTo("D")
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun addAllToLast() {
        val list = MutableStateList<String>()
        val modification = list.modification

        assertThat(list.addAll(listOf("A", "B", "C"))).isTrue()
        assertThat(list).isEqualTo(listOf("A", "B", "C"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun addAllByIndex() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        assertThat(list.addAll(1, listOf("D", "E"))).isTrue()
        assertThat(list).isEqualTo(listOf("A", "D", "E", "B", "C"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun set() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        list[1] = "D"
        assertThat(list[1]).isEqualTo("D")
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun setAll() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        assertThat(list.setAll(1, listOf("D", "E", "F"))).isTrue()
        assertThat(list).isEqualTo(listOf("A", "D", "E"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun remove() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        assertThat(list.remove("A")).isTrue()
        assertThat(list).isEqualTo(listOf("B", "C"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun removeAt() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        assertThat(list.removeAt(0)).isEqualTo("A")
        assertThat(list).isEqualTo(listOf("B", "C"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun removeIf() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        assertThat(list.removeIf { it == "A" }).isTrue()
        assertThat(list).isEqualTo(listOf("B", "C"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun removeAll() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        assertThat(list.removeAll(listOf("B", "C", "D"))).isTrue()
        assertThat(list).isEqualTo(listOf("A"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun removeAllByIndex() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        assertThat(list.removeAll(1, 2)).isTrue()
        assertThat(list).isEqualTo(listOf("A"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun retainAll() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C", "E", "F", "G"))
        val modification = list.modification

        assertThat(list.retainAll(listOf("B", "F"))).isTrue()
        assertThat(list).isEqualTo(listOf("B", "F"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun clear() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val modification = list.modification

        list.clear()
        assertThat(list).isEmpty()
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun lastIndexOf() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        assertThat(list.lastIndexOf("A")).isEqualTo(0)
        assertThat(list.lastIndexOf("B")).isEqualTo(1)
        assertThat(list.lastIndexOf("C")).isEqualTo(2)
    }

    @Test
    fun indexOf() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        assertThat(list.indexOf("A")).isEqualTo(0)
        assertThat(list.indexOf("B")).isEqualTo(1)
        assertThat(list.indexOf("C")).isEqualTo(2)
    }

    @Test
    fun contains() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        assertThat(list.contains("A")).isTrue()
        assertThat(list.contains("B")).isTrue()
        assertThat(list.contains("C")).isTrue()
    }

    @Test
    fun containsAll() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        assertThat(list.containsAll(listOf("B", "C", "D"))).isFalse()
        assertThat(list.containsAll(listOf("B", "C"))).isTrue()
    }

    @Test
    fun iterator() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val outcome = mutableListOf<String>()
        val iterator = list.iterator()
        while (iterator.hasNext()) {
            outcome.add(iterator.next())
        }
        assertThat(outcome).isEqualTo(listOf("A", "B", "C"))
    }

    @Test
    fun listIterator() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val outcome = mutableListOf<String>()
        var iterator = list.listIterator()
        while (iterator.hasNext()) {
            outcome.add(iterator.next())
        }
        assertThat(outcome).isEqualTo(listOf("A", "B", "C"))

        outcome.clear()
        iterator = list.listIterator(list.size)
        while (iterator.hasPrevious()) {
            outcome.add(iterator.previous())
        }
        assertThat(outcome).isEqualTo(listOf("C", "B", "A"))
    }

    @Test
    fun listIteratorModify() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C", "D"))
        var iterator = list.listIterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element == "B") iterator.remove()
        }
        assertThat(list).isEqualTo(listOf("A", "C", "D"))

        iterator = list.listIterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element == "C") iterator.set("B")
        }
        assertThat(list).isEqualTo(listOf("A", "B", "D"))

        iterator = list.listIterator()
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (element == "B") iterator.add("C")
        }
        assertThat(list).isEqualTo(listOf("A", "B", "C", "D"))
    }

    @Test
    fun validateModification() {
        val list = MutableStateList<String>()
        list.addAll(listOf("A", "B", "C"))
        val iterator = list.iterator()
        val result = runCatching {
            while (iterator.hasNext()) {
                iterator.next()
                list.removeAt(0)
            }
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(ConcurrentModificationException::class.java)
    }

    @Test
    fun subListIsEmpty() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList).isNotEmpty()
        assertThat(subList).isEqualTo(listOf("B", "C"))
    }

    @Test
    fun subListGetThrow() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        val result = runCatching { subList[2] }
        assertThat(result.exceptionOrNull()).isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun subListAddToLast() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.add("E")).isTrue()
        assertThat(subList).isEqualTo(listOf("B", "C", "E"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "C", "E", "D"))
    }

    @Test
    fun subListAddByIndex() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        subList.add(1, "E")
        assertThat(subList).isEqualTo(listOf("B", "E", "C"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "E", "C", "D"))
    }

    @Test
    fun subListAddAllToLast() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.addAll(listOf("E", "F"))).isTrue()
        assertThat(subList).isEqualTo(listOf("B", "C", "E", "F"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "C", "E", "F", "D"))
    }

    @Test
    fun subListAddAllByIndex() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        subList.addAll(1, listOf("E", "F"))
        assertThat(subList).isEqualTo(listOf("B", "E", "F", "C"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "E", "F", "C", "D"))
    }

    @Test
    fun subListSet() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.set(1, "E")).isEqualTo("C")
        assertThat(subList).isEqualTo(listOf("B", "E"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "E", "D"))
    }

    @Test
    fun subListRemove() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.remove("C")).isTrue()
        assertThat(subList).isEqualTo(listOf("B"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "D"))
    }

    @Test
    fun subListRemoveAt() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.removeAt(1)).isEqualTo("C")
        assertThat(subList).isEqualTo(listOf("B"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "D"))
    }

    @Test
    fun subListRemoveAll() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.removeAll(listOf("B", "C"))).isTrue()
        assertThat(subList).isEmpty()
        assertThat(parentList).isEqualTo(listOf("A", "D"))
    }

    @Test
    fun subListRetainAll() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.retainAll(listOf("B"))).isTrue()
        assertThat(subList).isEqualTo(listOf("B"))
        assertThat(parentList).isEqualTo(listOf("A", "B", "D"))
    }

    @Test
    fun subListClear() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        subList.clear()
        assertThat(subList).isEmpty()
        assertThat(parentList).isEqualTo(listOf("A", "D"))
    }

    @Test
    fun subListLastIndexOf() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.lastIndexOf("B")).isEqualTo(0)
        assertThat(subList.lastIndexOf("C")).isEqualTo(1)
    }

    @Test
    fun subListIndexOf() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.indexOf("B")).isEqualTo(0)
        assertThat(subList.indexOf("C")).isEqualTo(1)
    }

    @Test
    fun subListContains() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.contains("B")).isTrue()
        assertThat(subList.contains("C")).isTrue()
    }

    @Test
    fun subListContainsAll() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        assertThat(subList.containsAll(listOf("B", "C", "D"))).isFalse()
        assertThat(subList.containsAll(listOf("B", "C"))).isTrue()
    }

    @Test
    fun subListIterator() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        val outcome = mutableListOf<String>()
        val iterator = subList.iterator()
        while (iterator.hasNext()) {
            outcome.add(iterator.next())
        }
        assertThat(outcome).isEqualTo(listOf("B", "C"))
    }

    @Test
    fun subListListIterator() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        val outcome = mutableListOf<String>()
        var iterator = subList.listIterator()
        while (iterator.hasNext()) {
            outcome.add(iterator.next())
        }
        assertThat(outcome).isEqualTo(listOf("B", "C"))

        outcome.clear()
        iterator = subList.listIterator(subList.size)
        while (iterator.hasPrevious()) {
            outcome.add(iterator.previous())
        }
        assertThat(outcome).isEqualTo(listOf("C", "B"))
    }

    @Test
    fun subListListIteratorModify() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)

        var result = runCatching {
            val iterator = subList.listIterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (element == "B") iterator.remove()
            }
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)

        result = runCatching {
            val iterator = subList.listIterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (element == "B") iterator.set("E")
            }
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)

        result = runCatching {
            val iterator = subList.listIterator()
            while (iterator.hasNext()) {
                val element = iterator.next()
                if (element == "B") iterator.add("E")
            }
        }
        assertThat(result.exceptionOrNull()).isInstanceOf(UnsupportedOperationException::class.java)
    }

    @Test
    fun subListSubList() {
        val parentList = MutableStateList<String>()
        parentList.addAll(listOf("A", "B", "C", "D"))
        val subList = parentList.subList(1, 3)
        val subList2 = subList.subList(0, 1)
        assertThat(subList2).hasSize(1)
        assertThat(subList2.first()).isEqualTo("B")
    }

    @Test
    fun interoperability() {
        val list = MutableStateList<String>()
        val modification = list.modification
        list.state.updateList(UpdateOp.SubmitList(listOf("A", "B", "C")), false)
        assertThat(list).isEqualTo(listOf("A", "B", "C"))
        assertThat(list.modification).isGreaterThan(modification)
    }

    @Test
    fun stateFlow(): Unit = runBlocking {
        val list = MutableStateList<String>()
        val flow = list.asStateFlow()

        val events = mutableListOf<ListEvent<String>>()
        val collectJob = launch(start = UNDISPATCHED) {
            flow.collect { data -> data.flow.toList(events) }
        }
        list.add("A")
        list.remove("A")
        delay(200)
        collectJob.cancelAndJoin()

        events.removeFirst() // 移除首次事件
        val first = events.first()
        val last = events.last()
        assertThat(first.version).isEqualTo(1)
        assertThat(first.op).isEqualTo(UpdateOp.AddItem(0, "A"))
        assertThat(last.version).isEqualTo(2)
        assertThat(last.op).isEqualTo(UpdateOp.RemoveItems<String>(0, 1))
    }
}