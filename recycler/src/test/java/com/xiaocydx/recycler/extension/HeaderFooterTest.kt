package com.xiaocydx.recycler.extension

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.xiaocydx.recycler.TestActivity
import com.xiaocydx.recycler.TestAdapter
import com.xiaocydx.recycler.concat.ViewAdapter
import com.google.common.truth.Truth.assertThat
import io.mockk.spyk
import io.mockk.verifyOrder
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * HeaderFooter的单元测试
 *
 * @author xcc
 * @date 2021/10/15
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class HeaderFooterTest {
    private lateinit var scenario: ActivityScenario<TestActivity>
    private lateinit var headerView: View
    private lateinit var footerView: View
    private lateinit var headerAdapter: ViewAdapter<*>
    private lateinit var footerAdapter: ViewAdapter<*>
    private val testAdapter: TestAdapter = spyk(TestAdapter())
    private val testItems = (0..19).map { it.toString() }

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java)
            .moveToState(State.CREATED)
            .onActivity { activity ->
                headerView = View(activity)
                footerView = View(activity)
                headerAdapter = spyk(headerView.toAdapter())
                footerAdapter = spyk(footerView.toAdapter())
                activity.recyclerView.linear()
            }
    }

    @Test
    fun adapter_RepeatWithHeaderFooter() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val concatAdapter = testAdapter
                .withHeader(headerAdapter).withHeader(headerAdapter)
                .withFooter(footerAdapter).withFooter(footerAdapter)
            recyclerView.adapter = concatAdapter

            concatAdapter.adapters.apply {
                assertThat(size).isEqualTo(3)
                assertThat(get(0)).isEqualTo(headerAdapter)
                assertThat(get(1)).isEqualTo(testAdapter)
                assertThat(get(2)).isEqualTo(footerAdapter)
            }
            verifyOrder {
                headerAdapter.onAttachedToRecyclerView(recyclerView)
                testAdapter.onAttachedToRecyclerView(recyclerView)
                footerAdapter.onAttachedToRecyclerView(recyclerView)
            }
        }
    }

    @Test
    fun adapter_RepeatRemoveHeaderFooter() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            val concatAdapter = testAdapter
                .withHeader(headerAdapter).withFooter(footerAdapter)
            recyclerView.adapter = concatAdapter
            repeat(2) {
                concatAdapter.removeAdapter(headerAdapter)
                concatAdapter.removeAdapter(footerAdapter)
            }

            concatAdapter.adapters.apply {
                assertThat(size).isEqualTo(1)
                assertThat(get(0)).isEqualTo(testAdapter)
            }
            verifyOrder {
                headerAdapter.onDetachedFromRecyclerView(recyclerView)
                footerAdapter.onDetachedFromRecyclerView(recyclerView)
            }
        }
    }

    @Test
    fun recyclerview_RepeatAddHeaderFooter() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.apply {
                adapter = testAdapter
                repeat(2) {
                    addHeader(headerView)
                    addFooter(footerView)
                }
            }

            (recyclerView.adapter as ConcatAdapter).adapters.apply {
                assertThat(size).isEqualTo(3)
                assertThat((get(0) as SimpleViewAdapter).view).isEqualTo(headerView)
                assertThat(get(1)).isEqualTo(testAdapter)
                assertThat((get(2) as SimpleViewAdapter).view).isEqualTo(footerView)
            }
        }
    }

    @Test
    fun recyclerview_RepeatRemoveHeaderFooter() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.apply {
                adapter = testAdapter
                addHeader(headerView)
                addHeader(footerView)
                repeat(2) {
                    removeHeader(headerView)
                    removeFooter(footerView)
                }
            }

            (recyclerView.adapter as ConcatAdapter).adapters.apply {
                assertThat(size).isEqualTo(1)
                assertThat(get(0)).isEqualTo(testAdapter)
            }
        }
    }

    @Test
    fun recyclerView_RemoveHeader_ClearHeaderViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addHeader(headerView)
            testAdapter.items = testItems

            (recyclerView.adapter as ConcatAdapter).adapters[0].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(headerView)
                // 滚动到最后，触发headerView的ViewHolder回收
                recyclerView.scrollToLastItem()
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeHeader(headerView)
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    @Test
    fun recyclerView_RemoveFromParent_ClearHeaderViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addHeader(headerView)
            testAdapter.items = testItems

            (recyclerView.adapter as ConcatAdapter).adapters[0].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(headerView)
                // 滚动到最后，触发headerView的ViewHolder回收
                recyclerView.scrollToLastItem()
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeFromParent()
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    @Test
    fun recyclerView_RemoveFooter_ClearFooterViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addFooter(footerView)

            (recyclerView.adapter as ConcatAdapter).adapters[1].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(footerView)
                // 添加items占满屏幕，触发footerView的ViewHolder回收
                testAdapter.items = testItems
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeFooter(footerView)
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    @Test
    fun recyclerView_RemoveFromParent_ClearFooterViewHolder() {
        scenario.onActivity { activity ->
            val recyclerView = activity.recyclerView
            recyclerView.adapter = testAdapter
            recyclerView.addFooter(footerView)

            (recyclerView.adapter as ConcatAdapter).adapters[1].let { adapter ->
                adapter as SimpleViewAdapter
                assertThat(adapter.view).isEqualTo(footerView)
                // 添加items占满屏幕，触发footerView的ViewHolder回收
                testAdapter.items = testItems
                assertThat(adapter.getRecycledViewHolder()).isNotNull()
                recyclerView.removeFromParent()
                assertThat(adapter.getRecycledViewHolder()).isNull()
            }
        }
    }

    private fun RecyclerView.removeFromParent() {
        assertThat(parent).isNotNull()
        (parent as ViewGroup).removeView(this)
    }

    private fun RecyclerView.scrollToLastItem() {
        assertThat(layoutManager).isNotNull()
        val lastItemPosition = layoutManager!!.itemCount - 1
        scrollToPosition(lastItemPosition)
        assertThat(findLastCompletelyVisibleItemPosition()).isEqualTo(lastItemPosition)
    }
}