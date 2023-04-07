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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.testMainDispatcher
import com.xiaocydx.cxrv.testWorkDispatcher
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch

/**
 * [CoroutineListDiffer]的单元测试
 *
 * @author xcc
 * @date 2021/10/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class CoroutineListDifferTest {
    private val diffCallback: TestDiffCallback = spyk(TestDiffCallback())
    private val updateCallback: ListUpdateCallback = mockk(relaxed = true)
    private val differ: CoroutineListDiffer<String> = CoroutineListDiffer(
        diffCallback = diffCallback,
        updateCallback = updateCallback,
        // 设置调度延迟，模拟较长时间的差异计算。
        workDispatcher = testWorkDispatcher(dispatchDelay = 200),
        // 单元测试在主线程上执行，若runBlocking()把主线程阻塞了，
        // 则差异计算完成后无法恢复到主线程，因此用测试调度器替代主线程调度器。
        mainDispatcher = testMainDispatcher()
    )

    @Test
    fun execute_UpdateOp_SubmitList() {
        val count = CountDownLatch(2)
        val initList = listOf("A")
        val submitNewList = fun() {
            val newList = listOf("A", "B")
            differ.updateList(UpdateOp.SubmitList(newList)) {
                assertThat(differ.currentList).isEqualTo(newList)
                verify(atLeast = 1) { diffCallback.areItemsTheSame(initList[0], newList[0]) }
                verify(exactly = 1) { updateCallback.onInserted(initList.size, newList.size - initList.size) }
                count.countDown()
            }
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            assertThat(differ.currentList).isEqualTo(initList)
            verify(exactly = 1) { updateCallback.onInserted(0, initList.size) }
            submitNewList()
            count.countDown()
        }
        count.await()
    }

    @Test
    fun execute_UpdateOp_SetItem() {
        val count = CountDownLatch(2)
        val initList = listOf("A")
        val setItem = fun() {
            differ.updateList(UpdateOp.SetItem(0, "B")) {
                assertThat(differ.currentList).isEqualTo(listOf("B"))
                verify(exactly = 1) { diffCallback.areItemsTheSame("A", "B") }
                verify(exactly = 1) { updateCallback.onChanged(0, 1, null) }
                count.countDown()
            }
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            setItem()
            count.countDown()
        }
        count.await()
    }

    @Test
    fun execute_UpdateOp_SetItems() {
        val count = CountDownLatch(2)
        val initList = listOf("A", "B")
        val setItems = fun() {
            differ.updateList(UpdateOp.SetItems(0, listOf("C", "D", "E"))) {
                assertThat(differ.currentList).isEqualTo(listOf("C", "D"))
                verify(exactly = 1) { diffCallback.areItemsTheSame("A", "C") }
                verify(exactly = 1) { diffCallback.areItemsTheSame("B", "D") }
                verify(exactly = 1) { updateCallback.onChanged(0, 2, null) }
                count.countDown()
            }
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            setItems()
            count.countDown()
        }
        count.await()
    }

    @Test
    fun execute_UpdateOp_AddItem() {
        val count = CountDownLatch(2)
        val initList = listOf("A")
        val addItem = fun() {
            differ.updateList(UpdateOp.AddItem(initList.size, "B")) {
                assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onInserted(initList.size, 1) }
                count.countDown()
            }
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            addItem()
            count.countDown()
        }
        count.await()
    }

    @Test
    fun execute_UpdateOp_AddItems() {
        val count = CountDownLatch(2)
        val initList = listOf("A")
        val addItems = fun() {
            differ.updateList(UpdateOp.AddItems(initList.size, listOf("B", "C"))) {
                assertThat(differ.currentList).isEqualTo(listOf("A", "B", "C"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onInserted(initList.size, 2) }
                count.countDown()
            }
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            addItems()
            count.countDown()
        }
        count.await()
    }

    @Test
    fun execute_UpdateOp_RemoveItems() {
        val count = CountDownLatch(2)
        val initList = listOf("A", "B", "C")
        val removeItemAt = fun() {
            differ.updateList(UpdateOp.RemoveItems(position = 0, itemCount = 2)) {
                assertThat(differ.currentList).isEqualTo(listOf("C"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onRemoved(0, 2) }
                count.countDown()
            }
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            removeItemAt()
            count.countDown()
        }
        count.await()
    }

    @Test
    fun execute_UpdateOp_MoveItem() {
        val count = CountDownLatch(2)
        val initList = listOf("A", "B", "C")
        val moveItem = fun() {
            differ.updateList(UpdateOp.MoveItem(0, 2)) {
                assertThat(differ.currentList).isEqualTo(listOf("B", "C", "A"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onMoved(0, 2) }
            }
            count.countDown()
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            moveItem()
            count.countDown()
        }
        count.await()
    }

    @Test
    fun execute_UpdateOp_SubmitList_Await(): Unit = runBlocking {
        val initList = listOf("A")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        assertThat(differ.currentList).isEqualTo(initList)
        verify(exactly = 1) { updateCallback.onInserted(0, initList.size) }

        val newList = listOf("A", "B")
        differ.awaitUpdateList(UpdateOp.SubmitList(newList))
        assertThat(differ.currentList).isEqualTo(newList)
        verify(atLeast = 1) { diffCallback.areItemsTheSame(initList[0], newList[0]) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, newList.size - initList.size) }
    }

    @Test
    fun execute_UpdateOp_SetItem_Await(): Unit = runBlocking {
        val initList = listOf("A")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.SetItem(0, "B"))
        assertThat(differ.currentList).isEqualTo(listOf("B"))
        verify(exactly = 1) { diffCallback.areItemsTheSame("A", "B") }
        verify(exactly = 1) { updateCallback.onChanged(0, 1, null) }
    }

    @Test
    fun execute_UpdateOp_SetItems_Await(): Unit = runBlocking {
        val initList = listOf("A", "B")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.SetItems(0, listOf("C", "D", "E")))
        assertThat(differ.currentList).isEqualTo(listOf("C", "D"))
        verify(exactly = 1) { diffCallback.areItemsTheSame("A", "C") }
        verify(exactly = 1) { diffCallback.areItemsTheSame("B", "D") }
        verify(exactly = 1) { updateCallback.onChanged(0, 2, null) }
    }

    @Test
    fun execute_UpdateOp_AddItem_Await(): Unit = runBlocking {
        val initList = listOf("A")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.AddItem(initList.size, "B"))
        assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, 1) }
    }

    @Test
    fun execute_UpdateOp_AddItems_Await(): Unit = runBlocking {
        val initList = listOf("A")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.AddItems(initList.size, listOf("B", "C")))
        assertThat(differ.currentList).isEqualTo(listOf("A", "B", "C"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, 2) }
    }

    @Test
    fun execute_UpdateOp_RemoveItems_Await(): Unit = runBlocking {
        val initList = listOf("A", "B", "C")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.RemoveItems(position = 0, itemCount = 2))
        assertThat(differ.currentList).isEqualTo(listOf("C"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onRemoved(0, 2) }
    }

    @Test
    fun execute_UpdateOp_MoveItem_Await(): Unit = runBlocking {
        val initList = listOf("A", "B", "C")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.MoveItem(0, 2))
        assertThat(differ.currentList).isEqualTo(listOf("B", "C", "A"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onMoved(0, 2) }
    }

    @Test
    fun execute_UpdateOp_SubmitList_Cancel(): Unit = runBlocking {
        val listener = spyk(object : ListExecuteListener<String> {
            override fun onExecute(op: UpdateOp<String>) = Unit
        })
        differ.addListExecuteListener(listener)
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B")))

        // 提交新列表，进行(A, B) -> (A, B, C)的差异计算
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B", "C")))
        // 将addItemOp和removeItemOp添加到更新队列，在差异计算完成后执行
        val addItemOp = UpdateOp.AddItem(position = 0, "D")
        val removeItemOp = UpdateOp.RemoveItems<String>(position = 0, 1)
        differ.updateList(addItemOp)
        differ.updateList(removeItemOp)

        // 再次提交新列表，取消(A, B) -> (A, B, C)的差异计算和挂起的更新操作
        differ.awaitUpdateList(UpdateOp.SubmitList(emptyList()))
        assertThat(differ.currentList).isEmpty()
        // 验证取消(A, B) -> (A, B, C)的差异计算后不会添加C
        verify(exactly = 0) { updateCallback.onInserted(2, 1) }
        // 验证取消(A, B) -> (A, B, C)的差异计算后不会执行addItemOp和removeItemOp
        verify(exactly = 0) { listener.onExecute(addItemOp) }
        verify(exactly = 0) { listener.onExecute(removeItemOp) }
    }

    @Test
    fun execute_UpdateOp_Current_Await(): Unit = runBlocking {
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        differ.updateList(UpdateOp.SubmitList(listOf("C", "D"))).await()
        assertThat(differ.currentList).isEqualTo(listOf("C", "D"))
    }

    @Test
    fun execute_UpdateOp_Queue_Await(): Unit = runBlocking {
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        differ.updateList(UpdateOp.SubmitList(listOf("C", "D")))
        differ.updateList(UpdateOp.SetItem(0, "E"))
        differ.updateList(UpdateOp.RemoveItems(1, 1)).await()
        assertThat(differ.currentList).isEqualTo(listOf("E"))
    }

    @Test
    fun execute_UpdateOp_Current_Cancel(): Unit = runBlocking {
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        val result = differ.updateList(UpdateOp.SubmitList(listOf("C", "D")))
        assertThat(result is TestUpdateResult).isTrue()
        (result as TestUpdateResult).awaitBlock()
        differ.cancel()
        result.await()
        assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
    }

    @Test
    fun execute_UpdateOp_Queue_Cancel(): Unit = runBlocking {
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        differ.updateList(UpdateOp.SubmitList(listOf("C", "D")))
        differ.updateList(UpdateOp.SetItem(0, "E"))
        val result = differ.updateList(UpdateOp.RemoveItems(1, 1))
        assertThat(result is TestUpdateResult).isTrue()
        (result as TestUpdateResult).awaitBlock()
        differ.cancel()
        result.await()
        assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
    }

    @Test
    fun execute_SubmitList_On_MainThread(): Unit = runBlocking {
        val dispatcher = Dispatchers.Main
        val differ = CoroutineListDiffer(diffCallback, updateCallback, dispatcher, dispatcher)
        val initList = listOf("A", "B")
        val newList = listOf("C", "D")

        var result = differ.updateList(UpdateOp.SubmitList(initList))
        assertThat(result).isEqualTo(CompleteResult)

        result = differ.updateList(UpdateOp.SubmitList(newList))
        assertThat(result).isEqualTo(CompleteResult)
        assertThat(differ.currentList).isEqualTo(newList)
    }

    @Test
    fun setWorkDispatcher_Cancel() {
        val differ = CoroutineListDiffer(
            diffCallback, updateCallback,
            Dispatchers.Default, Dispatchers.Main
        )
        val initList = listOf("A", "B")
        val newList1 = listOf("C", "D")
        val newList2 = listOf("E", "F")

        var result = differ.updateList(UpdateOp.SubmitList(initList))
        assertThat(result).isEqualTo(CompleteResult)

        result = differ.updateList(UpdateOp.SubmitList(newList1))
        assertThat(result).isNotEqualTo(CompleteResult)

        differ.setWorkDispatcher(differ.mainDispatcher)
        result = differ.updateList(UpdateOp.SubmitList(newList2))
        assertThat(result).isEqualTo(CompleteResult)
        assertThat(differ.currentList).isEqualTo(newList2)
    }

    private class TestDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun getChangePayload(oldItem: String, newItem: String): Any? = null
    }
}