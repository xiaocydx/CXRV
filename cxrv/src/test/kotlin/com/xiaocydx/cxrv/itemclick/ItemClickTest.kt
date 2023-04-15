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

package com.xiaocydx.cxrv.itemclick

import android.os.Build
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.list.emptyDisposable
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate
import com.xiaocydx.cxrv.multitype.toListAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ItemClick的单元测试
 *
 * @author xcc
 * @date 2023/3/5
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class ItemClickTest {
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
    }

    @Test
    fun addDispatchTargetOnAttach() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val dispatcher = recyclerView.itemClickDispatcher
            assertThat(dispatcher.getDispatchTargets()).isEmpty()

            val delegate = TestDelegate()
            val adapter = delegate.toListAdapter()
            delegate.doOnItemClick { _, _ -> }
            delegate.doOnLongItemClick { _, _ -> true }
            adapter.doOnItemClick { _, _ -> }
            adapter.doOnLongItemClick { _, _ -> true }
            assertThat(dispatcher.getDispatchTargets()).isEmpty()

            recyclerView.adapter = adapter
            val targets = dispatcher.getDispatchTargets()
            assertThat(targets).hasSize(4)
            assertThat(targets.filterIsInstance<ClickDispatchTarget>()).hasSize(2)
            assertThat(targets.filterIsInstance<LongClickDispatchTarget>()).hasSize(2)
        }
    }

    @Test
    fun repeatAddDispatchTargetOnAttach() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val dispatcher = recyclerView.itemClickDispatcher

            val delegate = TestDelegate()
            val adapter = delegate.toListAdapter()
            delegate.doOnItemClick { _, _ -> }
            delegate.doOnLongItemClick { _, _ -> true }
            adapter.doOnItemClick { _, _ -> }
            adapter.doOnLongItemClick { _, _ -> true }

            recyclerView.adapter = adapter
            assertThat(dispatcher.getDispatchTargets()).hasSize(4)

            recyclerView.adapter = null
            assertThat(dispatcher.getDispatchTargets()).isEmpty()

            recyclerView.adapter = adapter
            assertThat(dispatcher.getDispatchTargets()).hasSize(4)
        }
    }

    @Test
    fun removeDispatchTargetOnDispose() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val dispatcher = recyclerView.itemClickDispatcher

            val delegate = TestDelegate()
            val adapter = delegate.toListAdapter()
            var disposable = emptyDisposable()
            disposable += delegate.doOnItemClick { _, _ -> }
            disposable += delegate.doOnLongItemClick { _, _ -> true }
            disposable += adapter.doOnItemClick { _, _ -> }
            disposable += adapter.doOnLongItemClick { _, _ -> true }

            recyclerView.adapter = adapter
            assertThat(dispatcher.getDispatchTargets()).hasSize(4)

            disposable.dispose()
            assertThat(dispatcher.getDispatchTargets()).isEmpty()
        }
    }

    private class TestDelegate : ViewTypeDelegate<String, ViewHolder>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = error("")
        override fun onCreateViewHolder(parent: ViewGroup): ViewHolder = error("")
    }
}