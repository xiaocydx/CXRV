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

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.xiaocydx.cxrv.internal.swap
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Test

/**
 * [calculateDiff]的单元测试
 *
 * @author xcc
 * @date 2023/9/2
 */
internal class CalculateDiffTest {

    @Test
    fun dispatchUpdatePosition() {
        val oldList = (1..10).map(::TestItem)
        val newList = ArrayList(oldList)

        // expect insert(position = 0, count = 1), order = 4
        newList.add(1, TestItem(id = 100))

        // expect move(fromPosition = 1, toPosition = 2), order = 2
        newList.swap(2, 3)

        // expect change(position = 3, count = 1, payload = null), order = 1
        newList[4] = newList[4].copy(content = "newContent")

        // expect remove(position = 0, count = 1), order = 3
        newList.removeAt(0)

        val result = oldList.calculateDiff(newList, TestItemCallback)
        val updateCallback = spyk<ListUpdateCallback>()
        result.dispatchUpdatesTo(updateCallback)
        verifySequence {
            updateCallback.onChanged(3, 1, null)
            updateCallback.onMoved(1, 2)
            updateCallback.onRemoved(0, 1)
            updateCallback.onInserted(0, 1)
        }
    }

    @Test
    fun areItemsTheSameReturnFalse() {
        val oldList = (1..10).map(::TestItem)
        val newList = ArrayList(oldList)

        // expect
        // remove(position = 0, count = 1), order = 1
        // insert(position = 0, count = 1), order = 2
        newList[0] = newList[0].copy(id = 100)

        val result = oldList.calculateDiff(newList, TestItemCallback)
        val updateCallback = spyk<ListUpdateCallback>()
        result.dispatchUpdatesTo(updateCallback)
        verifySequence {
            updateCallback.onRemoved(0, 1)
            updateCallback.onInserted(0, 1)
        }
    }

    @Test
    fun areItemsTheSameReturnTrue1() {
        val oldList = (1..10).map(::TestItem)
        val newList = ArrayList(oldList)

        // forbid
        // remove(position = 0, count = 1)
        // insert(position = 0, count = 1)
        //
        // expect
        // change(position = 0, count = 1, payload = null)
        newList[0] = newList[0].copy(content = "newContent")

        val result = oldList.calculateDiff(newList, TestItemCallback)
        val updateCallback = spyk<ListUpdateCallback>()
        result.dispatchUpdatesTo(updateCallback)
        verify(exactly = 0) { updateCallback.onRemoved(0, 1) }
        verify(exactly = 0) { updateCallback.onInserted(0, 1) }
        verify(exactly = 1) { updateCallback.onChanged(0, 1, null) }
    }

    @Test
    fun areItemsTheSameReturnTrue2() {
        val oldList = (1..10).map(::TestItem)
        val newList = ArrayList(oldList)

        // forbid
        // remove(position = 0, count = 1)
        // insert(position = 0, count = 1)
        // remove(position = 1, count = 1)
        // insert(position = 1, count = 1)
        // remove(position = 0, count = 2)
        // insert(position = 0, count = 2)
        // change(position = 0, count = 1, payload = null)
        // change(position = 1, count = 1, payload = null)
        // change(position = 0, count = 2, payload = null)
        //
        // expect
        // move(fromPosition = 0, toPosition = 1)
        newList.swap(0, 1)

        val result = oldList.calculateDiff(newList, TestItemCallback)
        val updateCallback = spyk<ListUpdateCallback>()
        result.dispatchUpdatesTo(updateCallback)
        verify(exactly = 0) { updateCallback.onRemoved(any(), any()) }
        verify(exactly = 0) { updateCallback.onInserted(any(), any()) }
        verify(exactly = 0) { updateCallback.onChanged(any(), any(), any()) }
        verify(exactly = 1) { updateCallback.onMoved(0, 1) }
    }

    private data class TestItem(val id: Int, val content: String) {
        constructor(id: Int) : this(id, id.toString())
    }

    private object TestItemCallback : DiffUtil.ItemCallback<TestItem>() {
        override fun areItemsTheSame(oldItem: TestItem, newItem: TestItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: TestItem, newItem: TestItem): Boolean {
            return oldItem == newItem
        }
    }
}