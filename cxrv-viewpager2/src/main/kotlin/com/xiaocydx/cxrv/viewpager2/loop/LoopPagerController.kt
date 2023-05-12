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

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.viewpager2.loop

import androidx.annotation.Px
import androidx.recyclerview.widget.LoopPagerAdapter
import androidx.recyclerview.widget.LoopPagerScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.xiaocydx.cxrv.internal.PreDrawListener

/**
 * [ViewPager2]循环页面的控制器，不支持反转布局
 *
 * @author xcc
 * @date 2023/5/9
 */
class LoopPagerController(private val viewPager2: ViewPager2) {
    private var extraPageLimit = 1
    private var checker: LoopPagerChecker? = null
    private var content: LoopPagerContent? = null
    private var scroller: LoopPagerScroller? = null
    private var observer: NotEmptyObserver? = null
    private var callbacks: MutableMap<OnPageChangeCallback, CallbackWrapper>? = null
    private val recyclerView = viewPager2.getChildAt(0) as RecyclerView

    /**
     * 对[ViewPager2]设置[adapter]的循环页面适配器
     *
     * 1. [LoopPagerController]的实现确保`holder.bindingAdapter`是[adapter]。
     * 2. [LoopPagerController]的实现确保`holder.bindingAdapterPosition`是[adapter]的`position`。
     * 这表示不会对使用`holder.bindingAdapter`和`holder.bindingAdapterPosition`实现的功能造成影响，
     * [adapter]的内部逻辑应当通过`holder.bindingAdapterPosition`访问数据源，以确保展示的内容正确。
     */
    fun setAdapter(adapter: Adapter<*>) {
        checker?.removeListener()
        checker = if (CHECKED_ENABLED) LoopPagerChecker() else null
        content = LoopPagerContent(viewPager2, adapter, extraPageLimit)
        scroller?.removeCallback()
        scroller = LoopPagerScroller(content!!)
        viewPager2.adapter = LoopPagerAdapter(content!!, scroller!!::updateAnchor)
        scrollToPosition(0)
    }

    /**
     * 对[ViewPager2]设置`paddings`，该函数用于展示多个页面的场景
     */
    fun setPadding(
        @Px left: Int = recyclerView.left,
        @Px top: Int = recyclerView.top,
        @Px right: Int = recyclerView.right,
        @Px bottom: Int = recyclerView.bottom
    ) {
        recyclerView.clipToPadding = false
        recyclerView.setPadding(left, top, right, bottom)
        val previousLimit = extraPageLimit
        val hasPadding = left != 0 || right != 0 || top != 0 || bottom != 0
        extraPageLimit = if (!hasPadding) NORMAL_EXTRA_PAGE_LIMIT else PADDING_EXTRA_PAGE_LIMIT
        if (extraPageLimit > previousLimit) content?.adapter?.let(::setAdapter)
    }

    /**
     * [ViewPager2]非平滑滚动至[position]，需要先调用[setAdapter]
     *
     * 若[setAdapter]设置的`adapter`，其`adapter.itemCount`为0，
     * 则当`adapter.itemCount`大于0时，才非平滑滚动至[position]。
     *
     * @param position 取值范围是`[0, adapter.itemCount)`，即`holder.bindingAdapterPosition`。
     */
    fun scrollToPosition(position: Int) {
        val content = content ?: return
        val wait = waitNotEmptyIfNecessary { scrollToPosition(position) }
        if (wait) return
        scroller?.scrollToPosition(content.toLayoutPosition(position))
    }

    /**
     * [ViewPager2]平滑滚动至[position]，需要先调用[setAdapter]
     *
     * 若[setAdapter]设置的`adapter`，其`adapter.itemCount`为0，
     * 则当`adapter.itemCount`大于0时，才平滑滚动至[position]。
     *
     * **注意**：[LoopPagerController]的实现会将循环页面的平滑滚动，
     * 优化至[ViewPager2.setCurrentItem]一样的效果，没有额外性能损耗。
     *
     * @param position  取值范围是`[0, adapter.itemCount)`，即`holder.bindingAdapterPosition`。
     * @param direction [position]对应`item`的查找方向，以[ViewPager2]水平布局方向为例，
     * [LookupDirection.END]向右查找[position]对应的`item`，并向右平滑滚动至[position]。
     */
    fun smoothScrollToPosition(position: Int, direction: LookupDirection = LookupDirection.END) {
        val content = content ?: return
        val wait = waitNotEmptyIfNecessary { smoothScrollToPosition(position, direction) }
        if (wait) return
        scroller?.smoothScrollToPosition(content.toLayoutPosition(position), direction)
    }

    private fun waitNotEmptyIfNecessary(action: () -> Unit): Boolean {
        observer?.removeObserver()
        observer = null
        val adapter = viewPager2.adapter as? LoopPagerAdapter
        if (adapter != null && adapter.itemCount == 0) {
            observer = NotEmptyObserver(adapter, action)
            return true
        }
        return false
    }

    /**
     * 注册[OnPageChangeCallback]
     *
     * @param callback 在调用[setAdapter]设置`adapter`后，才会执行[callback]的回调函数，
     * 对[OnPageChangeCallback.onPageScrolled]和[OnPageChangeCallback.onPageSelected]
     * 传入的`position`取值范围是`[0, adapter.itemCount)`，即`holder.bindingAdapterPosition`。
     */
    fun registerOnPageChangeCallback(callback: OnPageChangeCallback) {
        if (getCallbacks().containsKey(callback)) return
        viewPager2.registerOnPageChangeCallback(callback)
    }

    /**
     * 取消[registerOnPageChangeCallback]注册的[OnPageChangeCallback]
     */
    fun unregisterOnPageChangeCallback(callback: OnPageChangeCallback) {
        val wrapper = getCallbacks().remove(callback) ?: return
        viewPager2.unregisterOnPageChangeCallback(wrapper)
    }

    private fun getCallbacks(): MutableMap<OnPageChangeCallback, CallbackWrapper> {
        if (callbacks == null) callbacks = mutableMapOf()
        return callbacks!!
    }

    private inner class LoopPagerChecker : PreDrawListener(viewPager2) {

        override fun onPreDraw(): Boolean {
            require(viewPager2.adapter is LoopPagerAdapter) {
                "ViewPager的adapter被替换，无法支持循环页面"
            }
            return super.onPreDraw()
        }
    }

    private inner class CallbackWrapper(private val delegate: OnPageChangeCallback) : OnPageChangeCallback() {

        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            val bindingAdapterPosition = content?.toBindingAdapterPosition(position) ?: return
            delegate.onPageScrolled(bindingAdapterPosition, positionOffset, positionOffsetPixels)
        }

        override fun onPageSelected(position: Int) {
            val bindingAdapterPosition = content?.toBindingAdapterPosition(position) ?: return
            delegate.onPageSelected(bindingAdapterPosition)
        }

        override fun onPageScrollStateChanged(state: Int) {
            content ?: return
            delegate.onPageScrollStateChanged(state)
        }
    }

    private companion object {
        const val CHECKED_ENABLED = false
        const val NORMAL_EXTRA_PAGE_LIMIT = 1
        const val PADDING_EXTRA_PAGE_LIMIT = 2
    }
}

/**
 * [LoopPagerController.smoothScrollToPosition]的查找方向
 */
enum class LookupDirection {

    /**
     * 开始的查找方向
     */
    START,

    /**
     * 结束的查找方向
     */
    END
}