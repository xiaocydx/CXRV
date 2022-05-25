package com.xiaocydx.cxrv.itemvisible

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.TestAdapter
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * ItemVisible的单元测试
 *
 * @author xcc
 * @date 2021/10/18
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class ItemVisibleTest {
    private lateinit var scenario: ActivityScenario<TestActivity>
    private val testAdapter: TestAdapter = TestAdapter()
    private val testItems = (0..99).map { it.toString() }

    @Before
    fun setup() {
        scenario = ActivityScenario
            .launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
    }

    @Test
    fun recyclerView_LinearLayoutManager_ItemVisible() {
        recyclerView_ItemVisible {
            LinearLayoutManager(context)
        }
    }

    @Test
    fun recyclerView_GridLayoutManager_ItemVisible() {
        recyclerView_ItemVisible {
            GridLayoutManager(context, 3)
        }
    }

    @Test
    fun recyclerView_StaggeredGridLayoutManager_ItemVisible() {
        recyclerView_ItemVisible {
            StaggeredGridLayoutManager(3, VERTICAL)
        }
    }

    private inline fun recyclerView_ItemVisible(
        crossinline layout: RecyclerView.() -> LayoutManager
    ) {
        scenario.onActivity { activity ->
            activity.recyclerView.apply {
                layoutManager = layout()
                testItemVisibleProperty()
            }
        }.recreate().onActivity { activity ->
            activity.recyclerView.apply {
                layoutManager = layout()
                testItemVisibleHelper()
            }
        }.recreate().onActivity { activity ->
            activity.recyclerView.apply {
                layoutManager = layout()
                testFirstItemVisibleHandler()
            }
        }.recreate().onActivity { activity ->
            activity.recyclerView.apply {
                layoutManager = layout()
                testLastItemVisibleHandler()
            }
        }
    }

    private fun RecyclerView.testItemVisibleProperty() {
        adapter = testAdapter
        testAdapter.items = testItems
        assertThat(isFirstItemVisible).isTrue()
        assertThat(isFirstItemCompletelyVisible).isTrue()
        assertThat(isLastItemVisible).isFalse()
        assertThat(isLastItemCompletelyVisible).isFalse()

        scrollToPosition(testAdapter.items.lastIndex)
        assertThat(isFirstItemVisible).isFalse()
        assertThat(isFirstItemCompletelyVisible).isFalse()
        assertThat(isLastItemVisible).isTrue()
        assertThat(isLastItemCompletelyVisible).isTrue()
    }

    private fun RecyclerView.testItemVisibleHelper() {
        val helper = ItemVisibleHelper(this)
        adapter = testAdapter
        testAdapter.items = testItems
        assertThat(helper.isFirstItemVisible).isTrue()
        assertThat(helper.isFirstItemCompletelyVisible).isTrue()
        assertThat(helper.isLastItemVisible).isFalse()
        assertThat(helper.isLastItemCompletelyVisible).isFalse()

        scrollToPosition(testAdapter.items.lastIndex)
        assertThat(helper.isFirstItemVisible).isFalse()
        assertThat(helper.isFirstItemCompletelyVisible).isFalse()
        assertThat(helper.isLastItemVisible).isTrue()
        assertThat(helper.isLastItemCompletelyVisible).isTrue()
    }

    private fun RecyclerView.testFirstItemVisibleHandler() {
        val firstItemHandler: () -> Unit = mockk(relaxed = true)
        val firstItemCompletelyHandler: () -> Unit = mockk(relaxed = true)
        val firstItemDisposable =
                doOnFirstItemVisible(once = true, handler = firstItemHandler)
        val firstItemCompletelyDisposable =
                doOnFirstItemCompletelyVisible(once = true, handler = firstItemCompletelyHandler)

        adapter = testAdapter
        testAdapter.items = testItems
        verify(exactly = 1) { firstItemHandler.invoke() }
        verify(exactly = 1) { firstItemCompletelyHandler.invoke() }
        assertThat(firstItemDisposable.isDisposed).isTrue()
        assertThat(firstItemCompletelyDisposable.isDisposed).isTrue()
    }

    private fun RecyclerView.testLastItemVisibleHandler() {
        val lastItemHandler: () -> Unit = mockk(relaxed = true)
        val lastItemCompletelyHandler: () -> Unit = mockk(relaxed = true)
        val lastItemDisposable =
                doOnLastItemVisible(once = true, handler = lastItemHandler)
        val lastItemCompletelyDisposable =
                doOnLastItemCompletelyVisible(once = true, handler = lastItemCompletelyHandler)

        adapter = testAdapter
        testAdapter.items = testItems
        scrollToPosition(testAdapter.items.lastIndex)
        verify(exactly = 1) { lastItemHandler.invoke() }
        verify(exactly = 1) { lastItemCompletelyHandler.invoke() }
        assertThat(lastItemDisposable.isDisposed).isTrue()
        assertThat(lastItemCompletelyDisposable.isDisposed).isTrue()
    }
}