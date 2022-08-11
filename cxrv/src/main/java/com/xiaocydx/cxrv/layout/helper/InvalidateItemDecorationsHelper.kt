@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.internal.doOnPreDraw
import com.xiaocydx.cxrv.layout.callback.LayoutManagerCallback

/**
 * @author xcc
 * @date 2022/8/11
 */
internal class InvalidateItemDecorationsHelper : AdapterDataObserver(), LayoutManagerCallback {
    private var previousItemCount = 0
    private var postponeInvalidate = false
    private var layout: LayoutManager? = null
    private val view: RecyclerView?
        get() = layout?.mRecyclerView
    private val isStaggered: Boolean
        get() = layout is StaggeredGridLayoutManager

    override fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        oldAdapter?.unregisterAdapterDataObserver(this)
        newAdapter?.registerAdapterDataObserver(this)
        this.layout = layout
        previousItemCount = layout.itemCount
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        if (previousItemCount == 0) return
        if (!isStaggered) return
        invalidateItemDecorations()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (previousItemCount == 0) return
        // FIXME: 怎么才能按最后item进行减少？
        invalidateItemDecorations()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (!isStaggered) return
        invalidateItemDecorations()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if (!isStaggered) return
        invalidateItemDecorations()
    }

    private fun invalidateItemDecorations() {
        if (isStaggered) {
            postponeInvalidate = true
        } else {
            view?.invalidateItemDecorations()
        }
    }

    override fun onLayoutCompleted(layout: LayoutManager, state: State) {
        // FIXME: 瀑布流的滚动怎么支持？
        if (postponeInvalidate) {
            view?.doOnPreDraw { view?.invalidateItemDecorations() }
        }
        postponeInvalidate = false
        previousItemCount = state.itemCount
    }
}