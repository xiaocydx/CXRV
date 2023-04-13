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

package com.xiaocydx.cxrv.payload

import com.google.common.truth.Truth
import org.junit.Test

/**
 * [DiffPayload]的单元测试
 *
 * @author xcc
 * @date 2022/11/13
 */
internal class DiffPayloadTest {

    @Test
    @Suppress("LocalVariableName")
    fun addValue() {
        val oldItem = CountItem(count1 = 1, count2 = 2)
        val newItem = CountItem(count1 = 3, count2 = 4)

        val COUNT1 = Payload.value(1)
        val COUNT2 = Payload.value(2)

        val payload = Payload(oldItem, newItem) {
            repeat(2) {
                ifNotEquals { count1 }.add(COUNT1)
                ifNotEquals { count2 }.add(COUNT2)
            }
        }

        val outcome = arrayListOf<Int>()
        Payload.takeOrEmpty(listOf(payload), outcome::add)
        Truth.assertThat(outcome).hasSize(2)
        Truth.assertThat(outcome).contains(COUNT1)
        Truth.assertThat(outcome).contains(COUNT2)
    }

    private data class CountItem(val count1: Int, val count2: Int)
}