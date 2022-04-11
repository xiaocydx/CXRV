package com.xiaocydx.recycler.selection

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.recycler.TestActivity
import com.xiaocydx.recycler.extension.multiSelection
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
class MultiSelectionTest {
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
        val holder1 = findViewHolder(1)
        val holder2 = findViewHolder(2)
        var success = selection.select(holder1)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(holder1)).isTrue()

        success = selection.select(holder2)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(holder2)).isTrue()

        val selectedList = selection.selectedList
        assertThat(selectedList.contains(adapter.data[1])).isTrue()
        assertThat(selectedList.contains(adapter.data[2])).isTrue()
    }

    @Test
    fun unselect_Success() {
        val holder1 = findViewHolder(1)
        val holder2 = findViewHolder(2)
        selection.select(holder1)
        selection.select(holder2)

        var success = selection.unselect(holder1)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(holder1)).isFalse()

        success = selection.unselect(holder2)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(holder2)).isFalse()

        assertThat(selection.selectedList).isEmpty()
    }

    @Test
    fun selectAll_Success() {
        val success = selection.selectAll(recyclerView)
        assertThat(success).isTrue()
        assertThat(selection.selectedList).isEqualTo(adapter.data)
    }

    @Test
    fun clearSelected_Success() {
        selection.selectAll(recyclerView)
        val success = selection.clearSelected(recyclerView)
        assertThat(success).isTrue()
        assertThat(selection.selectedList).isEmpty()
    }

    @Test
    fun repeat_Select_Failure() {
        var success = true
        val holder = findViewHolder(0)
        repeat(2) {
            success = selection.select(holder)
        }
        assertThat(success).isFalse()
    }

    @Test
    fun repeat_Unselect_Failure() {
        var success = true
        val holder = findViewHolder(0)
        selection.select(holder)
        repeat(2) {
            success = selection.unselect(holder)
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
        selection.select(findViewHolder(0))
        selection.select(findViewHolder(1))
        selection.select(findViewHolder(2))
        verify(exactly = 1) { onSelectedMax() }
    }

    @Test
    fun select_Trigger_OnSelect() {
        val onSelect: (TestItem) -> Unit = mockk(relaxed = true)
        selection.onSelect(onSelect)
        val holder = findViewHolder(0)
        selection.select(holder)
        verify(exactly = 1) { onSelect(adapter.data[0]) }
    }

    @Test
    fun unselect_Trigger_OnUnselect() {
        val onUnselect: (TestItem) -> Unit = mockk(relaxed = true)
        selection.onUnselect(onUnselect)
        val holder = findViewHolder(0)
        selection.select(holder)
        selection.unselect(holder)
        verify(exactly = 1) { onUnselect(adapter.data[0]) }
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
        selection.unselect(findViewHolder(0))
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
        val holder = findViewHolder(0)
        selection.select(holder)
        adapter.data.removeAt(0)
        adapter.notifyDataSetChanged()
        assertThat(selection.isSelected(holder)).isFalse()
    }

    @Test
    fun removeItem_Trigger_ClearInvalidSelected() {
        val holder = findViewHolder(0)
        selection.select(holder)
        adapter.data.removeAt(0)
        adapter.notifyItemRemoved(0)
        assertThat(selection.isSelected(holder)).isFalse()
    }

    private fun findViewHolder(position: Int): RecyclerView.ViewHolder {
        return recyclerView.findViewHolderForAdapterPosition(position)!!
    }
}