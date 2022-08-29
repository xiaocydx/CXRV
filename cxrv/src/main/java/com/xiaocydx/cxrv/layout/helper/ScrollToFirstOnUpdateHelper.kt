@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.itemvisible.isFirstItemCompletelyVisible
import com.xiaocydx.cxrv.layout.callback.LayoutManagerCallback

/**
 * 往列表首位插入或交换item时，若当前首位完全可见，则滚动到更新后的首位
 *
 * @author xcc
 * @date 2022/8/12
 */
internal class ScrollToFirstOnUpdateHelper : AdapterDataObserver(), LayoutManagerCallback {
    private var previousItemCount = 0
    private var adapter: Adapter<*>? = null
    private var layout: LayoutManager? = null
    private val view: RecyclerView?
        get() = layout?.mRecyclerView

    var isEnabled = true

    override fun onAttachedToWindow(view: RecyclerView) {
        val layout = view.layoutManager ?: return
        onAdapterChanged(layout, oldAdapter = adapter, newAdapter = view.adapter)
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

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (isEnabled
                && positionStart == 0
                && previousItemCount != 0
                && view?.isFirstItemCompletelyVisible == true) {
            view?.scrollToPosition(0)
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if (isEnabled
                && (fromPosition == 0 || toPosition == 0)
                && view?.isFirstItemCompletelyVisible == true) {
            view?.scrollToPosition(0)
        }
    }

    override fun onLayoutCompleted(layout: LayoutManager, state: State) {
        previousItemCount = state.itemCount
    }

    override fun onCleared() {
        adapter?.unregisterAdapterDataObserver(this)
        adapter = null
        layout = null
    }
}