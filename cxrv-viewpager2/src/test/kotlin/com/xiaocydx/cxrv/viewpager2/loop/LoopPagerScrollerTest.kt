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
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.viewpager2.AdapterCallback
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
 * [LoopPagerScroller]的单元测试
 *
 * @author xcc
 * @date 2023/5/14
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class LoopPagerScrollerTest {
    private lateinit var content: LoopPagerContent
    private lateinit var loopPagerScroller: LoopPagerScroller
    private val contentCallback: AdapterCallback = spyk()

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
                content = LoopPagerContent(
                    viewPager2 = it.viewPager2,
                    adapter = TestAdapter(count = 3).apply {
                        callback = contentCallback
                    },
                    extraPageLimit = PADDING_EXTRA_PAGE_LIMIT
                )
                loopPagerScroller = LoopPagerScroller(content)
                it.viewPager2.adapter = LoopPagerAdapter(content, loopPagerScroller)
            }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * currentItem = 0 -> currentItem = 2
     * ```
     */
    @Test
    fun scrollToPosition() {
        assertThat(content.viewPager2.currentItem).isEqualTo(0)
        loopPagerScroller.scrollToPosition(2)
        assertThat(content.viewPager2.currentItem).isEqualTo(2)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * currentItem = 1 -> currentItem = 4，移除itemView，绑定新的holder
     * ```
     */
    @Test
    fun defaultUpdateAnchorWithoutOptimization() {
        verify(exactly = 0) { contentCallback.onBindViewHolder(any(), 2, any()) }
        loopPagerScroller.scrollToPosition(1)

        val previous = content.findViewHolderForLayoutPosition(1)
        verify(exactly = 1) { contentCallback.onBindViewHolder(previous, 2, any()) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previous) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previous) }

        loopPagerScroller.updateAnchorForCurrent()

        // currentItem = 1 -> currentItem = 4，移除itemView，绑定新的holder
        assertThat(content.viewPager2.currentItem).isEqualTo(4)
        val current = content.findViewHolderForLayoutPosition(4)
        assertThat(current).isNotSameInstanceAs(previous)
        verify(exactly = 1) { contentCallback.onBindViewHolder(current, 2, any()) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(current) }
        verify(exactly = 1) { contentCallback.onViewDetachedFromWindow(previous) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * currentItem = 1 -> currentItem = 4，不移除itemView，不绑定新的holder
     * ```
     */
    @Test
    fun defaultUpdateAnchorWithOptimization() = withOptimization {
        verify(exactly = 0) { contentCallback.onBindViewHolder(any(), 2, any()) }
        loopPagerScroller.scrollToPosition(1)

        val previousC = content.findViewHolderForLayoutPosition(1)
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousC, 2, any()) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousC) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousC) }

        loopPagerScroller.updateAnchorForCurrent()

        // currentItem = 1 -> currentItem = 4，不移除itemView，不绑定新的holder
        assertThat(content.viewPager2.currentItem).isEqualTo(4)
        val currentC = content.findViewHolderForLayoutPosition(4)
        assertThat(currentC).isSameInstanceAs(previousC)
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousC, 2, any()) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousC) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousC) }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * currentItem = 1 -> currentItem = 4，移除itemView，绑定新的holder
     * ```
     */
    @Test
    fun paddingUpdateAnchorWithoutOptimization() {
        content.setupPadding()
        loopPagerScroller.scrollToPosition(1)

        val previousB = content.findViewHolderForLayoutPosition(0)
        val previousC = content.findViewHolderForLayoutPosition(1)
        val previousA = content.findViewHolderForLayoutPosition(2)
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousB, 1, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousC, 2, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousA, 0, any()) }

        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousB) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousC) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousA) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousB) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousC) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousA) }

        loopPagerScroller.updateAnchorForCurrent()

        // currentItem = 1 -> currentItem = 4，移除itemView，绑定新的holder
        assertThat(content.viewPager2.currentItem).isEqualTo(4)
        val currentB = content.findViewHolderForLayoutPosition(3)
        val currentC = content.findViewHolderForLayoutPosition(4)
        val currentA = content.findViewHolderForLayoutPosition(5)
        assertThat(currentB).isNotSameInstanceAs(previousB)
        assertThat(currentC).isNotSameInstanceAs(previousC)
        assertThat(currentA).isNotSameInstanceAs(previousA)

        verify(exactly = 1) { contentCallback.onBindViewHolder(currentB, 1, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(currentC, 2, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(currentA, 0, any()) }

        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(currentB) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(currentC) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(currentA) }
        verify(exactly = 1) { contentCallback.onViewDetachedFromWindow(previousB) }
        verify(exactly = 1) { contentCallback.onViewDetachedFromWindow(previousC) }
        verify(exactly = 1) { contentCallback.onViewDetachedFromWindow(previousA) }

        content.restorePadding()
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * currentItem = 1 -> currentItem = 4，不移除itemView，不绑定新的holder
     * ```
     */
    @Test
    fun paddingUpdateAnchorWithOptimization() = withOptimization {
        content.setupPadding()
        loopPagerScroller.scrollToPosition(1)

        val previousB = content.findViewHolderForLayoutPosition(0)
        val previousC = content.findViewHolderForLayoutPosition(1)
        val previousA = content.findViewHolderForLayoutPosition(2)
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousB, 1, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousC, 2, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousA, 0, any()) }

        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousB) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousC) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousA) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousB) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousC) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousA) }

        loopPagerScroller.updateAnchorForCurrent()

        // currentItem = 1 -> currentItem = 4，不移除itemView，不绑定新的holder
        assertThat(content.viewPager2.currentItem).isEqualTo(4)
        val currentB = content.findViewHolderForLayoutPosition(3)
        val currentC = content.findViewHolderForLayoutPosition(4)
        val currentA = content.findViewHolderForLayoutPosition(5)
        assertThat(currentB).isSameInstanceAs(previousB)
        assertThat(currentC).isSameInstanceAs(previousC)
        assertThat(currentA).isSameInstanceAs(previousA)

        verify(exactly = 1) { contentCallback.onBindViewHolder(previousB, 1, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousC, 2, any()) }
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousA, 0, any()) }

        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousB) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousC) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousA) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousB) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousC) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousA) }

        content.restorePadding()
    }

    private inline fun withOptimization(block: () -> Unit) {
        val previous = AnchorOptimization.CHECK_SCROLL_STATE
        AnchorOptimization.CHECK_SCROLL_STATE = false
        block()
        AnchorOptimization.CHECK_SCROLL_STATE = previous
    }

    private val LoopPagerContent.recyclerView: RecyclerView
        get() = viewPager2.getChildAt(0) as RecyclerView

    private fun LoopPagerContent.setupPadding() {
        recyclerView.clipToPadding = false
        recyclerView.setPadding(100, 0, 100, 0)
    }

    private fun LoopPagerContent.restorePadding() {
        recyclerView.clipToPadding = true
        recyclerView.setPadding(0, 0, 0, 0)
    }

    private fun LoopPagerContent.findViewHolderForLayoutPosition(layoutPosition: Int): ViewHolder {
        return requireNotNull(recyclerView.findViewHolderForLayoutPosition(layoutPosition))
    }

    private class TestTargetScrapStore : TargetScrapStore {
        private val mutableMap = mutableMapOf<Int, ViewHolder>()

        override val size: Int
            get() = mutableMap.size

        override fun get(bindingAdapterPosition: Int): ViewHolder? {
            return mutableMap[bindingAdapterPosition]
        }

        override fun set(bindingAdapterPosition: Int, holder: ViewHolder) {
            mutableMap[bindingAdapterPosition] = holder
        }

        override fun valueAt(index: Int): ViewHolder {
            mutableMap.onEachIndexed { i, entry -> if (i == index) return entry.value }
            throw ArrayIndexOutOfBoundsException(index)
        }

        override fun clear() = mutableMap.clear()
    }

    private companion object {
        init {
            AnchorOptimization.targetScrapStoreProvider = { TestTargetScrapStore() }
        }
    }
}