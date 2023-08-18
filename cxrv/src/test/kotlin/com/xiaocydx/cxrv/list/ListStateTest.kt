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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
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

    @Test
    fun submitListAwait(): Unit = runBlockingTest { listState ->
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        assertThat(listState.currentList).isEqualTo(initList)

        val newList = listOf("A", "B")
        listState.updateList(UpdateOp.SubmitList(newList)).awaitTrue()
        assertThat(listState.currentList).isEqualTo(newList)
    }

    @Test
    fun setItemAwait(): Unit = runBlockingTest { listState ->
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        listState.updateList(UpdateOp.SetItem(0, "B")).awaitTrue()
        assertThat(listState.currentList).isEqualTo(listOf("B"))
    }

    @Test
    fun setItemsAwait(): Unit = runBlockingTest { listState ->
        val initList = listOf("A", "B")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        listState.updateList(UpdateOp.SetItems(0, listOf("C", "D", "E"))).awaitTrue()
        assertThat(listState.currentList).isEqualTo(listOf("C", "D"))
    }

    @Test
    fun addItemAwait(): Unit = runBlockingTest { listState ->
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        listState.updateList(UpdateOp.AddItem(initList.size, "B")).awaitTrue()
        assertThat(listState.currentList).isEqualTo(listOf("A", "B"))
    }

    @Test
    fun addItemsAwait(): Unit = runBlockingTest { listState ->
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        listState.updateList(UpdateOp.AddItems(initList.size, listOf("B", "C"))).awaitTrue()
        assertThat(listState.currentList).isEqualTo(listOf("A", "B", "C"))
    }

    @Test
    fun removeItemsAwait(): Unit = runBlockingTest { listState ->
        val initList = listOf("A", "B", "C")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        listState.updateList(UpdateOp.RemoveItems(position = 0, itemCount = 2)).awaitTrue()
        assertThat(listState.currentList).isEqualTo(listOf("C"))
    }

    @Test
    fun moveItemAwait(): Unit = runBlockingTest { listState ->
        val initList = listOf("A", "B", "C")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        listState.updateList(UpdateOp.MoveItem(0, 2)).awaitTrue()
        assertThat(listState.currentList).isEqualTo(listOf("B", "C", "A"))
    }

    @Test
    fun setItemFailure(): Unit = runBlockingTest { listState ->
        listState.updateList(UpdateOp.SetItem(0, "A")).awaitFalse()
    }

    @Test
    fun setItemsFailure(): Unit = runBlockingTest { listState ->
        listState.updateList(UpdateOp.SetItems(0, listOf("A"))).awaitFalse()
    }

    @Test
    fun addItemFailure(): Unit = runBlockingTest { listState ->
        listState.updateList(UpdateOp.AddItem(1, "A")).awaitFalse()
    }

    @Test
    fun addItemsFailure(): Unit = runBlockingTest { listState ->
        listState.updateList(UpdateOp.AddItems(1, listOf("A"))).awaitFalse()
    }

    @Test
    fun removeItemsFailure(): Unit = runBlockingTest { listState ->
        listState.updateList(UpdateOp.RemoveItems(0, 1)).awaitFalse()
    }

    @Test
    fun moveItemFailure(): Unit = runBlockingTest { listState ->
        listState.updateList(UpdateOp.MoveItem(0, 1)).awaitFalse()
    }

    @Test
    fun emitLatest(): Unit = runBlockingTest { listState ->
        val events = mutableListOf<ListEvent<String>>()
        val flow = listState.asFlow()
        val job = flow.onEach { data ->
            data.flow.toList(events)
        }.launchIn(this)
        delay(200)
        job.cancelAndJoin()
        assertThat(events).hasSize(1)
        assertThat(events.first().op).isInstanceOf(UpdateOp.SubmitList::class.java)
        assertThat(events.first().version).isEqualTo(0)
    }

    @Test
    fun emitUpdateOp(): Unit = runBlockingTest { listState ->
        val ops = mutableListOf<UpdateOp<String>>()
        val flow = listState.asFlow()
        val job = flow.onEach { data ->
            data.flow.map { it.op }.toList(ops)
        }.launchIn(this)
        val op: UpdateOp<String> = UpdateOp.SubmitList(listOf("A"))
        listState.updateList(op).awaitTrue()
        delay(200)
        job.cancelAndJoin()
        assertThat(ops).hasSize(1)
        assertThat(ops.first()).isEqualTo(op)
    }

    @Test
    fun clearUpdateOp(): Unit = runBlockingTest { listState ->
        val flow = listState.asFlow()
        val job1 = flow.onEach { data ->
            data.flow.collect { awaitCancellation() }
        }.launchIn(this)

        repeat(10) { listState.updateList(UpdateOp.SubmitList(listOf("A"))) }
        delay(200)
        job1.cancelAndJoin()

        val ops = mutableListOf<UpdateOp<String>>()
        val job2 = flow.onEach { data ->
            data.flow.map { it.op }.toList(ops)
        }.launchIn(this)
        delay(200)
        job2.cancelAndJoin()
        assertThat(ops).hasSize(1)
    }

    @Test
    fun collectMultiTimesThrowException(): Unit = runBlockingTest { listState ->
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
    fun collectFirstEmitLatestState(): Unit = runBlockingTest { listState ->
        val initList = listOf("A")
        listState.updateList(UpdateOp.SubmitList(initList)).awaitTrue()
        val flow = listState.asFlow()
        var event: ListEvent<String>? = null
        flow.collect { data -> event = data.flow.firstOrNull() }
        assertThat(event).isNotNull()
        assertThat(event!!.version).isEqualTo(listState.version)
        assertThat(event!!.op).isEqualTo(UpdateOp.SubmitList(listState.currentList))
    }

    private suspend fun UpdateResult.awaitTrue() {
        assertThat(isCompleted).isTrue()
        assertThat(await()).isTrue()
        assertThat(get()).isTrue()
    }

    private suspend fun UpdateResult.awaitFalse() {
        assertThat(isCompleted).isTrue()
        assertThat(await()).isFalse()
        assertThat(get()).isFalse()
    }

    private fun <T> runBlockingTest(
        block: suspend CoroutineScope.(ListState<String>) -> T
    ): T = runBlocking { block(ListState()) }
}