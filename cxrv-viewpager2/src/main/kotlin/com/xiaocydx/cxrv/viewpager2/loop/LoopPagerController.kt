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

import android.content.Context
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.recyclerview.widget.LoopPagerAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.pendingSavedState
import androidx.recyclerview.widget.recyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import androidx.viewpager2.widget.ViewPager2.PageTransformer
import androidx.viewpager2.widget.ViewPager2.SCROLL_STATE_IDLE
import androidx.viewpager2.widget.ViewPager2.ScrollState
import com.xiaocydx.cxrv.viewpager2.R
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.DEFAULT_EXTRA_PAGE_LIMIT
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.DEFAULT_SUPPORT_LOOP_COUNT
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerContent.Companion.PADDING_EXTRA_PAGE_LIMIT

/**
 * [ViewPager2]循环页面的控制器
 *
 * ```
 * val controller = LoopPagerController(viewPager2)
 * controller.setAdapter(adapter) // 必须调用
 * controller.setPadding(left, top, right, bottom) // 可选调用
 * controller.registerOnPageChangeCallback(callback) // 可选调用
 *
 * // 调用者可通过以下属性和函数，自行实现轮播交互
 * controller.scrollState
 * controller.currentPosition
 * controller.smoothScrollToPosition(position, direction, provider)
 * ```
 *
 * @author xcc
 * @date 2023/5/9
 */
class LoopPagerController(
    internal val viewPager2: ViewPager2,
    supportLoopCount: Int = DEFAULT_SUPPORT_LOOP_COUNT
) {
    private var extraPageLimit = DEFAULT_EXTRA_PAGE_LIMIT
    private var content: LoopPagerContent? = null
    private var scroller: LoopPagerScroller? = null
    private var listener: StopScrollListener? = null
    private var observer: NotEmptyDataObserver? = null
    private var callbacks: MutableMap<OnPageChangeCallback, CallbackWrapper>? = null

    /**
     * 当前选择的位置
     *
     * 该属性的取值范围是`[0, adapter.itemCount)`，即`holder.bindingAdapterPosition`，
     * 若未调用[setAdapter]设置`adapter`或者`adapter.itemCount`为0，则返回[NO_POSITION]。
     */
    val currentPosition: Int
        get() = content?.toBindingAdapterPosition(viewPager2.currentItem) ?: NO_POSITION

    /**
     * 支持循环的`adapter.itemCount`至少数量，该属性不会小于[DEFAULT_SUPPORT_LOOP_COUNT]
     */
    val supportLoopCount = supportLoopCount.coerceAtLeast(DEFAULT_SUPPORT_LOOP_COUNT)

    /**
     * 对[ViewPager2]设置[adapter]的循环页面适配器
     *
     * 1. [LoopPagerController]确保`holder.bindingAdapter`是[adapter]。
     * 2. [LoopPagerController]确保`holder.bindingAdapterPosition`是[adapter]的`position`。
     * 这表示不会对使用`holder.bindingAdapter`和`holder.bindingAdapterPosition`实现的功能造成影响，
     * [adapter]的内部逻辑应当通过`holder.bindingAdapterPosition`访问数据源，以确保绘制内容的正确性。
     *
     * @param adapter 如果是通过`adapter.notifyDataSetChanged()`更新页面，
     * 那么请确保是先修改数据，再调用`adapter.notifyDataSetChanged()`的顺序，
     * [LoopPagerController]基于这种顺序支持全量更新。
     * ```
     * // 不符合的顺序
     * adapter.notifyDataSetChanged()
     * data.removeAt(0)
     *
     * // 符合的顺序
     * data.removeAt(0)
     * adapter.notifyDataSetChanged()
     * ```
     */
    fun setAdapter(adapter: Adapter<*>) {
        content?.removeObserver()
        content = LoopPagerContent(
            viewPager2, adapter,
            extraPageLimit, supportLoopCount
        )
        scroller?.removeCallbacks()
        scroller = LoopPagerScroller(content!!)
        if (listener == null) {
            listener = StopScrollListener()
            viewPager2.addOnAttachStateChangeListener(listener)
        }
        viewPager2.adapter = LoopPagerAdapter(content!!, scroller!!)
        initAnchorIfNecessary()
    }

    /**
     * 视图重建过程会有`pendingSavedState`作为锚点信息，用于恢复滚动位置，
     * 设置的初始锚点信息不应当无效`pendingSavedState`的`anchorPosition`，
     * 在初始化阶段之后，确定没有`pendingSavedState`，才设置初始锚点信息，
     * 对[scrollToPosition]和[smoothScrollToPosition]不需要做这样的处理。
     */
    private fun initAnchorIfNecessary() {
        val content = content ?: return
        val scroller = scroller ?: return
        val wait = waitNotEmptyIfNecessary(::initAnchorIfNecessary)
        if (wait || viewPager2.recyclerView.pendingSavedState != null) return
        // 初始化阶段adapter.itemCount > 0，会直接设置初始锚点信息，不过这没有影响，
        // ViewPager2.restorePendingState()根据mPendingCurrentItem恢复currentItem。
        scroller.scrollToPosition(content.toLayoutPosition(0))
    }

    /**
     * 对[ViewPager2]设置`paddings`，该函数用于同时展示多个页面的场景
     */
    fun setPadding(
        @Px left: Int = viewPager2.recyclerView.left,
        @Px top: Int = viewPager2.recyclerView.top,
        @Px right: Int = viewPager2.recyclerView.right,
        @Px bottom: Int = viewPager2.recyclerView.bottom
    ) {
        viewPager2.recyclerView.clipToPadding = false
        viewPager2.recyclerView.setPadding(left, top, right, bottom)
        val previous = extraPageLimit
        val hasPadding = left != 0 || right != 0 || top != 0 || bottom != 0
        extraPageLimit = if (!hasPadding) DEFAULT_EXTRA_PAGE_LIMIT else PADDING_EXTRA_PAGE_LIMIT
        if (extraPageLimit > previous) content?.adapter?.let(::setAdapter)
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
        if (wait || position == content.toBindingAdapterPosition(viewPager2.currentItem)) return
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
     * @param direction [position]相应`item`的查找方向，以[ViewPager2]水平布局方向为例，
     * [LookupDirection.END]往右查找[position]相应的`item`，并往右平滑滚动至[position]。
     * @param provider  [SmoothScroller]的提供者，可用于修改平滑滚动的时长和插值器。
     */
    fun smoothScrollToPosition(
        position: Int,
        direction: LookupDirection = LookupDirection.END,
        provider: SmoothScrollerProvider? = null
    ) {
        val content = content ?: return
        val wait = waitNotEmptyIfNecessary { smoothScrollToPosition(position, direction, provider) }
        if (wait || position == content.toBindingAdapterPosition(viewPager2.currentItem)) return
        scroller?.smoothScrollToPosition(content.toLayoutPosition(position), direction, provider)
    }

    /**
     * [RecyclerView]提供的的滚动函数不会判断`adapter.itemCount`是否为0，
     * 但[ViewPager2.setCurrentItem]会判断，因此在不为0时，调用[action]。
     *
     * @return 是否需要等待`adapter.itemCount`不为0。
     */
    private inline fun waitNotEmptyIfNecessary(crossinline action: () -> Unit): Boolean {
        observer?.removeObserver()
        observer = null
        val adapter = viewPager2.adapter
        if (adapter != null && adapter.itemCount == 0) {
            // 在下一帧调用action()，确保adapter.itemCount是处理附加页面后的结果
            observer = NotEmptyDataObserver(adapter, nextFrame = true) {
                observer = null
                action()
            }
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
        val wrapper = CallbackWrapper(callback)
        getCallbacks()[callback] = wrapper
        viewPager2.registerOnPageChangeCallback(wrapper)
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

    /**
     * 停止滚动，若平滑滚动未完成，则修正`scrollState`和`currentItem`的布局位置
     */
    private inner class StopScrollListener : OnAttachStateChangeListener {

        override fun onViewAttachedToWindow(view: View) {
            if (observer != null) return
            scroller?.stopScrollToCurrent()
        }

        override fun onViewDetachedFromWindow(view: View) {
            observer?.removeObserver()
            observer = null
            scroller?.stopScrollToCurrent()
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
}

/**
 * [LoopPagerController.smoothScrollToPosition]的查找方向
 */
enum class LookupDirection {
    /**
     * 往开始方向查找
     */
    START,

    /**
     * 往结束方向查找
     */
    END
}

/**
 * [ViewPager2]的[Context]，等同于`ViewPager2.context`
 */
val LoopPagerController.context: Context
    get() = viewPager2.context

/**
 * [ViewPager2]的滚动状态，等同于`ViewPager2.scrollState`
 */
@ScrollState
val LoopPagerController.scrollState: Int
    get() = viewPager2.scrollState

/**
 * [ViewPager2]是否附加到window，等同于`ViewPager2.isAttachedToWindow`
 */
val LoopPagerController.isAttachedToWindow: Boolean
    get() = viewPager2.isAttachedToWindow

/**
 * 当前选择的`itemView`，当[scrollState]为[SCROLL_STATE_IDLE]时，可以跟[currentChildren]做对比
 */
val LoopPagerController.currentItemView: View?
    get() = viewPager2.recyclerView.findViewHolderForLayoutPosition(viewPager2.currentItem)?.itemView

/**
 * 当前`itemView`序列，当[scrollState]为[SCROLL_STATE_IDLE]时，可以跟[currentItemView]做对比
 */
val LoopPagerController.currentChildren: Sequence<View>
    get() = object : Sequence<View> {
        override fun iterator() = viewPager2.recyclerView.iterator()
    }

/**
 * 是否处理[ViewPager2]嵌套[ViewPager2]（LoopPager）的滚动冲突
 *
 * 1. 处理相同方向的滚动冲突，Child需要循环滚动，不允许Parent拦截触摸事件。
 * 2. 处理不同方向的滚动冲突，Parent拦截触摸事件的条件更严格，不会那么“灵敏”。
 */
var LoopPagerController.isVp2NestedScrollable: Boolean
    get() = viewPager2.getTag(R.id.tag_vp2_nested_scrollable) != null
    set(value) {
        val key = R.id.tag_vp2_nested_scrollable
        var listener = viewPager2.getTag(key) as? LoopPagerScrollableListener
        if (value && listener == null) {
            listener = LoopPagerScrollableListener()
            viewPager2.setTag(key, listener)
            viewPager2.recyclerView.addOnItemTouchListener(listener)
        } else if (!value && listener != null) {
            viewPager2.setTag(key, null)
            viewPager2.recyclerView.removeOnItemTouchListener(listener)
        }
    }

/**
 * 设置多个[PageTransformer]的简化函数
 */
fun LoopPagerController.setPageTransformer(vararg transformers: PageTransformer) {
    viewPager2.setPageTransformer(CompositePageTransformer()
        .apply { transformers.forEach(::addTransformer) })
}

fun LoopPagerController.getChildViewHolder(child: View): ViewHolder? {
    return viewPager2.recyclerView.getChildViewHolder(child)
}

fun LoopPagerController.addOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
    viewPager2.addOnAttachStateChangeListener(listener)
}

fun LoopPagerController.removeOnAttachStateChangeListener(listener: OnAttachStateChangeListener) {
    viewPager2.removeOnAttachStateChangeListener(listener)
}

private fun ViewGroup.iterator() = object : Iterator<View> {
    private var index = 0
    override fun hasNext() = index < childCount
    override fun next() = getChildAt(index++) ?: throw IndexOutOfBoundsException()
}