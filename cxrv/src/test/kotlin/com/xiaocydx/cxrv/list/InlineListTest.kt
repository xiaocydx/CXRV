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

package com.xiaocydx.cxrv.list

import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.internal.reverseAccessEach

import org.junit.Test

/**
 * [InlineList]的单元测试
 *
 * @author xcc
 * @date 2023/6/20
 */
internal class InlineListTest {

    @Test
    fun plus() {
        var list = InlineList<Int>()
        repeat(2) { list += 1 }
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isEqualTo(1)

        repeat(2) { list += 2 }
        assertThat(list.size).isEqualTo(2)
        assertThat(list[0]).isEqualTo(1)
        assertThat(list[1]).isEqualTo(2)
    }

    @Test
    fun minus() {
        var list = InlineList<Int>()
        list += 1
        list -= 1
        assertThat(list.size).isEqualTo(0)

        list += 1
        list += 2
        list -= 1
        assertThat(list.size).isEqualTo(1)
        assertThat(list[0]).isEqualTo(2)
    }

    @Test
    fun clear() {
        var list = InlineList<Int>()
        list += 1
        list += 2
        list = list.clear()
        assertThat(list.size).isEqualTo(0)
    }

    @Test
    fun contains() {
        var list = InlineList<Int>()
        assertThat(list.contains(1)).isFalse()
        list += 1
        assertThat(list.contains(1)).isTrue()
        list += 2
        assertThat(list.contains(2)).isTrue()
    }

    @Test
    fun throwIndexOutOfBoundsException() {
        var list = InlineList<Int>()
        var result = runCatching { list[0] }
        assertThat(result.exceptionOrNull()).isInstanceOf(IndexOutOfBoundsException::class.java)

        list += 1
        result = runCatching { list[1] }
        assertThat(result.exceptionOrNull()).isInstanceOf(IndexOutOfBoundsException::class.java)

        list += 2
        result = runCatching { list[2] }
        assertThat(result.exceptionOrNull()).isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun accessEach() {
        var list = InlineList<Int>()
        list += 1
        list += 2
        var index = 0
        list.accessEach {
            when (index) {
                0 -> assertThat(it).isEqualTo(1)
                1 -> assertThat(it).isEqualTo(2)
            }
            index++
        }
        assertThat(index).isEqualTo(2)
    }

    @Test
    fun reverseAccessEach() {
        var list = InlineList<Int>()
        list += 1
        list += 2
        var index = 1
        list.reverseAccessEach {
            when (index) {
                0 -> assertThat(it).isEqualTo(1)
                1 -> assertThat(it).isEqualTo(2)
            }
            index--
        }
        assertThat(index).isEqualTo(-1)
    }

    @Test
    fun reverseAccessEachAndMinus() {
        var list1 = InlineList<Int>()
        repeat(5) { list1 += (it + 1) }
        val list2 = ArrayList(list1.toList())

        list1.reverseAccessEach {
            if (it % 2 == 0) list1 -= it
        }
        list2.reverseAccessEach {
            if (it % 2 == 0) list2.remove(it)
        }
        assertThat(list1.toList()).isEqualTo(list2)
    }

    @Test
    fun toList() {
        var list1 = InlineList<Int>()
        list1 += 1
        list1 += 2
        val list2 = list1.toList()
        assertThat(list2.size).isEqualTo(2)
        assertThat(list2[0]).isEqualTo(1)
        assertThat(list2[1]).isEqualTo(2)
    }
}