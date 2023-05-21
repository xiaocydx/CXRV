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
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.viewpager2.AdapterCallback
import com.xiaocydx.cxrv.viewpager2.TestActivity
import com.xiaocydx.cxrv.viewpager2.TestAdapter
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.DEFAULT_SUPPORT_LOOP_COUNT
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
    private lateinit var updater: TestLoopAnchorUpdater
    private lateinit var scroller: LoopPagerScroller
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
                    extraPageLimit = PADDING_EXTRA_PAGE_LIMIT,
                    supportLoopCount = DEFAULT_SUPPORT_LOOP_COUNT
                )
                updater = TestLoopAnchorUpdater()
                scroller = LoopPagerScroller(content, updater)
                it.viewPager2.adapter = LoopPagerAdapter(content, scroller)
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
        scroller.scrollToPosition(2)
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
    fun defaultUpdateAnchorWithoutOptimization() = withoutOptimization {
        verify(exactly = 0) { contentCallback.onBindViewHolder(any(), 2, any()) }
        scroller.scrollToPosition(1)

        val previous = content.findViewHolderForLayoutPosition(1)
        verify(exactly = 1) { contentCallback.onBindViewHolder(previous, 2, any()) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previous) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previous) }

        scroller.updateAnchorInfo(UpdateReason.DRAGGING, content)

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
    fun defaultUpdateAnchorWithOptimization() {
        verify(exactly = 0) { contentCallback.onBindViewHolder(any(), 2, any()) }
        scroller.scrollToPosition(1)

        val previousC = content.findViewHolderForLayoutPosition(1)
        verify(exactly = 1) { contentCallback.onBindViewHolder(previousC, 2, any()) }
        verify(exactly = 1) { contentCallback.onViewAttachedToWindow(previousC) }
        verify(exactly = 0) { contentCallback.onViewDetachedFromWindow(previousC) }

        scroller.updateAnchorInfo(UpdateReason.DRAGGING, content)

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
    fun paddingUpdateAnchorWithoutOptimization() = withoutOptimization {
        content.setupPadding()
        scroller.scrollToPosition(1)

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

        scroller.updateAnchorInfo(UpdateReason.DRAGGING, content)

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
    fun paddingUpdateAnchorWithOptimization() {
        content.setupPadding()
        scroller.scrollToPosition(1)

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

        scroller.updateAnchorInfo(UpdateReason.DRAGGING, content)

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

    private inline fun withoutOptimization(block: () -> Unit) {
        val previous = updater.isOptimizationEnabled
        updater.isOptimizationEnabled = false
        block()
        updater.isOptimizationEnabled = previous
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

    private class TestLoopAnchorUpdater : LoopAnchorUpdater {
        private val optimization = LoopAnchorUpdaterImpl(TestTargetScrapStore())
        var isOptimizationEnabled = true

        override fun updateAnchorInfo(reason: UpdateReason, content: LoopPagerContent) {
            if (reason === UpdateReason.SCROLL) return
            if (isOptimizationEnabled) {
                optimization.updateAnchorInfo(reason, content)
            } else {
                val anchorPosition = getNewAnchorPositionForContent(content)
                if (anchorPosition == NO_POSITION) return
                content.viewPager2.setCurrentItem(anchorPosition, false)
            }
        }

        override fun removeUpdateAnchorInfoPending() {
            optimization.removeUpdateAnchorInfoPending()
        }

        private fun getNewAnchorPositionForContent(content: LoopPagerContent): Int {
            if (!content.supportLoop()) return NO_POSITION
            val headerFirst = content.firstExtraLayoutPosition(isHeader = true)
            val headerLast = content.lastExtraLayoutPosition(isHeader = true)
            val footerFirst = content.firstExtraLayoutPosition(isHeader = false)
            val footerLast = content.lastExtraLayoutPosition(isHeader = false)
            return when (val currentItem = content.viewPager2.currentItem) {
                in headerFirst..headerLast -> currentItem + content.itemCount
                in footerFirst..footerLast -> currentItem - content.itemCount
                else -> NO_POSITION
            }
        }
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
}