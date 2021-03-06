package com.xiaocydx.cxrv.list

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.testMainDispatcher
import com.xiaocydx.cxrv.testWorkDispatcher
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.CountDownLatch

/**
 * [CoroutineListDiffer]的单元测试
 *
 * @author xcc
 * @date 2021/10/12
 */
class CoroutineListDifferTest {
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
    fun execute_UpdateOp_SwapItem() {
        val count = CountDownLatch(2)
        val initList = listOf("A", "B")
        val swapItem = fun() {
            differ.updateList(UpdateOp.SwapItem(0, 1)) {
                assertThat(differ.currentList).isEqualTo(listOf("B", "A"))
                verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
                verify(exactly = 1) { updateCallback.onMoved(0, 1) }
            }
            count.countDown()
        }

        differ.updateList(UpdateOp.SubmitList(initList)) {
            swapItem()
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
    fun execute_UpdateOp_SwapItem_Await(): Unit = runBlocking {
        val initList = listOf("A", "B")
        differ.awaitUpdateList(UpdateOp.SubmitList(initList))
        differ.awaitUpdateList(UpdateOp.SwapItem(0, 1))
        assertThat(differ.currentList).isEqualTo(listOf("B", "A"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onMoved(0, 1) }
    }

    private class TestDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}