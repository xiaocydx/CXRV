@file:Suppress("CAST_NEVER_SUCCEEDS", "UNCHECKED_CAST")

package com.xiaocydx.cxrv.multitype

import android.os.Build
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.list.UpdateOp
import com.xiaocydx.cxrv.list.submitList
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [MultiTypeAdapter]的单元测试
 *
 * @author xcc
 * @date 2021/10/13
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
class MultiTypeAdapterTest {
    private val typeADelegate: TypeADelegate = spyk(TypeADelegate())
    private val typeBDelegate: TypeBDelegate = spyk(TypeBDelegate())

    @Test
    fun viewTypeDelegate_GetAdapter_NonNull() {
        val delegate = spyk(TestDelegate())
        val adapter = multiTypeAdapter<Any> { register(delegate) }
        verify(exactly = 1) { delegate.attachAdapter(adapter) }
        assertThat(delegate._adapter).isNotNull()
        assertThat(delegate._adapter).isEqualTo(adapter)
    }

    @Test
    fun adapter_GetItemViewType_Valid() {
        val adapter = multiTypeAdapter<Any> {
            register(typeADelegate) { it.type == TestType.TYPE_A }
            register(typeBDelegate) { it.type == TestType.TYPE_B }
        }
        val typeAItem = TypeTestItem(TestType.TYPE_A)
        val typeBItem = TypeTestItem(TestType.TYPE_B)
        adapter.submitList(listOf(typeAItem, typeBItem))
        assertThat(adapter.getItemViewType(0)).isEqualTo(typeADelegate.viewType)
        assertThat(adapter.getItemViewType(1)).isEqualTo(typeBDelegate.viewType)
    }

    @Test
    fun viewTypeDelegate_OnCreateViewHolder() {
        val delegate: TestDelegate = mockk(relaxed = true)
        val adapter =
                multiTypeAdapter<Any> { register(delegate) }
        val item = TestItem()
        adapter.submitList(listOf(item))

        val viewType = adapter.getItemViewType(0)
        val parent: ViewGroup = mockk(relaxed = true)
        adapter.onCreateViewHolder(parent, viewType)
        verify(exactly = 1) { delegate.onCreateViewHolder(parent) }
    }

    @Test
    fun viewTypeDelegate_OnBindViewHolder() {
        val delegate: TestDelegate = mockk(relaxed = true)
        val adapter =
                multiTypeAdapter<Any> { register(delegate) }
        val item = TestItem()
        adapter.submitList(listOf(item))

        val holder: ViewHolder = mockk()
        every { holder.itemViewType } returns adapter.getItemViewType(0)
        adapter.onBindViewHolder(holder, 0)
        verify(exactly = 1) { delegate.onBindViewHolder(holder, item) }
    }

    @Test
    fun viewTypeDelegate_DiffItemCallback(): Unit = runBlocking {
        val adapter = multiTypeAdapter<Any>(
            // 单元测试在主线程上执行，若runBlocking()把主线程阻塞了，
            // 则差异计算完成后无法恢复到主线程，因此直接在主线程上进行差异计算。
            workDispatcher = Dispatchers.Main.immediate,
        ) {
            register(typeADelegate) { it.type == TestType.TYPE_A }
            register(typeBDelegate) { it.type == TestType.TYPE_B }
        }

        val typeAItem = TypeTestItem(TestType.TYPE_A)
        val typeBItem = TypeTestItem(TestType.TYPE_B)
        adapter.awaitUpdateList(UpdateOp.SubmitList(listOf(typeBItem)))
        // 首位插入typeAItem
        adapter.awaitUpdateList(UpdateOp.SubmitList(listOf(typeAItem, typeBItem)))

        // 验证oldItem和newItem的ViewType类型不相等的情况不会出现
        verify(exactly = 0) { typeADelegate.areItemsTheSame(typeAItem, typeBItem) }
        verify(exactly = 0) { typeADelegate.areItemsTheSame(typeBItem, typeAItem) }
        verify(exactly = 0) { typeBDelegate.areItemsTheSame(typeAItem, typeBItem) }
        verify(exactly = 0) { typeBDelegate.areItemsTheSame(typeBItem, typeAItem) }

        verify { typeBDelegate.areItemsTheSame(typeBItem, typeBItem) }
        verify { typeBDelegate.areContentsTheSame(typeBItem, typeBItem) }
        verify(exactly = 0) { typeBDelegate.getChangePayload(any(), any()) }
    }

    private inline fun <T : Any> multiTypeAdapter(
        workDispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: MutableMultiType<T>.() -> Unit
    ): MultiTypeAdapter<T> = MultiTypeAdapter(
        multiType = mutableMultiTypeOf<T>().apply(block).complete(),
        workDispatcher = workDispatcher
    )
}