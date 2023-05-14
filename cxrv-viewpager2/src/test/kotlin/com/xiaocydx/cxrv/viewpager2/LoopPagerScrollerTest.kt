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

package com.xiaocydx.cxrv.viewpager2

import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LoopPagerScroller
import androidx.test.core.app.ActivityScenario
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

    @Test
    fun test() {
        ActivityScenario.launch(TestActivity::class.java)
            .moveToState(Lifecycle.State.CREATED)
            .onActivity {
                val viewPager2 = it.viewPager2
            }
    }
}