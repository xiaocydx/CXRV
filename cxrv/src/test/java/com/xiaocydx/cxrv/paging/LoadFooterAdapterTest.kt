package com.xiaocydx.cxrv.paging

import android.os.Build
import android.os.Looper.getMainLooper
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
import com.xiaocydx.cxrv.list.insertItem
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * [LoadFooterAdapter]的单元测试
 *
 * @author xcc
 * @date 2021/12/11
 */
@LooperMode(LooperMode.Mode.PAUSED)
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class LoadFooterAdapterTest {
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
            listAdapter.insertItem("A")
            collector.setLoadState(LoadType.APPEND, LoadState.Loading)

            shadowOf(getMainLooper()).idle()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun createAndShow_FullyView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            fully<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            listAdapter.insertItem("A")
            collector.setLoadState(LoadType.APPEND, LoadState.Success(isFully = true))

            shadowOf(getMainLooper()).idle()
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
            listAdapter.insertItem("A")
            val exception = IllegalArgumentException()
            collector.setLoadState(LoadType.APPEND, LoadState.Failure(exception))

            shadowOf(getMainLooper()).idle()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    @Test
    fun insertItem_CreateAndShow_FullyView() {
        val onVisibleChanged: OnVisibleChanged<View> = spyk({ _, _ ->
            assertThat(exception()).isNull()
        })
        val concatAdapter = getConcatAdapter {
            fully<View> {
                onCreateView(onCreateView)
                onVisibleChanged(onVisibleChanged)
            }
        }
        scenario.onActivity { activity ->
            activity.recyclerView.adapter = concatAdapter
            collector.setLoadState(LoadType.APPEND, LoadState.Success(isFully = true))

            shadowOf(getMainLooper()).idle()
            verify(exactly = 0) { onCreateView(ofType(), ofType()) }
            verify(exactly = 0) { onVisibleChanged(ofType(), ofType(), ofType()) }

            listAdapter.insertItem("A")

            shadowOf(getMainLooper()).idle()
            verify(exactly = 1) { onCreateView(ofType(), ofType()) }
            verify(exactly = 1) { onVisibleChanged(ofType(), ofType(), true) }
        }
    }

    private inline fun getConcatAdapter(block: LoadFooterConfig.() -> Unit): ConcatAdapter {
        val config = LoadFooterConfig().also(block)
        return listAdapter.withHeader(LoadFooterAdapter(config, listAdapter))
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