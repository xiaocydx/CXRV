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

@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.layout.callback.LayoutManagerCallback

/**
 * 列表更新时调用[RecyclerView.invalidateItemDecorations]，
 * 解决[ItemDecoration.getItemOffsets]调用不完整，导致实现复杂的问题。
 *
 * @author xcc
 * @date 2022/8/11
 */
internal class InvalidateItemDecorationsOnUpdateHelper : AdapterDataObserver(), LayoutManagerCallback {
    private var previousItemCount = 0
    private var invalidateOnNextLayout = false
    private var adapter: Adapter<*>? = null
    private var layout: LayoutManager? = null
    private val staggeredCompat = StaggeredCompat()
    private val view: RecyclerView?
        get() = layout?.mRecyclerView

    var isEnabled = true

    override fun onPreAttachedToWindow(view: RecyclerView) {
        val layout = view.layoutManager ?: return
        onPreAdapterChanged(layout, oldAdapter = adapter, newAdapter = view.adapter)
    }

    override fun onPreAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        if (adapter !== newAdapter) {
            adapter?.unregisterAdapterDataObserver(this)
            adapter = newAdapter
            adapter?.registerAdapterDataObserver(this)
        }
        this.layout = layout
        previousItemCount = layout.itemCount
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        requestInvalidateOnNextLayout()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
        requestInvalidateOnNextLayout()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (previousItemCount == 0) return
        // 兼容插入item的场景，例如分页加载下一页的场景
        requestInvalidateOnNextLayout()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        requestInvalidateOnNextLayout()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        requestInvalidateOnNextLayout()
    }

    override fun preRequestSimpleAnimationsInNextLayout() {
        staggeredCompat.preRequestSimpleAnimationsInNextLayout()
    }

    override fun onPreLayoutChildren(recycler: Recycler, state: State) {
        staggeredCompat.onPreLayoutChildren(state)
        if (isEnabled && invalidateOnNextLayout && !state.isPreLayout) {
            // preLayout阶段不重新计算间距，确保preLayout的布局结果不影响realLayout
            markItemDecorInsetsDirty()
        }
    }

    override fun postCalculateItemDecorationsForChild(child: View, outRect: Rect) {
        staggeredCompat.postCalculateItemDecorationsForChild(child)
    }

    private fun markItemDecorInsetsDirty() {
        val rv = view ?: return
        if (rv.itemDecorationCount == 0) return
        rv.markItemDecorInsetsDirty()
    }

    private fun requestInvalidateOnNextLayout() {
        invalidateOnNextLayout = true
    }

    override fun onPreLayoutCompleted(layout: LayoutManager, state: State) {
        invalidateOnNextLayout = false
        previousItemCount = state.itemCount
    }

    override fun onCleared() {
        adapter?.unregisterAdapterDataObserver(this)
        adapter = null
        layout = null
    }

    private inner class StaggeredCompat {
        private val isStaggered: Boolean
            get() = layout is StaggeredGridLayoutManager

        fun preRequestSimpleAnimationsInNextLayout() {
            if (!isEnabled || !isStaggered || invalidateOnNextLayout) return
            // 兼容调用StaggeredGridLayoutManager.onLayoutChildren(recycler, state, false)的场景
            markItemDecorInsetsDirty()
        }

        fun onPreLayoutChildren(state: State) {
            if (!isEnabled || !isStaggered || invalidateOnNextLayout) return
            if (state.willRunSimpleAnimations()) {
                // 兼容StaggeredGridLayoutManager强制执行简单动画的场景，
                // 例如滚动过程或者滚动状态更改为IDLE需要重新对齐的场景。
                requestInvalidateOnNextLayout()
                return
            }
            // 只兼容StaggeredGridLayoutManager滚动到指定位置的场景，其它重新计算锚点的场景需要靠反射兼容
            val lm = layout as? StaggeredGridLayoutManager ?: return
            val recalculateAnchor = state.itemCount > 0 && lm.mPendingScrollPosition != NO_POSITION
            if (recalculateAnchor) requestInvalidateOnNextLayout()
        }

        fun postCalculateItemDecorationsForChild(child: View) {
            if (!isEnabled || !isStaggered || !invalidateOnNextLayout) return
            if (view == null || view!!.isPreLayout) return
            // 在onLayoutChildren()布局期间，计算完Insets就废弃，
            // 确保layoutParams.mSpan参数正确后，再次计算Insets。
            (child.layoutParams as? LayoutParams)?.mInsetsDirty = true
        }
    }
}