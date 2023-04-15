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
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [CoroutineListDiffer]的单元测试
 *
 * @author xcc
 * @date 2021/10/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class CoroutineListDifferTest {

    @Test
    fun submitList(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val job = launch { awaitCancellation() }
        val initList = listOf("A")
        differ.updateList(UpdateOp.SubmitList(initList)) {
            assertThat(differ.currentList).isEqualTo(initList)
            verify(exactly = 1) { updateCallback.onInserted(0, initList.size) }
            val newList = listOf("A", "B")
            differ.updateList(UpdateOp.SubmitList(newList)) {
                assertThat(differ.currentList).isEqualTo(newList)
                verify(atLeast = 1) { diffCallback.areItemsTheSame(initList[0], newList[0]) }
                verify(exactly = 1) { updateCallback.onInserted(initList.size, newList.size - initList.size) }
                job.cancel()
            }
        }
    }

    @Test
    fun setItem(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val job = launch { awaitCancellation() }
        val initList = listOf("A")
        differ.updateList(UpdateOp.SubmitList(initList)) {
            differ.updateList(UpdateOp.SetItem(0, "B")) {
                assertThat(differ.currentList).isEqualTo(listOf("B"))
                verify(exactly = 1) { diffCallback.areItemsTheSame("A", "B") }
                verify(exactly = 1) { updateCallback.onChanged(0, 1, null) }
                job.cancel()
            }
        }
    }

    @Test
    fun setItems(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val job = launch { awaitCancellation() }
        val initList = listOf("A", "B")
        differ.updateList(UpdateOp.SubmitList(initList)) {
            differ.updateList(UpdateOp.SetItems(0, listOf("C", "D", "E"))) {
                assertThat(differ.currentList).isEqualTo(listOf("C", "D"))
                verify(exactly = 1) { diffCallback.areItemsTheSame("A", "C") }
                verify(exactly = 1) { diffCallback.areItemsTheSame("B", "D") }
                verify(exactly = 1) { updateCallback.onChanged(0, 2, null) }
                job.cancel()
            }
        }
    }

    @Test
    fun addItem(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val job = launch { awaitCancellation() }
        val initList = listOf("A")
        differ.updateList(UpdateOp.SubmitList(initList)) {
            differ.updateList(UpdateOp.AddItem(initList.size, "B")) {
                assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onInserted(initList.size, 1) }
                job.cancel()
            }
        }
    }

    @Test
    fun addItems(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val job = launch { awaitCancellation() }
        val initList = listOf("A")
        differ.updateList(UpdateOp.SubmitList(initList)) {
            differ.updateList(UpdateOp.AddItems(initList.size, listOf("B", "C"))) {
                assertThat(differ.currentList).isEqualTo(listOf("A", "B", "C"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onInserted(initList.size, 2) }
                job.cancel()
            }
        }
    }

    @Test
    fun removeItems(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val job = launch { awaitCancellation() }
        val initList = listOf("A", "B", "C")
        differ.updateList(UpdateOp.SubmitList(initList)) {
            differ.updateList(UpdateOp.RemoveItems(position = 0, itemCount = 2)) {
                assertThat(differ.currentList).isEqualTo(listOf("C"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onRemoved(0, 2) }
                job.cancel()
            }
        }
    }

    @Test
    fun moveItem(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val job = launch { awaitCancellation() }
        val initList = listOf("A", "B", "C")
        differ.updateList(UpdateOp.SubmitList(initList)) {
            differ.updateList(UpdateOp.MoveItem(0, 2)) {
                assertThat(differ.currentList).isEqualTo(listOf("B", "C", "A"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onMoved(0, 2) }
                job.cancel()
            }
        }
    }

    @Test
    fun submitListAwait(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
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
    fun setItemAwait(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val initList = listOf("A")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.SetItem(0, "B"))
        assertThat(differ.currentList).isEqualTo(listOf("B"))
        verify(exactly = 1) { diffCallback.areItemsTheSame("A", "B") }
        verify(exactly = 1) { updateCallback.onChanged(0, 1, null) }
    }

    @Test
    fun setItemsAwait(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val initList = listOf("A", "B")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.SetItems(0, listOf("C", "D", "E")))
        assertThat(differ.currentList).isEqualTo(listOf("C", "D"))
        verify(exactly = 1) { diffCallback.areItemsTheSame("A", "C") }
        verify(exactly = 1) { diffCallback.areItemsTheSame("B", "D") }
        verify(exactly = 1) { updateCallback.onChanged(0, 2, null) }
    }

    @Test
    fun addItemAwait(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val initList = listOf("A")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.AddItem(initList.size, "B"))
        assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, 1) }
    }

    @Test
    fun addItemsAwait(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val initList = listOf("A")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.AddItems(initList.size, listOf("B", "C")))
        assertThat(differ.currentList).isEqualTo(listOf("A", "B", "C"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, 2) }
    }

    @Test
    fun removeItemsAwait(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val initList = listOf("A", "B", "C")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.RemoveItems(position = 0, itemCount = 2))
        assertThat(differ.currentList).isEqualTo(listOf("C"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onRemoved(0, 2) }
    }

    @Test
    fun moveItemAwait(): Unit = runBlockingTest {
        val (differ, diffCallback, updateCallback) = it
        val initList = listOf("A", "B", "C")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.MoveItem(0, 2))
        assertThat(differ.currentList).isEqualTo(listOf("B", "C", "A"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onMoved(0, 2) }
    }

    @Test
    fun submitListCancel(): Unit = runBlockingTest {
        val (differ, _, updateCallback) = it
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
    fun currentAwait(): Unit = runBlockingTest {
        val differ = it.differ
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        differ.updateList(UpdateOp.SubmitList(listOf("C", "D"))).await()
        assertThat(differ.currentList).isEqualTo(listOf("C", "D"))
    }

    @Test
    fun queueAwait(): Unit = runBlockingTest {
        val differ = it.differ
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        differ.updateList(UpdateOp.SubmitList(listOf("C", "D")))
        differ.updateList(UpdateOp.SetItem(0, "E"))
        differ.updateList(UpdateOp.RemoveItems(1, 1)).await()
        assertThat(differ.currentList).isEqualTo(listOf("E"))
    }

    @Test
    fun currentCancel(): Unit = runBlockingTest {
        val differ = it.differ
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        val result = differ.updateList(UpdateOp.SubmitList(listOf("C", "D")))
        differ.cancel()
        result.await()
        assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
    }

    @Test
    fun queueCancel(): Unit = runBlockingTest {
        val differ = it.differ
        differ.updateList(UpdateOp.SubmitList(listOf("A", "B"))).await()
        differ.updateList(UpdateOp.SubmitList(listOf("C", "D")))
        differ.updateList(UpdateOp.SetItem(0, "E"))
        val result = differ.updateList(UpdateOp.RemoveItems(1, 1))
        differ.cancel()
        result.await()
        assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
    }

    @Test
    fun calculateDiffOnMainThread(): Unit = runBlockingTest {
        val differ = it.differ
        differ.setDiffDispatcher(differ.mainDispatcher)
        val initList = listOf("A", "B")
        val newList = listOf("C", "D")

        var result = differ.updateList(UpdateOp.SubmitList(initList))
        assertThat(result).isEqualTo(CompleteResult)

        result = differ.updateList(UpdateOp.SubmitList(newList))
        assertThat(result).isEqualTo(CompleteResult)
        assertThat(differ.currentList).isEqualTo(newList)
    }

    @Test
    fun setDiffDispatcherCancel(): Unit = runBlockingTest {
        val differ = it.differ
        val initList = listOf("A", "B")
        val newList1 = listOf("C", "D")
        val newList2 = listOf("E", "F")

        var result = differ.updateList(UpdateOp.SubmitList(initList))
        assertThat(result).isEqualTo(CompleteResult)

        result = differ.updateList(UpdateOp.SubmitList(newList1))
        assertThat(result).isNotEqualTo(CompleteResult)

        differ.setDiffDispatcher(differ.mainDispatcher)
        result = differ.updateList(UpdateOp.SubmitList(newList2))
        assertThat(result).isEqualTo(CompleteResult)
        assertThat(differ.currentList).isEqualTo(newList2)
    }

    private fun <T> runBlockingTest(
        block: suspend CoroutineScope.(TestProperty) -> T
    ): T = runBlocking {
        val diffCallback = TestDiffCallback()
        val updateCallback = TestUpdateCallback()
        val differ = CoroutineListDiffer(
            diffCallback = diffCallback,
            updateCallback = updateCallback,
            // 设置调度延迟，模拟较长时间的差异计算。
            diffDispatcher = TestDiffDispatcher(dispatchDelay = 100),
            mainDispatcher = TestMainDispatcher(coroutineContext)
        )
        block(TestProperty(differ, diffCallback, updateCallback))
    }

    @Suppress("TestFunctionName")
    private fun TestDiffCallback(): DiffUtil.ItemCallback<String> {
        return spyk(object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem
            override fun getChangePayload(oldItem: String, newItem: String): Any? = null
        })
    }

    @Suppress("TestFunctionName")
    private fun TestUpdateCallback(): ListUpdateCallback = mockk(relaxed = true)

    private data class TestProperty(
        val differ: CoroutineListDiffer<String>,
        val diffCallback: DiffUtil.ItemCallback<String>,
        val updateCallback: ListUpdateCallback
    )
}