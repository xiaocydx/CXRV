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

package com.xiaocydx.cxrv.concat

import android.os.Build
import android.os.Looper.getMainLooper
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.TestAdapter
import com.xiaocydx.cxrv.itemvisible.findLastCompletelyVisibleItemPosition
import io.mockk.spyk
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * HeaderFooter的单元测试
 *
 * @author xcc
 * @date 2021/10/15
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class HeaderFooterTest {
    private lateinit var scenario: ActivityScenario<TestActivity>
    private lateinit var headerView: View
    private lateinit var footerView: View
    private lateinit var headerAdapter: ViewAdapter<*>
    private lateinit var footerAdapter: ViewAdapter<*>
    private val testAdapter: TestAdapter = spyk(TestAdapter())
    private val testItems = (0..19).map { it.toString() }

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java)
            .moveToState(State.CREATED)
            .onActivity { activity ->
                headerView = View(activity)
                footerView = View(activity)
                headerAdapter = spyk(headerView.toAdapter())
                footerAdapter = spyk(footerView.toAdapter())
                activity.recyclerView.layoutManager = LinearLayoutManager(activity)
            }
    }

    @Test
    fun repeatWithHeaderFooterByAdapter() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val concatAdapter = testAdapter
                .withHeader(headerAdapter).withHeader(headerAdapter)
                .withFooter(footerAdapter).withFooter(footerAdapter)
            recyclerView.adapter = concatAdapter

            concatAdapter.adapters.apply {
                assertThat(size).isEqualTo(3)
                assertThat(get(0)).isEqualTo(headerAdapter)
                assertThat(get(1)).isEqualTo(testAdapter)
                assertThat(get(2)).isEqualTo(footerAdapter)
            }
            verifyOrder {
                headerAdapter.onAttachedToRecyclerView(recyclerView)
                testAdapter.onAttachedToRecyclerView(recyclerView)
                footerAdapter.onAttachedToRecyclerView(recyclerView)
            }
        }
    }

    @Test
    fun repeatRemoveHeaderFooterByAdapter() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val concatAdapter = testAdapter
                .withHeader(headerAdapter).withFooter(footerAdapter)
            recyclerView.adapter = concatAdapter
            repeat(2) {
                concatAdapter.removeAdapter(headerAdapter)
                concatAdapter.removeAdapter(footerAdapter)
            }

            concatAdapter.adapters.apply {
                assertThat(size).isEqualTo(1)
                assertThat(get(0)).isEqualTo(testAdapter)
            }
            verifyOrder {
                headerAdapter.onDetachedFromRecyclerView(recyclerView)
                footerAdapter.onDetachedFromRecyclerView(recyclerView)
            }
        }
    }

    @Test
    fun repeatAddHeaderFooterByRecyclerview() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.apply {
                adapter = testAdapter
                repeat(2) {
                    addHeader(headerView)
                    addFooter(footerView)
                }
            }

            (recyclerView.adapter as ConcatAdapter).adapters.apply {
                assertThat(size).isEqualTo(3)
                assertThat((get(0) as SimpleViewAdapter).view).isEqualTo(headerView)
                assertThat(get(1)).isEqualTo(testAdapter)
                assertThat((get(2) as SimpleViewAdapter).view).isEqualTo(footerView)
            }
        }
    }

    @Test
    fun repeatRemoveHeaderFooterByRecyclerview() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.apply {
                adapter = testAdapter
                addHeader(headerView)
                addHeader(footerView)
                repeat(2) {
                    removeHeader(headerView)
                    removeFooter(footerView)
                }
            }

            (recyclerView.adapter as ConcatAdapter).adapters.apply {
                assertThat(size).isEqualTo(1)
                assertThat(get(0)).isEqualTo(testAdapter)
            }
        }
    }

    @Test
    fun removeHeaderClearHeaderViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addHeader(headerView)
            shadowOf(getMainLooper()).idle()

            testAdapter.items = testItems
            shadowOf(getMainLooper()).idle()

            (recyclerView.adapter as ConcatAdapter).adapters[0].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(headerView)
                // 滚动到最后，触发headerView的ViewHolder回收
                recyclerView.scrollToLastItem()
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeHeader(headerView)
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    @Test
    fun removeFromParentClearHeaderViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addHeader(headerView)
            shadowOf(getMainLooper()).idle()

            testAdapter.items = testItems
            shadowOf(getMainLooper()).idle()

            (recyclerView.adapter as ConcatAdapter).adapters[0].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(headerView)
                // 滚动到最后，触发headerView的ViewHolder回收
                recyclerView.scrollToLastItem()
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeFromParent()
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    @Test
    fun removeFooterClearFooterViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addFooter(footerView)
            shadowOf(getMainLooper()).idle()

            (recyclerView.adapter as ConcatAdapter).adapters[1].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(footerView)
                // 添加items占满屏幕，触发footerView的ViewHolder回收
                testAdapter.items = testItems
                shadowOf(getMainLooper()).idle()
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeFooter(footerView)
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    @Test
    fun removeFromParentClearFooterViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addFooter(footerView)
            shadowOf(getMainLooper()).idle()

            (recyclerView.adapter as ConcatAdapter).adapters[1].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(footerView)
                // 添加items占满屏幕，触发footerView的ViewHolder回收
                testAdapter.items = testItems
                shadowOf(getMainLooper()).idle()
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeFromParent()
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    private fun RecyclerView.removeFromParent() {
        assertThat(parent).isNotNull()
        (parent as ViewGroup).removeView(this)
        shadowOf(getMainLooper()).idle()
    }

    private fun RecyclerView.scrollToLastItem() {
        assertThat(layoutManager).isNotNull()
        val lastItemPosition = layoutManager!!.itemCount - 1
        scrollToPosition(lastItemPosition)
        shadowOf(getMainLooper()).idle()
        assertThat(findLastCompletelyVisibleItemPosition()).isEqualTo(lastItemPosition)
    }
}