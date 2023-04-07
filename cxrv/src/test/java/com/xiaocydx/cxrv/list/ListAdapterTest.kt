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
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ListAdapter]的单元测试
 *
 * @author xcc
 * @date 2021/10/11
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class ListAdapterTest {
    private val adapter = TestListAdapter()
    private var changedDisposable: Disposable = spyk(
        adapter.doOnListChanged { }
    )

    @Test
    fun manual_Dispose() {
        changedDisposable.also {
            it.dispose()
            assertDisposed(it)
        }
    }

    @Test
    fun lifecycle_AutoDispose() {
        launch(TestActivity::class.java)
            .moveToState(State.CREATED)
            .onActivity { activity ->
                changedDisposable.autoDispose(activity)
            }.moveToState(State.DESTROYED)
        changedDisposable.also {
            assertDisposed(it)
            verify(exactly = 1) { it.dispose() }
        }
        assertDisposed(changedDisposable)
        verify(exactly = 1) { changedDisposable.dispose() }
    }

    @Test
    fun executeOnce_AutoDispose() {
        val handler: ListChangedListener<Any> = mockk(relaxed = true)
        val disposable = adapter.doOnListChanged(once = true, handler)
        adapter.submitList(listOf("A"))
        assertDisposed(disposable)
        adapter.submitList(listOf("A", "B"))
        verify(exactly = 1) { handler.onListChanged(any()) }
    }

    private fun assertDisposed(disposable: Disposable) {
        assertThat(disposable.isDisposed).isTrue()
    }

    private class TestListAdapter : ListAdapter<Any, ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            error("onCreateViewHolder")
        }

        override fun onBindViewHolder(holder: ViewHolder, item: Any) {
            error("onBindViewHolder")
        }

        override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
            error("areItemsTheSame")
        }
    }
}