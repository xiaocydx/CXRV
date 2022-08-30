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
    private var checker: ScrolledChecker? = null
    private val view: RecyclerView?
        get() = layout?.mRecyclerView
    private val isStaggered: Boolean
        get() = layout is StaggeredGridLayoutManager

    var isEnabled = true

    override fun onAttachedToWindow(view: RecyclerView) {
        val layout = view.layoutManager ?: return
        onAdapterChanged(layout, oldAdapter = adapter, newAdapter = view.adapter)
        if (checker == null && ScrolledChecker.support(layout)) {
            checker = ScrolledChecker(view, ::isEnabled).apply { attach() }
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

    private class ScrolledChecker(
        private val view: RecyclerView,
        private val isEnabled: () -> Boolean
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
            if (!isEnabled() || view.scrollState == SCROLL_STATE_IDLE) return
            val lm = view.layoutManager as? StaggeredGridLayoutManager ?: return
            val minPos = if (lm.mShouldReverseLayout) lm.lastChildPosition else lm.firstChildPosition
            if (minPos == 0 && lm.hasGapsToFix() != null) {
                view.markItemDecorInsetsDirty()
            }
        }

        companion object {
            fun support(layout: LayoutManager): Boolean {
                return layout is StaggeredGridLayoutManager
            }
        }
    }
}