package com.xiaocydx.cxrv.list

import android.os.Build
import com.google.common.truth.Truth.assertThat
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ListState]的单元测试
 *
 * @author xcc
 * @date 2022/7/17
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class ListStateTest {
    private val listState = ListState<String>()

    @Test
    fun execute_UpdateOp_SubmitList() {
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList))
        assertThat(listState.currentList).isEqualTo(initList)

        val newList = listOf("A", "B")
        listState.updateList(UpdateOp.SubmitList(newList))
        assertThat(listState.currentList).isEqualTo(newList)
    }

    @Test
    fun execute_UpdateOp_SetItem() {
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList))
        listState.updateList(UpdateOp.SetItem(0, "B"))
        assertThat(listState.currentList).isEqualTo(listOf("B"))
    }

    @Test
    fun execute_UpdateOp_SetItems() {
        val initList = listOf("A", "B")
        listState.updateList(UpdateOp.SubmitList(initList))
        listState.updateList(UpdateOp.SetItems(0, listOf("C", "D", "E")))
        assertThat(listState.currentList).isEqualTo(listOf("C", "D"))
    }

    @Test
    fun execute_UpdateOp_AddItem() {
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList))
        listState.updateList(UpdateOp.AddItem(initList.size, "B"))
        assertThat(listState.currentList).isEqualTo(listOf("A", "B"))
    }

    @Test
    fun execute_UpdateOp_AddItems() {
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList))
        listState.updateList(UpdateOp.AddItems(initList.size, listOf("B", "C")))
        assertThat(listState.currentList).isEqualTo(listOf("A", "B", "C"))
    }

    @Test
    fun execute_UpdateOp_RemoveItems() {
        val initList = listOf("A", "B", "C")
        listState.updateList(UpdateOp.SubmitList(initList))
        listState.updateList(UpdateOp.RemoveItems(position = 0, itemCount = 2))
        assertThat(listState.currentList).isEqualTo(listOf("C"))
    }

    @Test
    fun execute_UpdateOp_MoveItem() {
        val initList = listOf("A", "B", "C")
        listState.updateList(UpdateOp.SubmitList(initList))
        listState.updateList(UpdateOp.MoveItem(0, 2))
        assertThat(listState.currentList).isEqualTo(listOf("B", "C", "A"))
    }

    @Test
    fun execute_UpdateOp_DispatchListener() {
        val listener: (UpdateOp<String>) -> Unit = spyk()
        listState.addUpdatedListener(listener)
        val op: UpdateOp<String> = UpdateOp.SubmitList(listOf("A"))
        listState.updateList(op)
        verify(exactly = 1) { listener.invoke(op) }
    }

    @Test
    fun collect_MultiTimes_ThrowException(): Unit = runBlocking {
        val flow = listState.asFlow()
        val parentJob = SupervisorJob(coroutineContext.job)
        val deferred = async(parentJob) {
            flow.collect { data ->
                launch { data.flow.collect() }
                launch { data.flow.collect() }
            }
        }
        val result = kotlin.runCatching { deferred.await() }
        assertThat(result.exceptionOrNull()).isNotNull()
        parentJob.complete()
    }

    @Test
    fun collect_FirstEmit_LatestState(): Unit = runBlocking {
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList))
        val flow = listState.asFlow()
        var event: ListEvent<String>? = null
        flow.collect { data -> event = data.flow.firstOrNull() }
        assertThat(event).isNotNull()
        assertThat(event!!.version).isEqualTo(listState.version)
        assertThat(event!!.op).isEqualTo(UpdateOp.SubmitList(listState.currentList))
    }
}