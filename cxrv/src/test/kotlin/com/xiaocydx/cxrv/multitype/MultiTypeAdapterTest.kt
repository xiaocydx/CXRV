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

@file:Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")

package com.xiaocydx.cxrv.multitype

import android.os.Build
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.list.UpdateOp
import com.xiaocydx.cxrv.list.submitList
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MultiTypeAdapter]的单元测试
 *
 * @author xcc
 * @date 2021/10/13
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class MultiTypeAdapterTest {
    private val typeADelegate: TypeADelegate = spyk(TypeADelegate())
    private val typeBDelegate: TypeBDelegate = spyk(TypeBDelegate())

    @Test
    fun getAdapterNonNull() {
        val delegate = spyk(TestDelegate())
        val adapter = multiTypeAdapter<Any> { register(delegate) }
        verify(exactly = 1) { delegate.attachAdapter(adapter) }
        assertThat(delegate._adapter).isNotNull()
        assertThat(delegate._adapter).isEqualTo(adapter)
    }

    @Test
    fun getItemViewTypeValid() {
        val adapter = multiTypeAdapter<Any> {
            register(typeADelegate) { it.type == TestType.TYPE_A }
            register(typeBDelegate) { it.type == TestType.TYPE_B }
        }
        val typeAItem = TypeTestItem(TestType.TYPE_A)
        val typeBItem = TypeTestItem(TestType.TYPE_B)
        adapter.submitList(listOf(typeAItem, typeBItem))
        assertThat(adapter.getItemViewType(0)).isEqualTo(typeADelegate.viewType)
        assertThat(adapter.getItemViewType(1)).isEqualTo(typeBDelegate.viewType)
    }

    @Test
    fun onCreateViewHolder() {
        val delegate: TestDelegate = mockk(relaxed = true)
        val adapter =
                multiTypeAdapter<Any> { register(delegate) }
        val item = TestItem()
        adapter.submitList(listOf(item))

        val viewType = adapter.getItemViewType(0)
        val parent: ViewGroup = mockk(relaxed = true)
        adapter.onCreateViewHolder(parent, viewType)
        verify(exactly = 1) { delegate.onCreateViewHolder(parent) }
    }

    @Test
    fun onBindViewHolder() {
        val delegate: TestDelegate = mockk(relaxed = true)
        val adapter =
                multiTypeAdapter<Any> { register(delegate) }
        val item = TestItem()
        adapter.submitList(listOf(item))

        val holder: ViewHolder = mockk()
        every { holder.itemViewType } returns adapter.getItemViewType(0)
        adapter.onBindViewHolder(holder, 0)
        verify(exactly = 1) { delegate.onBindViewHolder(holder, item) }
    }

    @Test
    fun diffItemCallback(): Unit = runBlocking {
        val adapter = multiTypeAdapter<Any>(
            // 单元测试在主线程上执行，若runBlocking()把主线程阻塞了，
            // 则差异计算完成后无法恢复到主线程，因此直接在主线程上进行差异计算。
            diffDispatcher = Dispatchers.Main.immediate,
        ) {
            register(typeADelegate) { it.type == TestType.TYPE_A }
            register(typeBDelegate) { it.type == TestType.TYPE_B }
        }

        val oldItem = TypeTestItem(TestType.TYPE_A)
        val newItem = TypeTestItem(TestType.TYPE_B)
        adapter.awaitUpdateList(UpdateOp.SubmitList(listOf(oldItem)))
        // 首位插入typeAItem
        adapter.awaitUpdateList(UpdateOp.SubmitList(listOf(newItem, oldItem)))

        verify(exactly = 1) { typeADelegate.areItemsTheSame(oldItem, newItem) }
        verify(atLeast = 1) { typeADelegate.areItemsTheSame(oldItem, oldItem) }
        verify(exactly = 1) { typeADelegate.areContentsTheSame(oldItem, oldItem) }
        verify(exactly = 0) { typeADelegate.getChangePayload(any(), any()) }
        verify(exactly = 0) { typeBDelegate.areItemsTheSame(oldItem, newItem) }
    }

    private inline fun <T : Any> multiTypeAdapter(
        diffDispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: MutableMultiType<T>.() -> Unit
    ): MultiTypeAdapter<T> = MultiTypeAdapter(
        multiType = mutableMultiTypeOf<T>().apply(block).complete()
    ).apply { setDiffDispatcher(diffDispatcher) }
}