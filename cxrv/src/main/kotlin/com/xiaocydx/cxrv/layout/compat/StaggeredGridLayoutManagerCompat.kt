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

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.layout.callback.CompositeLayoutManagerCallback

/**
 * 提供兼容属性的[StaggeredGridLayoutManager]
 *
 * @author xcc
 * @date 2022/8/11
 */
open class StaggeredGridLayoutManagerCompat : StaggeredGridLayoutManager, LayoutManagerCompat {
    private val scrollHelper = ScrollToFirstOnUpdateHelper()
    private val saveStateHelper = SaveInstanceStateOnDetachHelper()
    private val invalidateHelper = InvalidateItemDecorationsOnUpdateHelper()
    private val dispatcher = CompositeLayoutManagerCallback(initialCapacity = 3)

    constructor(
        spanCount: Int,
        @Orientation orientation: Int
    ) : super(spanCount, orientation)

    constructor(
        context: Context,
        attrs: AttributeSet,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    init {
        dispatcher.addLayoutManagerCallback(scrollHelper)
        dispatcher.addLayoutManagerCallback(saveStateHelper)
        dispatcher.addLayoutManagerCallback(invalidateHelper)
    }

    /**
     * 是否启用兼容（默认启用）：
     * 往列表首位插入或移动item时，若当前首位完全可见，则滚动到更新后的首位。
     */
    var isScrollToFirstOnUpdate: Boolean
        get() = scrollHelper.isEnabled
        set(value) {
            scrollHelper.isEnabled = value
        }

    /**
     * 是否启用兼容（默认不启用）：
     * 在[onDetachedFromWindow]时保存[LayoutManager]的状态，
     * 在[onAttachedToWindow]时恢复[LayoutManager]的状态。
     *
     * 由于`super.onDetachedFromWindow(view, recycler)`会执行清除逻辑，
     * 因此需要在其之前保存状态，确保`ViewPager2`嵌套[RecyclerView]这些场景，
     * [RecyclerView]的滚动位置能够被正确恢复。
     */
    var isSaveStateOnDetach: Boolean
        get() = saveStateHelper.isEnabled
        set(value) {
            saveStateHelper.isEnabled = value
        }

    /**
     * 是否启用兼容（默认启用）：
     * 列表更新时调用[RecyclerView.invalidateItemDecorations]，
     * 解决[ItemDecoration.getItemOffsets]调用不完整，导致实现复杂的问题。
     */
    var isInvalidateItemDecorationsOnUpdate: Boolean
        get() = invalidateHelper.isEnabled
        set(value) {
            invalidateHelper.isEnabled = value
        }

    @CallSuper
    override fun hasGapsToFix(): View? {
        var hasGapsToFix = super.hasGapsToFix()
        if (hasGapsToFix == null) {
            // 修复局部更新后，首位item布局位置错误的问题
            val child = findViewByPosition(0)
            val lp = child?.layoutParams as? LayoutParams
            if (lp != null && lp.spanIndex != 0) hasGapsToFix = child
        }
        return hasGapsToFix
    }

    @CallSuper
    override fun setRecyclerView(recyclerView: RecyclerView?) {
        super.setRecyclerView(recyclerView)
        if (recyclerView == null) dispatcher.onCleared()
    }

    @CallSuper
    override fun onAttachedToWindow(view: RecyclerView) {
        dispatcher.onPreAttachedToWindow(view)
        super.onAttachedToWindow(view)
    }

    @CallSuper
    override fun onDetachedFromWindow(view: RecyclerView, recycler: Recycler) {
        dispatcher.onPreDetachedFromWindow(view, recycler)
        super.onDetachedFromWindow(view, recycler)
    }

    @CallSuper
    override fun onAdapterChanged(oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        dispatcher.onPreAdapterChanged(layout = this, oldAdapter, newAdapter)
        super.onAdapterChanged(oldAdapter, newAdapter)
    }

    @CallSuper
    override fun onLayoutChildren(recycler: Recycler, state: State) {
        dispatcher.onPreLayoutChildren(recycler, state)
        super.onLayoutChildren(recycler, state)
    }

    @CallSuper
    override fun requestSimpleAnimationsInNextLayout() {
        dispatcher.preRequestSimpleAnimationsInNextLayout()
        super.requestSimpleAnimationsInNextLayout()
    }

    @CallSuper
    override fun calculateItemDecorationsForChild(child: View, outRect: Rect) {
        super.calculateItemDecorationsForChild(child, outRect)
        dispatcher.postCalculateItemDecorationsForChild(child, outRect)
    }

    @CallSuper
    override fun onItemsChanged(recyclerView: RecyclerView) {
        dispatcher.onPreItemsChanged(recyclerView)
        super.onItemsChanged(recyclerView)
    }

    @CallSuper
    override fun onItemsAdded(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        dispatcher.onPreItemsAdded(recyclerView, positionStart, itemCount)
        super.onItemsAdded(recyclerView, positionStart, itemCount)
    }

    @CallSuper
    override fun onItemsRemoved(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        dispatcher.onPreItemsRemoved(recyclerView, positionStart, itemCount)
        super.onItemsRemoved(recyclerView, positionStart, itemCount)
    }

    @CallSuper
    override fun onItemsUpdated(recyclerView: RecyclerView, positionStart: Int, itemCount: Int) {
        dispatcher.onPreItemsUpdated(recyclerView, positionStart, itemCount)
        super.onItemsUpdated(recyclerView, positionStart, itemCount)
    }

    @CallSuper
    override fun onItemsUpdated(recyclerView: RecyclerView, positionStart: Int, itemCount: Int, payload: Any?) {
        dispatcher.onPreItemsUpdated(recyclerView, positionStart, itemCount, payload)
        super.onItemsUpdated(recyclerView, positionStart, itemCount, payload)
    }

    @CallSuper
    override fun onItemsMoved(recyclerView: RecyclerView, from: Int, to: Int, itemCount: Int) {
        dispatcher.onPreItemsMoved(recyclerView, from, to, itemCount)
        super.onItemsMoved(recyclerView, from, to, itemCount)
    }

    @CallSuper
    override fun onLayoutCompleted(state: State) {
        dispatcher.onPreLayoutCompleted(layout = this, state)
        super.onLayoutCompleted(state)
    }
}