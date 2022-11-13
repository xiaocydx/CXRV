package com.xiaocydx.cxrv.payload

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * [Payload]的单元测试
 *
 * @author xcc
 * @date 2022/11/13
 */
class PayloadTest {

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
    fun add_Value_Success() {
        val payload = Payload {
            repeat(2) { (1..5).forEach(::add) }
        }
        val outcome = arrayListOf<Int>()
        Payload.take(payload, outcome::add)
        assertThat(outcome).hasSize(3)
        assertThat(outcome).contains(1)
        assertThat(outcome).contains(2)
        assertThat(outcome).contains(4)
    }

    @Test
    fun take_Payload_Empty_Value() {
        val payload = "notTake"
        val outcome = arrayListOf<Int>()
        Payload.take(payload, outcome::add)
        assertThat(outcome).hasSize(1)
        assertThat(outcome).contains(Payload.EMPTY)
    }

    @Test
    @Suppress("LocalVariableName")
    fun take_Payloads_Value_Success() {
        val VALUE1 = Payload.value(1)
        val payloads = listOf(
            "notTake",
            Payload { add(VALUE1) },
            Payload { add(VALUE1) }
        )

        val outcome = arrayListOf<Int>()
        Payload.take(payloads, outcome::add)
        assertThat(outcome).hasSize(2)
        assertThat(outcome[0]).isEqualTo(VALUE1)
        assertThat(outcome[1]).isEqualTo(VALUE1)
    }

    @Test
    fun take_Payloads_Empty_Value() {
        val payloads = listOf("notTake")
        val outcome = arrayListOf<Int>()
        Payload.take(payloads, outcome::add)
        assertThat(outcome).hasSize(1)
        assertThat(outcome).contains(Payload.EMPTY)
    }
}