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
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.test.core.app.ActivityScenario.launch
import com.google.common.truth.Truth.assertThat
import com.xiaocydx.cxrv.viewpager2.TestActivity
import com.xiaocydx.cxrv.viewpager2.TestAdapter
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.DEFAULT_SUPPORT_LOOP_COUNT
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.PADDING_EXTRA_PAGE_LIMIT
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [LoopPagerContent]的单元测试
 *
 * @author xcc
 * @date 2023/5/14
 */
@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(RobolectricTestRunner::class)
internal class LoopPagerContentTest {
    private lateinit var content: LoopPagerContent

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
                    adapter = TestAdapter(count = 3),
                    extraPageLimit = PADDING_EXTRA_PAGE_LIMIT,
                    supportLoopCount = DEFAULT_SUPPORT_LOOP_COUNT
                )
            }
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * currentCount = 3
     * ```
     */
    @Test
    fun itemCount() {
        assertThat(content.itemCount).isEqualTo(3)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * supportLoop = true
     * ```
     */
    @Test
    fun supportLoop() {
        assertThat(content.supportLoop()).isTrue()
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * ```
     */
    @Test
    fun toLayoutPosition() {
        var layoutPosition = content.toLayoutPosition(0)
        assertThat(layoutPosition).isEqualTo(2)
        layoutPosition = content.toLayoutPosition(2)
        assertThat(layoutPosition).isEqualTo(4)
        layoutPosition = content.toLayoutPosition(3)
        assertThat(layoutPosition).isEqualTo(NO_POSITION)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * layoutPositionRange = [0, 6]
     * ```
     */
    @Test
    fun firstLayoutPosition() {
        assertThat(content.firstLayoutPosition()).isEqualTo(0)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * layoutPositionRange = [0, 6]
     * ```
     */
    @Test
    fun lastLayoutPosition() {
        assertThat(content.lastLayoutPosition()).isEqualTo(6)
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
    fun firstExtraLayoutPosition() {
        assertThat(content.firstExtraLayoutPosition(isHeader = true)).isEqualTo(0)
        assertThat(content.firstExtraLayoutPosition(isHeader = false)).isEqualTo(5)
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
    fun lastExtraLayoutPosition() {
        assertThat(content.lastExtraLayoutPosition(isHeader = true)).isEqualTo(1)
        assertThat(content.lastExtraLayoutPosition(isHeader = false)).isEqualTo(6)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * layoutPositions = {0 ，1 ，2 ，3 ，4 ，5 ，6}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * ```
     */
    @Test
    fun toBindingAdapterPosition() {
        var bindingAdapterPosition = content.toBindingAdapterPosition(0)
        assertThat(bindingAdapterPosition).isEqualTo(1)
        bindingAdapterPosition = content.toBindingAdapterPosition(5)
        assertThat(bindingAdapterPosition).isEqualTo(0)
        bindingAdapterPosition = content.toBindingAdapterPosition(7)
        assertThat(bindingAdapterPosition).isEqualTo(NO_POSITION)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * bindingAdapterPositionRange = [0, 2]
     * ```
     */
    @Test
    fun firstBindingAdapterPosition() {
        assertThat(content.firstBindingAdapterPosition()).isEqualTo(0)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * bindingAdapterPositionRange = [0, 2]
     * ```
     */
    @Test
    fun lastBindingAdapterPosition() {
        assertThat(content.lastBindingAdapterPosition()).isEqualTo(2)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     * footerBindingAdapterPositionRange = [0, 1]
     * ```
     */
    @Test
    fun firstExtraBindingAdapterPosition() {
        assertThat(content.firstExtraBindingAdapterPosition(isHeader = true)).isEqualTo(1)
        assertThat(content.firstExtraBindingAdapterPosition(isHeader = false)).isEqualTo(0)
    }

    /**
     * ```
     * {B* ，C* ，A ，B ，C ，A* ，B*}
     * bindingAdapterPositions = {1 ，2 ，0 ，1 ，2 ，0 ，1}
     * headerBindingAdapterPositionRange = [1, 2]
     * footerBindingAdapterPositionRange = [0, 1]
     * ```
     */
    @Test
    fun lastExtraBindingAdapterPosition() {
        assertThat(content.lastExtraBindingAdapterPosition(isHeader = true)).isEqualTo(2)
        assertThat(content.lastExtraBindingAdapterPosition(isHeader = false)).isEqualTo(1)
    }
}