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

package com.xiaocydx.cxrv.recycle

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.prepareScrap
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.TestActivity
import com.xiaocydx.cxrv.TestAdapter
import com.xiaocydx.cxrv.list.TestMainDispatcher
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * PrepareScrap的单元测试
 *
 * @author xcc
 * @date 2023/4/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class PrepareScrapTest {
    private val typeA = 1
    private val typeB = 2
    private val typeACount = 10
    private val typeBCount = 20
    private lateinit var scenario: ActivityScenario<TestActivity>

    @Before
    fun setup() {
        scenario = launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
    }

    @Test
    fun prepareScrap_CollectToList(): Unit = runBlocking {
        var recyclerView: RecyclerView? = null
        scenario.onActivity { recyclerView = it.recyclerView }
        val rv = recyclerView!!
        val adapter = TestAdapter()
        rv.adapter = adapter

        val holderList = rv.prepareScrap(
            prepareAdapter = adapter,
            mainDispatcher = TestMainDispatcher(coroutineContext)
        ) {
            add(typeA, typeACount)
            add(typeB, typeBCount)
        }.toList()

        val listA = holderList.filter { it.itemViewType == typeA }
        val listB = holderList.filter { it.itemViewType == typeB }
        assertThat(listA.size).isEqualTo(typeACount)
        assertThat(listB.size).isEqualTo(typeBCount)
    }

    @Test
    fun prepareScrap_Choreographer() {
        // TODO: Choreographer的可测试性
    }
}