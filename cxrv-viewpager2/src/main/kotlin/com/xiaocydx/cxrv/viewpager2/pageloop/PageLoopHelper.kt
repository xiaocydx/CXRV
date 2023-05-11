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

package com.xiaocydx.cxrv.viewpager2.pageloop

import androidx.recyclerview.widget.PageLoopAdapter
import androidx.recyclerview.widget.PageLoopCallback
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewpager2.widget.ViewPager2

/**
 * @author xcc
 * @date 2023/5/9
 */
class PageLoopHelper(
    private val viewPager2: ViewPager2,
    private val contentAdapter: Adapter<*>,
    private val extraPageLimit: Int = 1
) {
    private var notEmptyObserver: NotEmptyObserver? = null
    private var pageLoopCallback: PageLoopCallback? = null
    private var pageLoopAdapter: PageLoopAdapter? = null

    fun attach() = apply {
        if (pageLoopAdapter != null) return@apply
        val content = PageLoopContent(contentAdapter, extraPageLimit)
        pageLoopCallback = content.createPageLoopCallback(viewPager2)
        pageLoopAdapter = content.createPageLoopAdapter(pageLoopCallback!!::updateAnchor)
        pageLoopCallback?.let(viewPager2::registerOnPageChangeCallback)
        viewPager2.adapter = pageLoopAdapter
    }

    fun detach() = apply {
        if (pageLoopAdapter == null) return@apply
        viewPager2.adapter = contentAdapter
        pageLoopCallback?.let(viewPager2::unregisterOnPageChangeCallback)
        notEmptyObserver?.removeObserver()
        notEmptyObserver = null
        pageLoopAdapter = null
        pageLoopCallback = null
    }

    fun setCurrentItem(position: Int, smoothScroll: Boolean = false) {
        val pageLoopAdapter = pageLoopAdapter ?: return
        assert(viewPager2.adapter == pageLoopAdapter)

        notEmptyObserver?.removeObserver()
        notEmptyObserver = null
        if (pageLoopAdapter.itemCount == 0) {
            notEmptyObserver = NotEmptyObserver(pageLoopAdapter) {
                setCurrentItem(position, smoothScroll)
            }
            return
        }

        var layoutPosition = position
        if (pageLoopAdapter.itemCount > 1) {
            layoutPosition += extraPageLimit
        }
        viewPager2.setCurrentItem(layoutPosition, smoothScroll)
    }
}