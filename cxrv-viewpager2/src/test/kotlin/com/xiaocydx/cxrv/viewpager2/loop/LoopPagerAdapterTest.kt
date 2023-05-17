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

package com.xiaocydx.cxrv.viewpager2.loop

import android.os.Build
import androidx.lifecycle.Lifecycle.State.CREATED
import androidx.recyclerview.widget.LoopPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.viewpager2.TestActivity
import com.xiaocydx.cxrv.viewpager2.TestAdapter
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.PADDING_EXTRA_PAGE_LIMIT
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [LoopPagerAdapter]的单元测试
 *
 * @author xcc
 * @date 2023/5/14
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class LoopPagerAdapterTest {
    private lateinit var content: LoopPagerContent
    private lateinit var contentAdapter: TestAdapter
    private lateinit var loopPagerAdapter: LoopPagerAdapter
    private val loopPagerObserver = spyk(object : AdapterDataObserver() {})

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     *
     * currentCount = 3
     * supportLoop = true
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * layoutPositionRange = [0, 6]
     * headerLayoutPositionRange = [0, 1]
     * footerLayoutPositionRange = [5, 6]
     *
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * bindingAdapterPositionRange = [0, 2]
     * headerBindingAdapterPositionRange = [1, 2]
     * footerBindingAdapterPositionRange = [0, 1]
     * ```
     */
    @Before
    fun setup() {
        launch(TestActivity::class.java)
            .moveToState(CREATED)
            .onActivity {
                contentAdapter = TestAdapter(count = 3)
                content = LoopPagerContent(
                    viewPager2 = it.viewPager2,
                    adapter = contentAdapter,
                    extraPageLimit = PADDING_EXTRA_PAGE_LIMIT
                )
                loopPagerAdapter = LoopPagerAdapter(content, spyk())
                loopPagerAdapter.registerAdapterDataObserver(loopPagerObserver)
                it.viewPager2.adapter = loopPagerAdapter
            }
    }

    @Test
    fun bindingAdapter() {
        val rv = content.viewPager2.getChildAt(0) as RecyclerView
        val holder = loopPagerAdapter.onCreateViewHolder(rv, 0)
        loopPagerAdapter.bindViewHolder(holder, 0)
        assertThat(holder.bindingAdapter).isEqualTo(contentAdapter)
    }

    @Test
    fun bindingAdapterPosition() {
        val rv = content.viewPager2.getChildAt(0) as RecyclerView
        val holder = loopPagerAdapter.onCreateViewHolder(rv, 0)
        loopPagerAdapter.bindViewHolder(holder, 0)
        ViewHolder::class.java.getDeclaredField("mOwnerRecyclerView")
            .apply { isAccessible = true }.set(holder, rv)
        assertThat(holder.bindingAdapterPosition).isEqualTo(1)
    }

    @Test
    fun forwardNotifyDataSetChanged() {
        contentAdapter.notifyDataSetChanged()
        verify(exactly = 1) { loopPagerObserver.onChanged() }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * ```
     */
    @Test
    fun forwardNotifyItemRangeChanged() {
        val positionStart = 0
        val itemCount = 2
        val payload = "payload"
        contentAdapter.notifyItemRangeChanged(positionStart, itemCount, payload)
        val start = positionStart + 2
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(start, itemCount, payload) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     *
     * -> insert D
     * {C* ，D* ，A ，B ，C ，D ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6 ，7}
     * bindingAdapterPositions = {2 ，3 ，0 ，1 ，2 ，3 ，0 ，1}
     * ```
     */
    @Test
    fun forwardNotifyItemRangeInserted() {
        contentAdapter.count += 1
        val positionStart = contentAdapter.itemCount
        val itemCount = 1
        contentAdapter.notifyItemRangeInserted(positionStart, itemCount)
        val start = positionStart + 2
        verify(exactly = 1) { loopPagerObserver.onItemRangeInserted(start, itemCount) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     *
     * -> remove C
     * {A* ，B* ，A ，B ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5}
     * bindingAdapterPositions = {0 ，1 ，0 ，1 ，0 ，1}
     * ```
     */
    @Test
    fun forwardNotifyItemRangeRemoved() {
        contentAdapter.count -= 1
        val positionStart = contentAdapter.itemCount - 1
        val itemCount = 1
        contentAdapter.notifyItemRangeRemoved(positionStart, itemCount)
        val start = positionStart + 2
        verify(exactly = 1) { loopPagerObserver.onItemRangeRemoved(start, itemCount) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     *
     * -> move C to A
     * {A* ，B* ，C ，A ，B ，C* ，A*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * ```
     */
    @Test
    fun forwardNotifyItemMoved() {
        val fromPosition = 2
        val toPosition = 0
        contentAdapter.notifyItemMoved(fromPosition, toPosition)
        val from = fromPosition + 2
        val to = toPosition + 2
        verify(exactly = 1) { loopPagerObserver.onItemRangeMoved(from, to, 1) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * headerLayoutPositionRange = [0, 1]
     * footerLayoutPositionRange = [5, 6]
     * ```
     */
    @Test
    fun updateExtraPageForChanged() {
        contentAdapter.notifyDataSetChanged()
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(0, 2) }
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(5, 2) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * positionStart <= 1，无需更新
     * ```
     */
    @Test
    fun updateHeaderForInserted1() {
        contentAdapter.count += 1
        contentAdapter.notifyItemRangeInserted(1, 1)
        verify(exactly = 0) { loopPagerObserver.onItemRangeChanged(0, any(), any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * positionStart = 2，更新layoutPositionRange = [0, 0]
     * ```
     */
    @Test
    fun updateHeaderForInserted2() {
        contentAdapter.count += 1
        contentAdapter.notifyItemRangeInserted(2, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(0, 1, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * positionStart = 3，更新layoutPositionRange = [0, 1]
     * ```
     */
    @Test
    fun updateHeaderForInserted3() {
        contentAdapter.count += 1
        contentAdapter.notifyItemRangeInserted(3, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(0, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * positionStart = 0，无需更新
     * ```
     */
    @Test
    fun updateHeaderForRemoved1() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRangeRemoved(0, 1)
        verify(exactly = 0) { loopPagerObserver.onItemRangeChanged(0, any(), any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * positionStart = 1，更新layoutPositionRange = [0, 0]
     * ```
     */
    @Test
    fun updateHeaderForRemoved2() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRangeRemoved(1, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(0, 1, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * positionStart = 2，更新layoutPositionRange = [0, 1]
     * ```
     */
    @Test
    fun updateHeaderForRemoved3() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRangeRemoved(2, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(0, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * fromPosition = 0，toPosition = 0，无需更新
     * ```
     */
    @Test
    fun updateHeaderForMoved1() {
        contentAdapter.notifyItemMoved(0, 0)
        verify(exactly = 0) { loopPagerObserver.onItemRangeChanged(0, any(), any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * fromPosition = 0，toPosition = 1，更新layoutPositionRange = [0, 0]
     * ```
     */
    @Test
    fun updateHeaderForMoved2() {
        contentAdapter.notifyItemMoved(0, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(0, 1, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * fromPosition = 0，toPosition = 2，更新layoutPositionRange = [0, 1]
     * ```
     */
    @Test
    fun updateHeaderForMoved3() {
        contentAdapter.notifyItemMoved(0, 2)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(0, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * remove C -> move A to B
     * ```
     */
    @Test
    fun updateHeaderForComplex() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRemoved(2)
        contentAdapter.notifyItemMoved(0, 1)
        verify(exactly = 2) { loopPagerObserver.onItemRangeChanged(0, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * positionStart > 1，无需更新
     * ```
     */
    @Test
    fun updateFooterForInserted1() {
        contentAdapter.count += 1
        contentAdapter.notifyItemRangeInserted(2, 1)
        verify(exactly = 0) { loopPagerObserver.onItemRangeChanged(5, any(), any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * positionStart = 1，更新layoutPositionRange = [6, 6]
     * ```
     */
    @Test
    fun updateFooterForInserted2() {
        contentAdapter.count += 1
        contentAdapter.notifyItemRangeInserted(1, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(6, 1, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * positionStart = 0，更新layoutPositionRange = [5, 6]
     * ```
     */
    @Test
    fun updateFooterForInserted3() {
        contentAdapter.count += 1
        contentAdapter.notifyItemRangeInserted(0, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(5, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * positionStart = 2，无需更新
     * ```
     */
    @Test
    fun updateFooterForRemoved1() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRangeRemoved(2, 1)
        verify(exactly = 0) { loopPagerObserver.onItemRangeChanged(5, any(), any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * positionStart = 1，更新layoutPositionRange = [6, 6]
     * ```
     */
    @Test
    fun updateFooterForRemoved2() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRangeRemoved(1, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(6, 1, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * positionStart = 0，更新layoutPositionRange = [5, 6]
     * ```
     */
    @Test
    fun updateFooterForRemoved3() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRangeRemoved(0, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(5, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * fromPosition = 2，toPosition = 2，无需更新
     * ```
     */
    @Test
    fun updateFooterForMoved1() {
        contentAdapter.notifyItemMoved(2, 2)
        verify(exactly = 0) { loopPagerObserver.onItemRangeChanged(5, any(), any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * fromPosition = 2，toPosition = 1，更新layoutPositionRange = [6, 6]
     * ```
     */
    @Test
    fun updateFooterForMoved2() {
        contentAdapter.notifyItemMoved(2, 1)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(6, 1, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * footerBindingAdapterPositionRange = [0, 1]
     *
     * fromPosition = 2，toPosition = 0，更新layoutPositionRange = [5, 6]
     * ```
     */
    @Test
    fun updateFooterForMoved3() {
        contentAdapter.notifyItemMoved(2, 0)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(5, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     *
     * remove A -> move C to B
     * ```
     */
    @Test
    fun updateFooterForComplex() {
        contentAdapter.count -= 1
        contentAdapter.notifyItemRemoved(0)
        contentAdapter.notifyItemMoved(1, 0)
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(5, 2) }
        verify(exactly = 1) { loopPagerObserver.onItemRangeChanged(5, 2, any()) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * currentCount = 3
     * ```
     */
    @Test
    fun totalPageCount() {
        assertThat(loopPagerAdapter.itemCount).isEqualTo(7)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*} -> {C}
     * ```
     */
    @Test
    fun notSupportLoopAfterRemoved() {
        contentAdapter.count = 1
        contentAdapter.notifyItemRangeRemoved(0, 2)
        assertThat(loopPagerAdapter.itemCount).isEqualTo(1)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*} -> {C} -> {B* ，C* ，A ，B ，C ，A* ，B*}
     * ```
     */
    @Test
    fun supportLoopAfterInserted() {
        contentAdapter.count = 1
        contentAdapter.notifyItemRangeRemoved(0, 2)
        assertThat(loopPagerAdapter.itemCount).isEqualTo(1)

        contentAdapter.count = 3
        contentAdapter.notifyItemRangeInserted(0, 2)
        assertThat(loopPagerAdapter.itemCount).isEqualTo(7)
    }
}