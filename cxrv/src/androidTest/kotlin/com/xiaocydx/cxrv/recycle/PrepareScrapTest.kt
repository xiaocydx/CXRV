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

import android.content.Context
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.prepareScrap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/**
 * PrepareScrap的单元测试
 *
 * @author xcc
 * @date 2023/4/13
 */
@RunWith(AndroidJUnit4::class)
internal class PrepareScrapTest {
    private val typeA = 1
    private val typeB = 2
    private val typeACount = 10
    private val typeBCount = 20

    @Test
    fun getPrepareScrapCount(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val adapter = TestAdapter()
        rv.adapter = adapter
        val result = rv.prepareScrap(adapter) {
            add(typeA, typeACount)
            add(typeB, typeBCount)
        }
        assertThat(result.getPreparedScrapCount(typeA)).isEqualTo(typeACount)
        assertThat(result.getPreparedScrapCount(typeB)).isEqualTo(typeBCount)
    }

    @Test
    fun getRecycledScrapCount(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(Context())
        val adapter = TestAdapter()
        rv.adapter = adapter

        val scrapA = adapter.createViewHolder(rv, typeA)
        val scrapB = adapter.createViewHolder(rv, typeB)
        rv.recycledViewPool.putRecycledView(scrapA)
        rv.recycledViewPool.putRecycledView(scrapB)

        val result = rv.prepareScrap(adapter) {
            add(typeA, typeACount)
            add(typeB, typeBCount)
        }
        assertThat(result.getRecycledScrapCount(typeA)).isEqualTo(typeACount + 1)
        assertThat(result.getRecycledScrapCount(typeB)).isEqualTo(typeBCount + 1)
    }

    @Suppress("TestFunctionName")
    private fun Context() = ApplicationProvider.getApplicationContext<Context>()

    private class TestAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            assertThat(Looper.myLooper()).isEqualTo(Looper.getMainLooper())
            return object : ViewHolder(View(parent.context)) {}
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = error("")
        override fun getItemCount(): Int = error("")
    }
}