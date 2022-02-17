package com.xiaocydx.recycler.multitype

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
class MutableMultiTypeTest {
    private val testDelegate: TestDelegate = spyk(TestDelegate())
    private val typeADelegate: TypeADelegate = spyk(TypeADelegate())
    private val typeBDelegate: TypeBDelegate = spyk(TypeBDelegate())

    @Test
    fun register_OneToOneType() {
        mutableMultiTypeOf<Any>().init {
            register(testDelegate)
            assertThat(size).isEqualTo(1)
            assertRegistered(testDelegate)
        }
    }

    @Test
    fun register_OneToManyTypes() {
        mutableMultiTypeOf<TypeTestItem>().init {
            register(typeADelegate) { it.type == TestType.TYPE_A }
            register(typeBDelegate) { it.type == TestType.TYPE_B }
            assertThat(size).isEqualTo(2)
            assertRegistered(typeADelegate)
            assertRegistered(typeBDelegate)
        }
    }

    @Test
    fun register_OneToManyTypes_CheckTypeGroups() {
        var exception: IllegalStateException? = null
        try {
            mutableMultiTypeOf<TypeTestItem>().init {
                register(typeADelegate)
                register(typeBDelegate)
            }
        } catch (e: IllegalStateException) {
            exception = e
        }
        assertThat(exception).apply {
            isNotNull()
            hasMessageThat().contains(typeADelegate.javaClass.simpleName)
            hasMessageThat().contains(typeBDelegate.javaClass.simpleName)
        }
    }

    @Test
    fun register_OneToOneType_OneToManyTypes() {
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
}