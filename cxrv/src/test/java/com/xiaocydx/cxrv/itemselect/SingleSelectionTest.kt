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

package com.xiaocydx.cxrv.itemselect

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [SingleSelection]的单元测试
 *
 * @author xcc
 * @date 2022/1/23
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class SingleSelectionTest {
    private lateinit var recyclerView: RecyclerView
    private val adapter = TestAdapter(
        data = (1..10).map { TestItem(it.toString()) }.toMutableList()
    )
    private val selection = adapter.singleSelection(
        itemKey = { it.key },
        itemAccess = { getItem(it) }
    )

    @Before
    fun setup() {
        launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity { activity ->
                recyclerView = activity.recyclerView
                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(activity)
            }
    }

    @Test
    fun select_Success() {
        val item = adapter.data.first()
        val success = selection.select(item)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(item)).isTrue()
        assertThat(selection.selectedItem()).isEqualTo(item)
    }

    @Test
    fun unselect_Success() {
        val item = adapter.data.first()
        selection.select(item)
        val success = selection.unselect(item)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(item)).isFalse()
        assertThat(selection.selectedItem()).isNull()
    }

    @Test
    fun repeat_Select_Failure() {
        var success = true
        val item = adapter.data.first()
        repeat(2) {
            success = selection.select(item)
        }
        assertThat(success).isFalse()
    }

    @Test
    fun repeat_Unselect_Failure() {
        var success = true
        val item = adapter.data.first()
        selection.select(item)
        repeat(2) {
            success = selection.unselect(item)
        }
        assertThat(success).isFalse()
    }

    @Test
    fun selectCurrent_UnselectPrevious() {
        val item1 = adapter.data.first()
        selection.select(item1)
        val item2 = adapter.data.last()
        selection.select(item2)
        assertThat(selection.isSelected(item2)).isTrue()
    }

    @Test
    fun select_Trigger_OnSelect() {
        val onSelect: (TestItem) -> Unit = mockk(relaxed = true)
        selection.onSelect(onSelect)
        val item = adapter.data.first()
        selection.select(item)
        verify(exactly = 1) { onSelect(item) }
    }

    @Test
    fun unselect_Trigger_OnUnselect() {
        val onUnselect: (TestItem) -> Unit = mockk(relaxed = true)
        selection.onUnselect(onUnselect)
        val item = adapter.data.first()
        selection.select(item)
        selection.unselect(item)
        verify(exactly = 1) { onUnselect(item) }
    }

    @Test
    fun changed_Trigger_ClearInvalidSelected() {
        val item = adapter.data.first()
        selection.select(item)
        adapter.data.remove(item)
        adapter.notifyDataSetChanged()
        assertThat(selection.isSelected(item)).isFalse()
    }

    @Test
    fun removeItem_Trigger_ClearInvalidSelected() {
        val item = adapter.data.first()
        selection.select(item)
        adapter.data.remove(item)
        adapter.notifyItemRemoved(0)
        assertThat(selection.isSelected(item)).isFalse()
    }
}