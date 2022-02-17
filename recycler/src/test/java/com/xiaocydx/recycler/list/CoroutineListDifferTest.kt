package com.xiaocydx.recycler.list

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import com.xiaocydx.recycler.testMainDispatcher
import com.xiaocydx.recycler.testWorkDispatcher
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test

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
    fun execute_UpdateOp_SubmitList(): Unit = runBlocking {
        val initList = listOf("A")
        differ.updateListAwait(UpdateOp.SubmitList(initList))
        assertThat(differ.currentList).isEqualTo(initList)
        verify(exactly = 1) { updateCallback.onInserted(0, initList.size) }

        val newList = listOf("A", "B")
        differ.updateListAwait(UpdateOp.SubmitList(newList))
        assertThat(differ.currentList).isEqualTo(newList)
        verify(atLeast = 1) { diffCallback.areItemsTheSame(initList[0], newList[0]) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, newList.size - initList.size) }
    }

    @Test
    fun execute_UpdateOp_SetItem(): Unit = runBlocking {
        val initList = listOf("A")
        differ.updateListAwait(UpdateOp.SubmitList(initList))
        differ.updateListAwait(UpdateOp.SetItem(0, "B"))
        assertThat(differ.currentList).isEqualTo(listOf("B"))
        verify(exactly = 1) { diffCallback.areItemsTheSame("A", "B") }
        verify(exactly = 1) { updateCallback.onChanged(0, 1, null) }
    }

    @Test
    fun execute_UpdateOp_AddItem(): Unit = runBlocking {
        val initList = listOf("A")
        differ.updateListAwait(UpdateOp.SubmitList(initList))
        differ.updateListAwait(UpdateOp.AddItem(initList.size, "B"))
        assertThat(differ.currentList).isEqualTo(listOf("A", "B"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, 1) }
    }

    @Test
    fun execute_UpdateOp_AddItems(): Unit = runBlocking {
        val initList = listOf("A")
        differ.updateListAwait(UpdateOp.SubmitList(initList))
        differ.updateListAwait(UpdateOp.AddItems(initList.size, listOf("B", "C")))
        assertThat(differ.currentList).isEqualTo(listOf("A", "B", "C"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onInserted(initList.size, 2) }
    }

    @Test
    fun execute_UpdateOp_RemoveItemAt(): Unit = runBlocking {
        val initList = listOf("A", "B")
        differ.updateListAwait(UpdateOp.SubmitList(initList))
        differ.updateListAwait(UpdateOp.RemoveItemAt(0))
        assertThat(differ.currentList).isEqualTo(listOf("B"))
        verify(exactly = 0) { diffCallback.areItemsTheSame(any(), any()) }
        verify(exactly = 1) { updateCallback.onRemoved(0, 1) }
    }

    @Test
    fun execute_UpdateOp_SwapItem(): Unit = runBlocking {
        val initList = listOf("A", "B")
        differ.updateListAwait(UpdateOp.SubmitList(initList))
        differ.updateListAwait(UpdateOp.SwapItem(0, 1))
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