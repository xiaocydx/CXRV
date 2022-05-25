package com.xiaocydx.cxrv.paging

import android.os.Build
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.extension.Disposable
import com.xiaocydx.cxrv.extension.autoDispose
import com.xiaocydx.cxrv.list.ListAdapter
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [PagingCollector]的单元测试
 *
 * @author xcc
 * @date 2021/10/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class PagingCollectorTest {
    private val adapter = TestAdapter()
    private val collector = adapter.pagingCollector
    private var loadStateDisposable: Disposable = spyk(
        collector.doOnLoadStatesChanged { _, _ -> }
    )

    @Test
    fun manual_Dispose() {
        loadStateDisposable.also {
            it.dispose()
            assertDisposed(it)
        }
    }

    @Test
    fun coroutineScope_AutoDispose() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.cancel()
        loadStateDisposable.also {
            it.autoDispose(scope)
            assertDisposed(it)
            verify(exactly = 1) { it.dispose() }
        }
    }

    @Test
    fun lifecycle_AutoDispose() {
        launch(TestActivity::class.java)
            .moveToState(State.CREATED)
            .onActivity { activity ->
                loadStateDisposable.autoDispose(activity)
            }.moveToState(State.DESTROYED)
        assertDisposed(loadStateDisposable)
        verify(exactly = 1) { loadStateDisposable.dispose() }
    }

    @Test
    fun executeOnce_AutoDispose() {
        val handler: LoadStatesListener = mockk(relaxed = true)
        val disposable = collector.doOnLoadStatesChanged(once = true, handler)

        collector.setLoadState(
            loadType = LoadType.REFRESH,
            newState = LoadState.Loading
        )
        assertDisposed(disposable)

        collector.setLoadState(
            loadType = LoadType.REFRESH,
            newState = LoadState.Success(isFully = true)
        )
        verify(exactly = 1) { handler.onLoadStatesChanged(any(), any()) }
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