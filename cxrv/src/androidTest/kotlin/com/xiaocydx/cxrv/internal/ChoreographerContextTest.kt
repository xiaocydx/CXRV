/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.cxrv.internal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * [ChoreographerContext]的单元测试
 *
 * @author xcc
 * @date 2023/4/14
 */
@RunWith(AndroidJUnit4::class)
internal class ChoreographerContextTest {

    @Test
    fun dispatchToDoFrame(): Unit = runBlocking(Dispatchers.Main) {
        assertThat(stackTraceContainsDoFrame()).isFalse()
        val dispatcher = TestChoreographerContext()
        withContext(dispatcher) {
            assertThat(stackTraceContainsDoFrame()).isTrue()
        }
        assertThat(stackTraceContainsDoFrame()).isFalse()
    }

    @Test
    fun dispatchYield(): Unit = runBlocking(Dispatchers.Main) {
        val launchDispatchCount = 1
        val yieldDispatchCount = 5
        val dispatcher = TestChoreographerContext()
        withContext(dispatcher) {
            repeat(yieldDispatchCount) { yield() }
        }
        val expectedCount = launchDispatchCount + yieldDispatchCount
        assertThat(dispatcher.dispatchCount).isEqualTo(expectedCount)
    }

    @Test
    fun dispatcherEquals(): Unit = runBlocking(Dispatchers.Main) {
        val dispatcher1 = TestChoreographerContext()
        val dispatcher2 = TestChoreographerContext()
        assertThat(dispatcher1).isEqualTo(dispatcher2)
        assertThat(System.identityHashCode(dispatcher1))
            .isNotEqualTo(System.identityHashCode(dispatcher2))
    }

    @Test
    fun dispatcherHashCode(): Unit = runBlocking(Dispatchers.Main) {
        val dispatcher1 = TestChoreographerContext()
        val dispatcher2 = TestChoreographerContext()
        assertThat(dispatcher1.hashCode())
            .isEqualTo(dispatcher2.hashCode())
        assertThat(System.identityHashCode(dispatcher1))
            .isNotEqualTo(System.identityHashCode(dispatcher2))
    }

    @Test
    @Suppress("SpellCheckingInspection")
    fun withContextUndispatched(): Unit = runBlocking(Dispatchers.Main) {
        assertThat(stackTraceContainsDoFrame()).isFalse()
        val dispatcher1 = TestChoreographerContext()
        val dispatcher2 = TestChoreographerContext()
        withContext(dispatcher1) { withContext(dispatcher2) {} }
        assertThat(dispatcher2.dispatchCount).isEqualTo(0)
    }

    /**
     * 若调用栈包含`doFrame`，则表示[ChoreographerContext]调度成功
     */
    private fun stackTraceContainsDoFrame(): Boolean {
        val result = runCatching { throw RuntimeException() }
        val doFrame = result.exceptionOrNull()?.stackTrace
            ?.firstOrNull { it.methodName.contains("doFrame") }
        return doFrame != null
    }

    private class TestChoreographerContext : ChoreographerContext() {
        private val _dispatchCount = AtomicInteger(0)
        val dispatchCount: Int
            get() = _dispatchCount.get()

        override fun isDispatchNeeded(context: CoroutineContext): Boolean = true

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            _dispatchCount.incrementAndGet()
            super.dispatch(context, block)
        }
    }
}