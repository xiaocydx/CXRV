@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.doOnPreDraw
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
    private var postponeInvalidate = false
    private var adapter: Adapter<*>? = null
    private var layout: LayoutManager? = null
    private var checker: StaggeredScrolledChecker? = null
    private val view: RecyclerView?
        get() = layout?.mRecyclerView
    private val isStaggered: Boolean
        get() = layout is StaggeredGridLayoutManager

    var isEnabled = true

    override fun onAttachedToWindow(view: RecyclerView) {
        val layout = view.layoutManager ?: return
        onAdapterChanged(layout, oldAdapter = adapter, newAdapter = view.adapter)
        if (checker == null && isStaggered) {
            checker = StaggeredScrolledChecker(view).apply { attach() }
        }
    }

    override fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        if (adapter !== newAdapter) {
            adapter?.unregisterAdapterDataObserver(this)
            adapter = newAdapter
            adapter?.registerAdapterDataObserver(this)
        }
        this.layout = layout
        previousItemCount = layout.itemCount
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        if (!isEnabled || previousItemCount == 0 || !isStaggered) return
        invalidateItemDecorations()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (!isEnabled || previousItemCount == 0) return
        invalidateItemDecorations()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (!isEnabled || !isStaggered) return
        invalidateItemDecorations()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if (!isEnabled || !isStaggered) return
        invalidateItemDecorations()
    }

    private fun invalidateItemDecorations() {
        if (!isEnabled) return
        if (isStaggered) {
            postponeInvalidate = true
        } else {
            view?.invalidateItemDecorations()
        }
    }

    override fun onLayoutCompleted(layout: LayoutManager, state: State) {
        val count = view?.itemDecorationCount ?: 0
        if (isEnabled && postponeInvalidate && count > 0) {
            // 此时view.isComputingLayout为true，
            // 调用view.invalidateItemDecorations()会抛出异常，
            // 因此在view.doOnPreDraw()触发时调用，规避异常检测。
            view?.doOnPreDraw { view?.invalidateItemDecorations() }
        }
        postponeInvalidate = false
        previousItemCount = state.itemCount
    }

    override fun onCleared() {
        checker?.detach()
        adapter?.unregisterAdapterDataObserver(this)
        checker = null
        adapter = null
        layout = null
    }

    private inner class StaggeredScrolledChecker(
        private val view: RecyclerView
    ) : OnScrollListener() {

        fun attach() {
            view.addOnScrollListener(this)
        }

        fun detach() {
            view.removeOnScrollListener(this)
        }

        /**
         * [RecyclerView.markItemDecorInsetsDirty]的判断条件，
         * copy自[StaggeredGridLayoutManager.checkForGaps]。
         */
        override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
            assert(this.view === view)
            val lm = view.layoutManager
            if (!isEnabled || view.itemDecorationCount == 0 || lm !is StaggeredGridLayoutManager) return
            if (view.scrollState == SCROLL_STATE_IDLE) {
                // 兼容滚动到指定位置的场景，仍需找到更好的解决方案
                invalidateItemDecorations()
                return
            }
            val minPos = if (lm.mShouldReverseLayout) lm.lastChildPosition else lm.firstChildPosition
            if (minPos == 0 && lm.hasGapsToFix() != null) {
                view.markItemDecorInsetsDirty()
            }
        }
    }
}