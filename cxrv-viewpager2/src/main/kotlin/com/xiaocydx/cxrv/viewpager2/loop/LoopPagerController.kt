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

import androidx.recyclerview.widget.LoopPagerAdapter
import androidx.recyclerview.widget.LoopPagerCallback
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewpager2.widget.ViewPager2

/**
 * @author xcc
 * @date 2023/5/9
 */
class LoopPagerController(
    private val viewPager2: ViewPager2,
    private val contentAdapter: Adapter<*>,
    private val extraPageLimit: Int = 1
) {
    private var notEmptyObserver: NotEmptyObserver? = null
    private var loopPagerAdapter: LoopPagerAdapter? = null
    private var loopPagerCallback: LoopPagerCallback? = null

    // TODO: 补充运行时的loopPagerAdapter断言
    fun attach() = apply {
        if (loopPagerAdapter != null) return@apply
        val content = LoopPagerContent(contentAdapter, extraPageLimit)
        loopPagerAdapter = content.createLoopPagerAdapter()
        loopPagerCallback = content.createLoopPagerCallback(viewPager2)
        loopPagerCallback?.attach()
        viewPager2.adapter = loopPagerAdapter
    }

    fun detach() = apply {
        if (loopPagerAdapter == null) return@apply
        viewPager2.adapter = contentAdapter
        loopPagerCallback?.detach()
        notEmptyObserver?.removeObserver()
        notEmptyObserver = null
        loopPagerAdapter = null
        loopPagerCallback = null
    }

    // TODO: 观察平滑滚动是否符合预期，需要记录layoutManager的状态么？
    fun setCurrentItem(position: Int, smoothScroll: Boolean = false) {
        val loopPagerAdapter = loopPagerAdapter ?: return
        assert(viewPager2.adapter == loopPagerAdapter)

        notEmptyObserver?.removeObserver()
        notEmptyObserver = null
        if (loopPagerAdapter.itemCount == 0) {
            notEmptyObserver = NotEmptyObserver(loopPagerAdapter) {
                setCurrentItem(position, smoothScroll)
            }
            return
        }

        var layoutPosition = position
        if (loopPagerAdapter.itemCount > 1) {
            layoutPosition += extraPageLimit
        }
        viewPager2.setCurrentItem(layoutPosition, smoothScroll)
    }
}