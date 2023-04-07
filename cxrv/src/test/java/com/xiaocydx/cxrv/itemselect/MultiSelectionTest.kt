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
 * [MultiSelection]的单元测试
 *
 * @author xcc
 * @date 2022/1/23
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class MultiSelectionTest {
    private lateinit var recyclerView: RecyclerView
    private val adapter = TestAdapter(
        data = (1..10).map { TestItem(it.toString()) }.toMutableList()
    )
    private val selection = adapter.multiSelection(
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
        val item1 = adapter.data.first()
        val item2 = adapter.data.last()
        var success = selection.select(item1)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(item1)).isTrue()

        success = selection.select(item2)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(item2)).isTrue()

        val selectedItems = selection.selectedItems()
        assertThat(selectedItems).isEqualTo(listOf(item1, item2))
    }

    @Test
    fun unselect_Success() {
        val item1 = adapter.data.first()
        val item2 = adapter.data.last()
        selection.select(item1)
        selection.select(item2)

        var success = selection.unselect(item1)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(item1)).isFalse()

        success = selection.unselect(item2)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(item2)).isFalse()

        assertThat(selection.selectedItems()).isEmpty()
    }

    @Test
    fun selectAll_Success() {
        val success = selection.selectAll(recyclerView)
        assertThat(success).isTrue()
        assertThat(selection.selectedItems()).isEqualTo(adapter.data)
    }

    @Test
    fun clearSelected_Success() {
        selection.selectAll(recyclerView)
        val success = selection.clearSelected(recyclerView)
        assertThat(success).isTrue()
        assertThat(selection.selectedItems()).isEmpty()
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
    fun repeat_SelectAll_Failure() {
        var success = true
        repeat(2) {
            success = selection.selectAll(recyclerView)
        }
        assertThat(success).isFalse()
    }

    @Test
    fun repeat_ClearSelected_Failure() {
        var success = true
        repeat(2) {
            success = selection.clearSelected(recyclerView)
        }
        assertThat(success).isFalse()
    }

    @Test
    fun select_Trigger_OnSelectMax() {
        val onSelectedMax: () -> Unit = mockk(relaxed = true)
        val selection = adapter.multiSelection(
            maxSelectSize = 2,
            itemKey = { it.key },
            itemAccess = { getItem(it) }
        )
        selection.onSelectedMax(onSelectedMax)
        selection.select(adapter.data[0])
        selection.select(adapter.data[1])
        selection.select(adapter.data[2])
        verify(exactly = 1) { onSelectedMax() }
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
    fun selectAll_Trigger_OnSelectAllStateChange() {
        val onStateChange: (Boolean) -> Unit = mockk(relaxed = true)
        selection.onSelectAllStateChange(onStateChange)
        selection.selectAll(recyclerView)
        verify(exactly = 1) { onStateChange(true) }
    }

    @Test
    fun unselect_Trigger_OnSelectAllStateChange() {
        val onStateChange: (Boolean) -> Unit = mockk(relaxed = true)
        selection.onSelectAllStateChange(onStateChange)
        selection.selectAll(recyclerView)
        selection.unselect(adapter.data.first())
        verify(exactly = 1) { onStateChange(false) }
    }

    @Test
    fun clearSelected_Trigger_OnSelectAllStateChange() {
        val onStateChange: (Boolean) -> Unit = mockk(relaxed = true)
        selection.onSelectAllStateChange(onStateChange)
        selection.selectAll(recyclerView)
        selection.clearSelected(recyclerView)
        verify(exactly = 1) { onStateChange(false) }
    }

    @Test
    fun changed_Trigger_OnSelectAllStateChange() {
        val onStateChange: (Boolean) -> Unit = mockk(relaxed = true)
        selection.onSelectAllStateChange(onStateChange)
        selection.selectAll(recyclerView)

        val size = adapter.data.size
        adapter.data.add(TestItem((size + 1).toString()))
        adapter.notifyDataSetChanged()
        verify(exactly = 1) { onStateChange(false) }
    }

    @Test
    fun insertItem_Trigger_OnSelectAllStateChange() {
        val onStateChange: (Boolean) -> Unit = mockk(relaxed = true)
        selection.onSelectAllStateChange(onStateChange)
        selection.selectAll(recyclerView)

        val size = adapter.data.size
        adapter.data.add(TestItem((size + 1).toString()))
        adapter.notifyItemInserted(size)
        verify(exactly = 1) { onStateChange(false) }
    }

    @Test
    fun changed_Trigger_ClearInvalidSelected() {
        val item = adapter.data.first()
        selection.select(item)
        adapter.data.removeAt(0)
        adapter.notifyDataSetChanged()
        assertThat(selection.isSelected(item)).isFalse()
    }

    @Test
    fun removeItem_Trigger_ClearInvalidSelected() {
        val item = adapter.data.first()
        selection.select(item)
        adapter.data.removeAt(0)
        adapter.notifyItemRemoved(0)
        assertThat(selection.isSelected(item)).isFalse()
    }
}