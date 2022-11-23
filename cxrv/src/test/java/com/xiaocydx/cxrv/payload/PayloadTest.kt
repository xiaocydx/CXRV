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
    fun validate_Value_Success() {
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
    fun take_Payloads_Recycle_Success() {
        // 清空对象池
        var recycled: Payload?
        do {
            recycled = Payload.pool.acquire()
        } while (recycled != null)

        val payload = Payload {
            add(Payload.value(1))
        }
        Payload.take(listOf(payload)) {}

        recycled = Payload.pool.acquire()
        assertThat(recycled).isNotNull()
        assertThat(recycled!!.isEmpty()).isTrue()
        assertThat(recycled).isSameInstanceAs(payload)
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