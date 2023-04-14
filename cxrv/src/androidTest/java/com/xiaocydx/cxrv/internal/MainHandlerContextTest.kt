package com.xiaocydx.cxrv.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * [MainHandlerContext]的单元测试
 *
 * @author xcc
 * @date 2023/4/14
 */
@RunWith(AndroidJUnit4::class)
internal class MainHandlerContextTest {

    @Test
    fun dispatcherEquals() {
        val dispatcher1 = TestMainHandlerContext()
        val dispatcher2 = TestMainHandlerContext()
        assertThat(dispatcher1).isEqualTo(dispatcher2)
        assertThat(System.identityHashCode(dispatcher1))
            .isNotEqualTo(System.identityHashCode(dispatcher2))
    }

    @Test
    fun dispatcherHashCode() {
        val dispatcher1 = TestMainHandlerContext()
        val dispatcher2 = TestMainHandlerContext()
        assertThat(dispatcher1.hashCode())
            .isEqualTo(dispatcher2.hashCode())
        assertThat(System.identityHashCode(dispatcher1))
            .isNotEqualTo(System.identityHashCode(dispatcher2))
    }

    @Test
    @Suppress("SpellCheckingInspection")
    fun withContextUndispatched(): Unit = runBlocking {
        val dispatcher1 = TestMainHandlerContext()
        val dispatcher2 = TestMainHandlerContext()
        withContext(dispatcher1) { withContext(dispatcher2) {} }
        assertThat(dispatcher2.dispatchCount).isEqualTo(0)
    }

    private class TestMainHandlerContext : MainHandlerContext() {
        private val _dispatchCount = AtomicInteger(0)
        val dispatchCount: Int
            get() = _dispatchCount.get()

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            _dispatchCount.incrementAndGet()
            super.dispatch(context, block)
        }
    }
}