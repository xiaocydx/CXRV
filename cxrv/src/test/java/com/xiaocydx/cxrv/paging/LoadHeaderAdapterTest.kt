package com.xiaocydx.cxrv.paging

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.extension.withHeader
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.clear
import com.xiaocydx.cxrv.list.insertItem
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [LoadHeaderAdapter]的单元测试
 *
 * @author xcc
 * @date 2021/12/11
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class LoadHeaderAdapterTest {
    private val listAdapter = TestListAdapter()
    private val collector = listAdapter.pagingCollector
    private val onCreateView: OnCreateView<View> = spyk({ View(it.context) })
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario
            .launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity { activity ->
                activity.recyclerView.layoutManager = LinearLayoutManager(activity)
            }
    }

    @Test
    fun createAndShow_LoadingView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            loading<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            collector.setLoadState(LoadType.REFRESH, LoadState.Loading)
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun createAndShow_EmptyView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            empty<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            collector.setLoadState(
                LoadType.REFRESH,
                LoadState.Success(isFully = true)
            )
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun createAndShow_FailureView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNotNull()
        })
        val concatAdapter = getConcatAdapter {
            failure<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            val exception = IllegalArgumentException()
            collector.setLoadState(LoadType.REFRESH, LoadState.Failure(exception))
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun clearList_CreateAndShow_EmptyView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            empty<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            listAdapter.insertItem("A")
            collector.setLoadState(
                LoadType.REFRESH,
                LoadState.Success(isFully = true)
            )
            verify(exactly = 0) { onCreateView(ofType(), ofType()) }
            verify(exactly = 0) { onVisibleChanged(ofType(), ofType(), ofType()) }

            listAdapter.clear()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    private inline fun getConcatAdapter(block: LoadHeaderConfig.() -> Unit): ConcatAdapter {
        val config = LoadHeaderConfig().also(block)
        return listAdapter.withHeader(LoadHeaderAdapter(config, listAdapter))
    }

    private class TestListAdapter : ListAdapter<String, ViewHolder>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            error("areItemsTheSame")
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return object : ViewHolder(View(parent.context)) {}
        }
    }
}