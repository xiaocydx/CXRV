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

package com.xiaocydx.cxrv.recycle.prepare

import android.content.Context
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.prepareScrap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * [PrepareScrap]的单元测试
 *
 * @author xcc
 * @date 2023/4/13
 */
@RunWith(AndroidJUnit4::class)
internal class PrepareScrapTest {
    private val typeA = 1
    private val typeB = 2
    private val typeACount = 10
    private val typeBCount = 20

    @Test
    fun getPrepareScrapCountCompat(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val adapter = TestAdapter()
        rv.adapter = adapter
        val result = rv.prepareScrap(adapter) {
            add(typeA, typeACount)
            add(typeB, typeBCount)
        }
        assertThat(result.getPreparedScrapCount(typeA)).isEqualTo(typeACount)
        assertThat(result.getPreparedScrapCount(typeB)).isEqualTo(typeBCount)
    }

    @Test
    fun getRecycledScrapCountCompat(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val adapter = TestAdapter()
        rv.adapter = adapter

        val scrapA = adapter.createViewHolder(rv, typeA)
        val scrapB = adapter.createViewHolder(rv, typeB)
        rv.recycledViewPool.putRecycledView(scrapA)
        rv.recycledViewPool.putRecycledView(scrapB)

        val result = rv.prepareScrap(adapter) {
            add(typeA, typeACount)
            add(typeB, typeBCount)
        }
        assertThat(result.getRecycledScrapCount(typeA)).isEqualTo(typeACount + 1)
        assertThat(result.getRecycledScrapCount(typeB)).isEqualTo(typeBCount + 1)
    }

    @Test
    fun prepareView(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val scrapMap = rv.prepareView()
            .view(typeA, typeACount) { TestView(it.context) }
            .view(typeB, typeBCount) { TestView(it.context) }
            .toList().groupBy { it.viewType }
        assertThat(scrapMap[typeA]).hasSize(typeACount)
        assertThat(scrapMap[typeB]).hasSize(typeBCount)
    }

    @Test
    fun prepareHolder(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val scrapMap = rv.prepareHolder()
            .holder(typeA, typeACount) { TestHolder(it.context) }
            .holder(typeB, typeBCount) { TestHolder(it.context) }
            .toList().groupBy { it.viewType }
        assertThat(scrapMap[typeA]).hasSize(typeACount)
        assertThat(scrapMap[typeB]).hasSize(typeBCount)
    }

    @Test
    fun putToRecycledViewPool(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val pool = rv.recycledViewPool
        rv.prepareHolder()
            .holder(typeA, typeACount) { TestHolder(it.context) }
            .holder(typeB, typeBCount) { TestHolder(it.context) }
            .putToRecycledViewPool()
            .toList().groupBy { it.viewType }
        assertThat(pool.getRecycledViewCount(typeA)).isEqualTo(typeACount)
        assertThat(pool.getRecycledViewCount(typeB)).isEqualTo(typeBCount)
    }

    @Test
    fun contextPreservation(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val collectContext = currentCoroutineContext().minusKey(Job)
        rv.prepareHolder()
            .holder(typeA, typeACount) { TestHolder(it.context) }
            .map { currentCoroutineContext().minusKey(Job) }
            .collect { assertThat(it).isEqualTo(collectContext) }
    }

    @Test
    fun dispatcher(): Unit = runBlocking(Dispatchers.Main) {
        val threadName = "TestDispatcher"
        val executor = Executors.newSingleThreadExecutor { Thread(it, threadName) }
        val dispatcher = executor.asCoroutineDispatcher()
        val rv = RecyclerView(Context())
        val scrapList = rv.prepareHolder()
            .dispatcher(dispatcher)
            .holder(typeA, typeACount) {
                assertThat(Thread.currentThread().name).isEqualTo(threadName)
                TestHolder(it.context)
            }.toList()
        assertThat(scrapList).hasSize(typeACount)
        dispatcher.close()
    }

    @Test
    fun inflater(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val instance = LayoutInflater.from(rv.context)
        val scrapList = rv.prepareHolder()
            .inflater(PrepareScrapTest::TestLayoutInflater)
            .holder(typeA, typeACount) {
                assertThat(it.inflater).isNotEqualTo(instance)
                assertThat(it.inflater).isInstanceOf(TestLayoutInflater::class.java)
                TestHolder(it.context)
            }.toList()
        assertThat(scrapList).hasSize(typeACount)
    }

    @Test
    fun deadline(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val scrapList = rv.prepareHolder()
            .deadline {
                delay(10)
                System.nanoTime()
            }
            .holder(typeA, typeACount * 100) { TestHolder(it.context) }
            .toList()
        assertThat(scrapList.size).isNotEqualTo(0)
        assertThat(scrapList.size).isNotEqualTo(typeACount * 100)
    }

    @Test
    fun scrapInflater(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val rvContext = rv.context
        val rvInflater = LayoutInflater.from(rv.context)
        rv.prepareView()
            .view(typeA, typeACount) {
                val viewContext = it.context
                assertThat(viewContext).isNotEqualTo(rvContext)
                // LayoutInflater.from(viewContext)为预创建流程构建的Inflater
                assertThat(LayoutInflater.from(viewContext)).isNotEqualTo(rvInflater)
                View(viewContext)
            }
            .onEach {
                val viewContext = it.value.context
                assertThat(viewContext).isNotEqualTo(rvContext)
                // LayoutInflater.from(viewContext)恢复为获取rvInflater
                assertThat(LayoutInflater.from(viewContext)).isEqualTo(rvInflater)
            }
            .collect()
    }

    @Test
    fun reuseAdapter(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val adapter = TestAdapter()
        val scrapMap = rv.prepareHolder()
            .reuse(typeA, typeACount, adapter)
            .reuse(typeB, typeBCount, adapter)
            .toList().groupBy { it.viewType }
        assertThat(scrapMap[typeA]).hasSize(typeACount)
        assertThat(scrapMap[typeB]).hasSize(typeBCount)
    }

    @Test
    fun reuseDelegate(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val delegateA = TestTypeADelegate()
        val delegateB = TestTypeBDelegate()
        val scrapMap = rv.prepareHolder()
            .reuse(typeACount, delegateA)
            .reuse(typeBCount, delegateB)
            .toList().groupBy { it.viewType }
        assertThat(scrapMap[delegateA.viewType]).hasSize(typeACount)
        assertThat(scrapMap[delegateB.viewType]).hasSize(typeBCount)
    }

    @Test
    fun throwException(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        var exception: Throwable? = null
        rv.prepareHolder()
            .inflater { throw RuntimeException() }
            .holder(typeA, typeACount) { TestHolder(it.context) }
            .catch { exception = it }
            .collect()
        assertThat(exception).isInstanceOf(RuntimeException::class.java)

        exception = null
        rv.prepareHolder()
            .deadline { throw RuntimeException() }
            .holder(typeA, typeACount) { TestHolder(it.context) }
            .catch { exception = it }
            .collect()
        assertThat(exception).isInstanceOf(RuntimeException::class.java)

        exception = null
        rv.prepareHolder()
            .holder(typeA, typeACount) { throw RuntimeException() }
            .catch { exception = it }
            .collect()
        assertThat(exception).isInstanceOf(RuntimeException::class.java)
    }

    @Suppress("TestFunctionName")
    private fun Context() = ApplicationProvider.getApplicationContext<Context>()

    private class TestAdapter : RecyclerView.Adapter<TestHolder>() {
        override fun onBindViewHolder(holder: TestHolder, position: Int) = error("")
        override fun getItemCount(): Int = error("")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TestHolder(parent.context)
    }

    private class TestTypeADelegate : ViewTypeDelegate<Any, TestHolder>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any) = error("")
        override fun onCreateViewHolder(parent: ViewGroup) = TestHolder(parent.context)
    }

    private class TestTypeBDelegate : ViewTypeDelegate<Any, TestHolder>() {
        override fun areItemsTheSame(oldItem: Any, newItem: Any) = error("")
        override fun onCreateViewHolder(parent: ViewGroup) = TestHolder(parent.context)
    }

    private class TestHolder(itemView: View) : ViewHolder(itemView) {
        constructor(context: Context) : this(TestView(context))
    }

    private class TestView(context: Context) : View(context) {
        init {
            assertThat(Looper.myLooper()).isEqualTo(Looper.getMainLooper())
        }
    }

    private class TestLayoutInflater(context: Context) : ScrapLayoutInflater(context)
}