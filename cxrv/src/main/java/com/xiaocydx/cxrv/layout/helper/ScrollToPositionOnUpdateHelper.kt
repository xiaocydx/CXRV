@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.itemvisible.isFirstItemCompletelyVisible
import com.xiaocydx.cxrv.layout.callback.LayoutManagerCallback

/**
 * @author xcc
 * @date 2022/8/12
 */
class ScrollToPositionOnUpdateHelper : AdapterDataObserver(), LayoutManagerCallback {
    private var previousItemCount = 0
    private var layout: LayoutManager? = null
    private val view: RecyclerView?
        get() = layout?.mRecyclerView

    override fun onAdapterChanged(layout: LayoutManager, oldAdapter: Adapter<*>?, newAdapter: Adapter<*>?) {
        oldAdapter?.unregisterAdapterDataObserver(this)
        newAdapter?.registerAdapterDataObserver(this)
        this.layout = layout
        previousItemCount = layout.itemCount
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (positionStart == 0
                && previousItemCount != 0
                && view?.isFirstItemCompletelyVisible == true) {
            // TODO: 2022/6/12 观察对StaggeredGridLayoutManager的影响
            view?.scrollToPosition(0)
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if ((fromPosition == 0 || toPosition == 0)
                && view?.isFirstItemCompletelyVisible == true) {
            view?.scrollToPosition(0)
        }
    }

    override fun onLayoutCompleted(layout: LayoutManager, state: State) {
        previousItemCount = state.itemCount
    }
}