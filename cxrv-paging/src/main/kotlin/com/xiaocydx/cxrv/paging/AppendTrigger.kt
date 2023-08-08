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

package com.xiaocydx.cxrv.paging

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import androidx.recyclerview.widget.RecyclerView.OnChildAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.isPreLayout
import com.xiaocydx.cxrv.internal.PreDrawListener
import com.xiaocydx.cxrv.internal.hasDisplayItem
import com.xiaocydx.cxrv.internal.log
import com.xiaocydx.cxrv.list.AdapterAttachCallback
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.ListChangedListener
import com.xiaocydx.cxrv.list.ViewHolderListener
import com.xiaocydx.cxrv.list.adapter
import kotlin.math.max
import kotlin.math.min

/**
 * [PagingSource]的末尾加载触发器
 *
 * ### [preAppend]
 * 刷新加载结果可能和之前第一页结果一致，此时`onBindViewHolder`不会被调用，
 * 也就不会尝试触发末尾加载，因此调用[preAppend]推迟尝试触发末尾加载。
 *
 * ### [append]
 * 调用[append]时，通过[removePreAppend]移除[preAppend]，
 * 表示末尾加载已触发，不用靠[preAppend]推迟尝试触发末尾加载。
 *
 * ### 刷新流程执行时序
 * ```
 *  +----------------+        +-----------------+        +---------------+
 *  | RefreshSuccess | -----> |    preAppend    | -----> | AppendLoading |
 *  +----------------+        +-----------------+        +---------------+
 *          |                        ∧
 *          |                        | remove
 *          V                        |
 *  +----------------+        +-----------------+        +---------------+
 *  |     append     | -----> | removePreAppend | -----> | AppendLoading |
 *  +----------------+        +-----------------+        +---------------+
 * ```
 * @author xcc
 * @date 2021/9/19
 */
internal class AppendTrigger(
    val prefetchEnabled: Boolean,
    val prefetchItemCount: Int,
    private val adapter: ListAdapter<*, *>,
    private val collector: PagingCollector<*>,
) : AdapterAttachCallback {
    private var rv: RecyclerView? = null
    private var retryListener: AppendRetryListener? = null
    private var bindListener: AppendBindListener? = null
    private var scrollListener: AppendScrollListener? = null
    private var preDrawListener: AppendPreDrawListener? = null
    private var stateListener: AppendStateListener? = null
    private var appendRequested = false
    private val loadStates: LoadStates
        get() = collector.loadStates

    fun attach() {
        bindListener = if (prefetchEnabled) AppendBindListener() else null
        stateListener = AppendStateListener()
        adapter.addAdapterAttachCallback(this)
    }

    fun detach() {
        bindListener?.removeListener()
        stateListener?.removeListener()
        bindListener = null
        stateListener = null
        rv?.let(::onDetachedFromRecyclerView)
        adapter.removeAdapterAttachCallback(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.rv = recyclerView
        retryListener = AppendRetryListener(recyclerView)
        scrollListener = AppendScrollListener(recyclerView)
        preDrawListener = AppendPreDrawListener(recyclerView)
        retryListener?.isEnabled = true
        scrollListener?.isEnabled = !prefetchEnabled
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        this.rv = null
        retryListener?.removeListener()
        scrollListener?.removeListener()
        preDrawListener?.removeListener()
        retryListener = null
        scrollListener = null
        preDrawListener = null
    }

    private fun preAppend() {
        scrollListener?.isEnabled = true
        preDrawListener?.isEnabled = true
    }

    private fun removePreAppend() {
        scrollListener?.isEnabled = !prefetchEnabled
        preDrawListener?.isEnabled = false
    }

    private fun append() {
        removePreAppend()
        if (!appendRequested) {
            // 为了支持prefetchItemCount而添加的属性，
            // 用于拦截collector.append()的冗余调用。
            appendRequested = true
            collector.append()
        }
    }

    /**
     * 若指定的item可视，则触发末尾加载
     *
     * 该函数的计算逻辑基于`layoutPosition`而不是`bindingAdapterPosition`，因此会存在一定偏差，
     * 例如最后一个item可能是loadFooter或者[adapter]的最后一个item，不过对替补方案而言已经足够。
     */
    private fun appendIfTargetItemVisible(from: String) {
        // 当append.isFailure = true时，由AppendRetryListener处理
        if (!loadStates.isAllowAppend || loadStates.append.isFailure) return
        if (!adapter.hasDisplayItem) {
            log { "$from trigger append, displayItem = 0" }
            append()
            return
        }

        val rv = rv
        val lm = rv?.layoutManager ?: return
        // 不能用layoutManager.findXXXVisibleItemPosition()这类查找函数，
        // 具体原因可看LayoutManager.enableBoundCheckCompat()的注释说明。
        var minPosition = 0
        var maxPosition = NO_POSITION
        for (index in 0 until lm.childCount) {
            // rv.childCount包含正在运行remove动画的子View，不需要这些子View参与判断
            val holder = lm.getChildAt(index)?.let(rv::getChildViewHolder) ?: continue
            minPosition = min(holder.layoutPosition, minPosition)
            maxPosition = max(holder.layoutPosition, maxPosition)
        }

        val endPosition = lm.itemCount - 1
        val startPosition = (endPosition - prefetchItemCount).coerceAtLeast(0)
        val layoutPosition = when {
            startPosition in minPosition..maxPosition -> startPosition
            endPosition in minPosition..maxPosition -> endPosition
            startPosition < minPosition && endPosition > maxPosition -> Int.MIN_VALUE
            else -> NO_POSITION
        }
        if (layoutPosition != NO_POSITION) {
            if (layoutPosition == Int.MIN_VALUE) {
                log { "$from trigger append" }
            } else {
                log { "$from trigger append, layoutPosition = $layoutPosition" }
            }
            append()
        }
    }

    /**
     * 末尾加载失败后，若最后一个item再次可视，则触发末尾加载，
     * 最后一个item可能是loadFooter或者[adapter]的最后一个item。
     */
    private inner class AppendRetryListener(
        private val rv: RecyclerView
    ) : OnChildAttachStateChangeListener {
        var isEnabled = false

        init {
            rv.addOnChildAttachStateChangeListener(this)
        }

        override fun onChildViewAttachedToWindow(view: View) {
            if (!isEnabled || !loadStates.append.isFailure) return
            if (rv.scrollState == SCROLL_STATE_IDLE) return
            val lm = rv.layoutManager ?: return
            val holder = rv.getChildViewHolder(view) ?: return
            if (holder.layoutPosition != lm.itemCount - 1) return
            log { "AppendRetryListener trigger append, layoutPosition = ${holder.layoutPosition}" }
            append()
        }

        override fun onChildViewDetachedFromWindow(view: View) = Unit

        fun removeListener() {
            rv.removeOnChildAttachStateChangeListener(this)
        }
    }

    /**
     * 在[onBindViewHolder]被调用时尝试触发末尾加载
     */
    private inner class AppendBindListener : ViewHolderListener<ViewHolder> {

        init {
            adapter.addViewHolderListener(this)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
            // 当append.isFailure = true时，由AppendRetryListener处理
            if (!loadStates.isAllowAppend || loadStates.append.isFailure) return
            val endPosition = adapter.itemCount - 1
            val startPosition = (endPosition - prefetchItemCount).coerceAtLeast(0)
            if (position !in startPosition..endPosition) return

            if (rv?.isPreLayout == true) {
                // 此时处于preLayout，填充子View时调用了onBindViewHolder()，
                // 例如列表全选功能，调用notifyItemRangeChanged()做全选更新，
                // 这种情况不启用滚动绑定触发方案，而是启用滚动监听触发方案。
                preAppend()
            } else {
                log { "AppendBindListener trigger append, bindingAdapterPosition = $position" }
                append()
            }
        }

        fun removeListener() {
            adapter.removeViewHolderListener(this)
        }
    }

    /**
     * [AppendBindListener]的替补方案，在[onScrolled]被调用时尝试触发末尾加载
     */
    private inner class AppendScrollListener(private val rv: RecyclerView) : OnScrollListener() {
        var isEnabled = false

        init {
            rv.addOnScrollListener(this)
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (isEnabled) appendIfTargetItemVisible(from = "AppendScrollListener")
        }

        fun removeListener() {
            rv.removeOnScrollListener(this)
        }
    }

    /**
     * [AppendScrollListener]的替补方案，在[onPreDraw]被调用时尝试触发末尾加载
     */
    private inner class AppendPreDrawListener(rv: RecyclerView) : PreDrawListener(rv) {
        var isEnabled = false

        override fun onPreDraw(): Boolean {
            if (isEnabled) appendIfTargetItemVisible(from = "AppendPreDrawListener")
            return super.onPreDraw()
        }

        /**
         * ViewPager2滚动时会将嵌套的RecyclerView从Window上分离，
         * 保存在离屏缓存中，滚动回来时触发该函数，此时数据可能发生变化，
         * 需要尝试触发末尾加载。
         */
        override fun onViewAttachedToWindow(view: View) {
            super.onViewAttachedToWindow(view)
            preAppend()
        }
    }

    /**
     * 在列表状态和分页加载状态更改时调整末尾加载的启用方案
     */
    private inner class AppendStateListener :
            HandleEventListener<Any>, ListChangedListener<Any>, LoadStatesListener {
        private var forceAppend = false

        init {
            adapter.addListChangedListener(this)
            collector.addLoadStatesListener(this)
            collector.addHandleEventListener(this)
        }

        override suspend fun handleEvent(rv: RecyclerView, event: PagingEvent<Any>) {
            // PagingFetcher的加载过程已确保末尾加载data不为空，
            // 若仍然收到data为空的事件，则表示操作符过滤了data，
            // 对于这种情况，强制触发末尾加载。
            forceAppend = event.loadType == LoadType.APPEND
                    && event is PagingEvent.LoadDataSuccess
                    && event.data.isEmpty()
        }

        /**
         * [onListChanged]在[onLoadStatesChanged]之前被调用，
         * 当刷新加载成功时，`lm.itemCount`已更新，`loadStates.refresh`未更新，
         * 因此不会调用[preAppend]，由[onLoadStatesChanged]主动触发末尾加载。
         */
        override fun onListChanged(current: List<Any>) {
            val lm = rv?.layoutManager ?: return
            if (loadStates.isAllowAppend && lm.itemCount < lm.childCount) {
                preAppend()
            }
        }

        override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
            if (appendRequested && previous.append != current.append) {
                appendRequested = false
            }

            val forceAppend = forceAppend.also { forceAppend = false }
            if (forceAppend && loadStates.isAllowAppend) {
                log { "AppendStateListener force trigger append, loadStates = $loadStates" }
                append()
                return
            }

            if (previous.refreshToSuccess(current)
                    || (previous.refresh.isIncomplete && current.refresh.isSuccess)) {
                // 1. previous.refreshToSuccess(current)
                // 当刷新加载成功时，加载结果可能和之前第一页结果一致，
                // 此时AppendBindListener.onBindViewHolder()不会被调用，
                // 因此需要主动触发末尾加载。
                //
                // 2. previous.refresh.isIncomplete && current.refresh.isSuccess
                // refresh从Incomplete直接流转至Success，可能是RecyclerView的重建恢复流程。
                preAppend()
            }
        }

        fun removeListener() {
            forceAppend = false
            adapter.removeListChangedListener(this)
            collector.removeLoadStatesListener(this)
            collector.removeHandleEventListener(this)
        }
    }
}