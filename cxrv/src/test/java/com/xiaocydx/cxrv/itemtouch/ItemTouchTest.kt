package com.xiaocydx.cxrv.itemtouch

import android.os.Build
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.list.ListAdapter
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ItemTouch的单元测试
 *
 * @author xcc
 * @date 2023/3/5
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class ItemTouchTest {
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
    }

    @Test
    fun add_ItemTouchCallback_OnAttach() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val dispatcher = recyclerView.itemTouchDispatcher
            assertThat(dispatcher.getItemTouchCallbacks()).isEmpty()

            val adapter = TestAdapter()
            adapter.itemTouch { }
            assertThat(dispatcher.getItemTouchCallbacks()).isEmpty()

            recyclerView.adapter = adapter
            assertThat(dispatcher.getItemTouchCallbacks()).isNotEmpty()
        }
    }

    @Test
    fun repeat_Add_ItemTouchCallback_OnAttach() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val dispatcher = recyclerView.itemTouchDispatcher

            val adapter = TestAdapter()
            adapter.itemTouch { }

            recyclerView.adapter = adapter
            assertThat(dispatcher.getItemTouchCallbacks()).isNotEmpty()

            recyclerView.adapter = null
            assertThat(dispatcher.getItemTouchCallbacks()).isEmpty()

            recyclerView.adapter = adapter
            assertThat(dispatcher.getItemTouchCallbacks()).isNotEmpty()
        }
    }

    @Test
    fun remove_ItemTouchCallback_OnDispose() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val dispatcher = recyclerView.itemTouchDispatcher

            val adapter = TestAdapter()
            val disposable = adapter.itemTouch { }

            recyclerView.adapter = adapter
            assertThat(dispatcher.getItemTouchCallbacks()).isNotEmpty()

            disposable.dispose()
            assertThat(dispatcher.getItemTouchCallbacks()).isEmpty()
        }
    }

    private class TestAdapter : ListAdapter<String, ViewHolder>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = error("")
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = error("")
    }
}