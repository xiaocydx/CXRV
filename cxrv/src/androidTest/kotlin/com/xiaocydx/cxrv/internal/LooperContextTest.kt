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

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.Executors

/**
 * [LooperContext]的单元测试
 *
 * @author xcc
 * @date 2023/4/13
 */
@RunWith(AndroidJUnit4::class)
internal class LooperContextTest {

    @Test
    fun updateAndRestoreLooper(): Unit = runBlocking(Dispatchers.Main) {
        val looper = Looper.myLooper()!!
        val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        withContext(dispatcher) { assertThat(Looper.myLooper()).isNull() }

        withContext(dispatcher + LooperContext(looper)) {
            currentCoroutineContext().job.invokeOnCompletion {
                assertThat(Looper.myLooper()).isEqualTo(looper)
            }
            assertThat(Looper.myLooper()).isEqualTo(looper)
        }

        withContext(dispatcher) { assertThat(Looper.myLooper()).isNull() }
    }
}