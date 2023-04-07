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

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [Payload]的单元测试
 *
 * @author xcc
 * @date 2022/11/13
 */
internal class PayloadTest {

    @Test
    @Suppress("LocalVariableName")
    fun generate_Value_Success() {
        val VALUE1 = Payload.value(1)
        val VALUE2 = Payload.value(2)
        assertThat(VALUE1.countOneBits()).isEqualTo(1)
        assertThat(VALUE2.countOneBits()).isEqualTo(1)
        assertThat(VALUE2 / VALUE1).isEqualTo(2)
    }

    @Test
    fun check_Value_Success() {
        val payload = Payload {
            repeat(2) { (1..5).forEach(::add) }
        }
        val outcome = arrayListOf<Int>()
        Payload.take(listOf(payload), outcome::add)
        assertThat(outcome).hasSize(3)
        assertThat(outcome).contains(1)
        assertThat(outcome).contains(2)
        assertThat(outcome).contains(4)
    }

    @Test
    @Suppress("LocalVariableName")
    fun check_Payload_Equals() {
        val VALUE1 = Payload.value(1)
        val VALUE2 = Payload.value(2)
        val VALUE3 = Payload.value(3)

        val payloadA = Payload { add(VALUE1);add(VALUE2) }
        val payloadB = Payload { add(VALUE1);add(VALUE2) }
        val payloadC = Payload { add(VALUE2);add(VALUE3) }
        assertThat(payloadA).isEqualTo(payloadB)
        assertThat(payloadA).isNotEqualTo(payloadC)
        assertThat(payloadB).isNotEqualTo(payloadC)
    }

    @Test
    @Suppress("LocalVariableName")
    fun check_Payload_HashCode() {
        val VALUE1 = Payload.value(1)
        val VALUE2 = Payload.value(2)
        val VALUE3 = Payload.value(3)

        val payloadA = Payload { add(VALUE1);add(VALUE2) }
        val payloadB = Payload { add(VALUE1);add(VALUE2) }
        val payloadC = Payload { add(VALUE2);add(VALUE3) }
        assertThat(payloadA.hashCode()).isEqualTo(payloadB.hashCode())
        assertThat(payloadA.hashCode()).isNotEqualTo(payloadC.hashCode())
        assertThat(payloadB.hashCode()).isNotEqualTo(payloadC.hashCode())
    }

    @Test
    @Suppress("LocalVariableName")
    fun take_Payloads_ForEach_Success() {
        val VALUE1 = Payload.value(1)
        val VALUE2 = Payload.value(2)
        val payloads = listOf(
            "notTake",
            Payload { add(VALUE1) },
            Payload { add(VALUE2) }
        )

        val outcome = arrayListOf<Int>()
        Payload.take(payloads, outcome::add)
        assertThat(outcome).hasSize(2)
        assertThat(outcome).contains(VALUE1)
        assertThat(outcome).contains(VALUE2)
    }

    @Test
    @Suppress("LocalVariableName")
    fun take_Payloads_Merge_Success() {
        val VALUE1 = Payload.value(1)
        val VALUE2 = Payload.value(2)
        val VALUE3 = Payload.value(3)
        val VALUE4 = Payload.value(4)

        val payloadA = Payload { add(VALUE1);add(VALUE2) }
        val payloadB = Payload { add(VALUE2);add(VALUE3) }
        val payloadC = Payload { add(VALUE3);add(VALUE4) }
        val payloadD = Payload { add(VALUE1);add(VALUE2);add(VALUE3);add(VALUE4) }

        val payloads = listOf(payloadA, payloadB, payloadC, payloadD)
        val outcome = arrayListOf<Int>()
        Payload.take(payloads, outcome::add)
        assertThat(outcome).hasSize(4)
        assertThat(outcome).contains(VALUE1)
        assertThat(outcome).contains(VALUE2)
        assertThat(outcome).contains(VALUE3)
        assertThat(outcome).contains(VALUE4)
    }

    @Test
    fun take_Payloads_Empty() {
        val outcome = arrayListOf<Int>()
        Payload.take(listOf(), outcome::add)
        assertThat(outcome).hasSize(1)
        assertThat(outcome).contains(Payload.EMPTY)
    }

    @Test
    fun takeOrEmpty_Payloads_Empty() {
        val outcome = arrayListOf<Int>()
        Payload.takeOrEmpty(listOf(), outcome::add)
        assertThat(outcome).hasSize(1)
        assertThat(outcome).contains(Payload.EMPTY)
    }

    @Test
    fun takeOrEmpty_Payloads_Empty_Value() {
        val outcome = arrayListOf<Int>()
        val payload = Payload { }
        Payload.takeOrEmpty(listOf(payload), outcome::add)
        assertThat(outcome).hasSize(1)
        assertThat(outcome).contains(Payload.EMPTY)
    }
}