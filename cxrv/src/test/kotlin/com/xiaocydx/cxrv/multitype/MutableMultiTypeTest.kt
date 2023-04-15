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

package com.xiaocydx.cxrv.multitype

import android.os.Build
import com.google.common.truth.Truth.assertThat
import io.mockk.spyk
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MutableMultiType]的单元测试
 *
 * @author xcc
 * @date 2021/10/12
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class MutableMultiTypeTest {
    private val testDelegate: TestDelegate = spyk(TestDelegate())
    private val typeADelegate: TypeADelegate = spyk(TypeADelegate())
    private val typeBDelegate: TypeBDelegate = spyk(TypeBDelegate())

    @Test
    fun registerOneToOne() {
        mutableMultiTypeOf<Any>().init {
            register(testDelegate)
            assertThat(size).isEqualTo(1)
            assertRegistered(testDelegate)
        }
    }

    @Test
    fun registerOneToMany() {
        mutableMultiTypeOf<TypeTestItem>().init {
            register(typeADelegate) { it.type == TestType.TYPE_A }
            register(typeBDelegate) { it.type == TestType.TYPE_B }
            assertThat(size).isEqualTo(2)
            assertRegistered(typeADelegate)
            assertRegistered(typeBDelegate)
        }
    }

    @Test
    fun registerOneToManyCheckTypeGroups() {
        var exception: IllegalArgumentException? = null
        try {
            mutableMultiTypeOf<TypeTestItem>().init {
                register(typeADelegate)
                register(typeBDelegate)
            }
        } catch (e: IllegalArgumentException) {
            exception = e
        }
        assertThat(exception).apply {
            isNotNull()
            hasMessageThat().contains(typeADelegate.javaClass.simpleName)
            hasMessageThat().contains(typeBDelegate.javaClass.simpleName)
        }
    }

    @Test
    fun registerOneToOneAndOneToMany() {
        mutableMultiTypeOf<Any>().init {
            register(testDelegate)
            register(typeADelegate) { it.type == TestType.TYPE_A }
            register(typeBDelegate) { it.type == TestType.TYPE_B }
            assertThat(size).isEqualTo(3)

            assertRegistered(testDelegate)
            assertRegistered(typeADelegate)
            assertRegistered(typeBDelegate)
        }
    }

    private fun <T : Any> MultiType<T>.assertRegistered(delegate: ViewTypeDelegate<*, *>) {
        assertThat(keyAt(delegate.viewType)?.delegate).isEqualTo(delegate)
    }

    private inline fun <T : Any> MutableMultiTypeImpl<T>.init(
        block: MutableMultiType<T>.() -> Unit
    ) {
        block()
        complete()
    }
}