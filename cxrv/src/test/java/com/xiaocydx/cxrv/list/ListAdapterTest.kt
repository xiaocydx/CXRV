package com.xiaocydx.cxrv.list

import android.os.Build
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.State
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.extension.Disposable
import com.xiaocydx.cxrv.extension.autoDispose
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
 * [ListAdapter]的单元测试
 *
 * @author xcc
 * @date 2021/10/11
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class ListAdapterTest {
    private val adapter = TestListAdapter()
    private var changedDisposable: Disposable = spyk(
        adapter.doOnListChanged { }
    )

    @Test
    fun manual_Dispose() {
        changedDisposable.also {
            it.dispose()
            assertDisposed(it)
        }
    }

    @Test
    fun coroutineScope_AutoDispose() {
        val scope = CoroutineScope(EmptyCoroutineContext)
        scope.cancel()
        changedDisposable.also {
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
                changedDisposable.autoDispose(activity)
            }.moveToState(State.DESTROYED)
        changedDisposable.also {
            assertDisposed(it)
            verify(exactly = 1) { it.dispose() }
        }
        assertDisposed(changedDisposable)
        verify(exactly = 1) { changedDisposable.dispose() }
    }

    @Test
    fun executeOnce_AutoDispose() {
        val handler: ListChangedListener<Any> = mockk(relaxed = true)
        val disposable = adapter.doOnListChanged(once = true, handler)
        adapter.submitList(listOf("A"))
        assertDisposed(disposable)
        adapter.submitList(listOf("A", "B"))
        verify(exactly = 1) { handler.onListChanged(any()) }
    }

    private fun assertDisposed(disposable: Disposable) {
        assertThat(disposable.isDisposed).isTrue()
    }

    private class TestListAdapter : ListAdapter<Any, ViewHolder>() {
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