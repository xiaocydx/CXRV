package com.xiaocydx.cxrv.payload

import com.google.common.truth.Truth
import org.junit.Test

/**
 * [DiffPayload]的单元测试
 *
 * @author xcc
 * @date 2022/11/13
 */
class DiffPayloadTest {

    @Test
    @Suppress("LocalVariableName")
    fun add_Value_Success() {
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
        Payload.take(payload, outcome::add)
        Truth.assertThat(outcome).hasSize(2)
        Truth.assertThat(outcome).contains(COUNT1)
        Truth.assertThat(outcome).contains(COUNT2)
    }

    private data class CountItem(val count1: Int, val count2: Int)
}