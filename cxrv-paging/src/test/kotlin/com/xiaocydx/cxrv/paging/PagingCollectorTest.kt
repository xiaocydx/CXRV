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
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.autoDispose
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [PagingCollector]的单元测试
 *
 * @author xcc
 * @date 2021/10/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class PagingCollectorTest {
    private val adapter = TestAdapter()
    private val collector = adapter.pagingCollector
    private var loadStateDisposable: Disposable = spyk(
        collector.doOnLoadStatesChanged { _, _ -> }
    )

    @Test
    fun manualDispose() {
        loadStateDisposable.also {
            it.dispose()
            assertDisposed(it)
        }
    }

    @Test
    fun lifecycleAutoDispose() {
        launch(TestActivity::class.java)
            .moveToState(State.CREATED)
            .onActivity { activity ->
                loadStateDisposable.autoDispose(activity)
            }.moveToState(State.DESTROYED)
        assertDisposed(loadStateDisposable)
        verify(exactly = 1) { loadStateDisposable.dispose() }
    }

    @Test
    fun executeOnceAutoDispose() {
        val handler: LoadStatesListener = mockk(relaxed = true)
        val disposable = collector.doOnLoadStatesChanged(once = true, handler)
        collector.setLoadState(loadType = LoadType.REFRESH, newState = LoadState.Loading)
        assertDisposed(disposable)
        collector.setLoadState(loadType = LoadType.REFRESH, newState = LoadState.Success(isFully = true))
        verify(exactly = 1) { handler.onLoadStatesChanged(any(), any()) }
    }

    @Test
    fun displayLoadStates() {
        collector.setDisplayTransformer(DisplayTransformer.NonLoading)
        collector.setLoadState(loadType = LoadType.REFRESH, newState = LoadState.Loading)
        assertThat(collector.loadStates).isEqualTo(LoadStates(LoadState.Loading, LoadState.Incomplete))
        assertThat(collector.displayLoadStates).isEqualTo(LoadStates(LoadState.Incomplete, LoadState.Incomplete))
    }

    private fun assertDisposed(disposable: Disposable) {
        assertThat(disposable.isDisposed).isTrue()
    }

    private class TestAdapter : ListAdapter<Any, ViewHolder>() {
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