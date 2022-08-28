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
    private val view: RecyclerView?
        get() = layout?.mRecyclerView
    private val isStaggered: Boolean
        get() = layout is StaggeredGridLayoutManager

    var isEnabled = true

    override fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        adapter?.unregisterAdapterDataObserver(this)
        adapter = newAdapter
        adapter?.registerAdapterDataObserver(this)
        this.layout = layout
        previousItemCount = layout.itemCount
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        if (!isEnabled || previousItemCount == 0 || !isStaggered) return
        invalidateItemDecorations()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (!isEnabled || previousItemCount == 0) return
        // TODO: 按最后item进行减少
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
        if (!isEnabled) return
        // TODO: 支持瀑布流滚动更新
        if (postponeInvalidate) {
            view?.doOnPreDraw { view?.invalidateItemDecorations() }
        }
        postponeInvalidate = false
        previousItemCount = state.itemCount
    }

    override fun onCleared() {
        adapter?.unregisterAdapterDataObserver(this)
        adapter = null
        layout = null
    }
}