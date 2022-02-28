package com.xiaocydx.recycler.extension

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.recycler.TestActivity
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
class SingleSelectionTest {
    private lateinit var recyclerView: RecyclerView
    private val adapter = TestAdapter(
        data = (1..10).map { TestItem(it.toString()) }.toMutableList()
    )
    private val selection = SingleSelection(
        adapter = adapter,
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
        val holder = findViewHolder(0)
        val success = selection.select(holder)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(holder)).isTrue()
        assertThat(selection.selectedItem).isEqualTo(adapter.data[0])
    }

    @Test
    fun unselect_Success() {
        val holder = findViewHolder(0)
        selection.select(holder)
        val success = selection.unselect(holder)
        assertThat(success).isTrue()
        assertThat(selection.isSelected(holder)).isFalse()
        assertThat(selection.selectedItem).isNull()
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
    fun selectCurrent_UnselectPrevious() {
        val holder1 = findViewHolder(1)
        selection.select(holder1)
        val holder2 = findViewHolder(2)
        selection.select(holder2)
        assertThat(selection.isSelected(holder2)).isTrue()
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

    private fun findViewHolder(position: Int): ViewHolder {
        return recyclerView.findViewHolderForAdapterPosition(position)!!
    }
}