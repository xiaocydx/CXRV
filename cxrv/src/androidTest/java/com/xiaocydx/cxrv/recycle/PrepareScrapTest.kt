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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.prepareScrap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
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
    fun collectToList(): Unit = runBlocking(Dispatchers.Main) {
        val rv = RecyclerView(ApplicationProvider.getApplicationContext())
        val adapter = TestAdapter()
        rv.adapter = adapter

        val holderList = rv.prepareScrap(adapter) {
            add(typeA, typeACount)
            add(typeB, typeBCount)
        }.toList()

        val listA = holderList.filter { it.itemViewType == typeA }
        val listB = holderList.filter { it.itemViewType == typeB }
        assertThat(listA.size).isEqualTo(typeACount)
        assertThat(listB.size).isEqualTo(typeBCount)
    }

    @Test
    fun choreographer() {
        // TODO: Choreographer的可测试性
    }

    private class TestAdapter : RecyclerView.Adapter<ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return object : ViewHolder(View(parent.context)) {}
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = error("")
        override fun getItemCount(): Int = error("")
    }
}