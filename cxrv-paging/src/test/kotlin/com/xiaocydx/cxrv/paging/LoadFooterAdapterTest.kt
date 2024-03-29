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

package com.xiaocydx.cxrv.paging

import android.os.Build
import android.os.Looper.getMainLooper
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.concat.withHeader
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.insertItem
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * [LoadFooterAdapter]的单元测试
 *
 * @author xcc
 * @date 2021/12/11
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class LoadFooterAdapterTest {
    private val listAdapter = TestListAdapter()
    private val collector = listAdapter.pagingCollector
    private val onCreateView: OnCreateView<View> = spyk({ View(it.context) })
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario
            .launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity { activity ->
                activity.recyclerView.layoutManager = LinearLayoutManager(activity)
            }
    }

    @Test
    fun createAndShowLoadingView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            loading<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            listAdapter.insertItem("A")
            collector.setLoadState(LoadType.APPEND, LoadState.Loading)
            shadowOf(getMainLooper()).idle()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun createAndShowFullyView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            fully<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            listAdapter.insertItem("A")
            collector.setLoadState(LoadType.APPEND, LoadState.Success(isFully = true))
            shadowOf(getMainLooper()).idle()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun createAndShowFailureView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNotNull()
        })
        val concatAdapter = getConcatAdapter {
            failure<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            listAdapter.insertItem("A")
            val exception = IllegalArgumentException()
            collector.setLoadState(LoadType.APPEND, LoadState.Failure(exception))
            shadowOf(getMainLooper()).idle()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun insertItemCreateAndShowFullyView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            fully<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            collector.setLoadState(LoadType.APPEND, LoadState.Success(isFully = true))
            shadowOf(getMainLooper()).idle()
            verify(exactly = 0) { onCreateView(ofType(), ofType()) }
            verify(exactly = 0) { onVisibleChanged(ofType(), ofType(), ofType()) }

            listAdapter.insertItem("A")
            shadowOf(getMainLooper()).idle()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    private inline fun getConcatAdapter(block: LoadFooterConfig.() -> Unit): ConcatAdapter {
        val config = LoadFooterConfig().also(block)
        return listAdapter.withHeader(LoadFooterAdapter(config, listAdapter))
    }

    private class TestListAdapter : ListAdapter<String, ViewHolder>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            error("areItemsTheSame")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return object : ViewHolder(View(parent.context)) {}
        }
    }
}