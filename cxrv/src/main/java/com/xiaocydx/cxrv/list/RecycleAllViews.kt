@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.os.Parcelable
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.StaggeredGridLayoutManagerCompat

/**
 * 将[RecyclerView]的子View和离屏缓存回收进[RecycledViewPool]
 *
 * ### 增加回收上限
 * 当回收数量超过`viewType`的回收上限时，
 * 调用[increaseMaxScrap]获取增加后的回收上限。
 *
 * ### 恢复回收上限
 * 若在回收过程中，调用了[increaseMaxScrap]并设置了增加后的回收上限，
 * 则在回收完成后，会恢复`viewType`原本的回收上限，避免后续缓存堆积。
 *
 * ### 确保执行回调
 * 该函数确保执行回收流程相关的回调，回调包括不限于：
 * 1. [Adapter.onViewRecycled]
 * 2. [Adapter.onViewDetachedFromWindow]
 * 3. [RecyclerListener.onViewRecycled]
 * 4. [OnChildAttachStateChangeListener.onChildViewDetachedFromWindow]
 *
 * @param increaseMaxScrap 获取`viewType`增加后的回收上限。
 */
fun RecyclerView.recycleAllViews(increaseMaxScrap: IncreaseMaxScrap) {
    RecycleAllViewsRunner(this, increaseMaxScrap).run()
}

/**
 * 当[RecyclerView]从Window上分离时：
 * 1. 保存[LayoutManager]的状态，当再次添加到Window上或者重建时，能够恢复滚动位置。
 * 2. 调用[recycleAllViews]，将子View和离屏缓存回收进[RecycledViewPool]。
 *
 * **注意**：由于[StaggeredGridLayoutManager.onDetachedFromWindow]的清除逻辑，
 * 比[View.OnAttachStateChangeListener.onViewDetachedFromWindow]先执行，
 * 因此导致该函数无法保存正确的状态，恢复的滚动位置带有一定程度的偏移。
 * 此时需要搭配[StaggeredGridLayoutManagerCompat.isSaveStateOnDetach]使用：
 * ```
 * recyclerView.staggered(spanCount) { isSaveStateOnDetach = true }
 * recyclerView.setRecycleAllViewsOnDetach(increaseMaxScrap)
 * ```
 *
 * @param increaseMaxScrap 获取`viewType`增加后的回收上限。
 * @return 调用[Disposable.dispose]可以移除设置的视图回收处理。
 */
fun RecyclerView.setRecycleAllViewsOnDetach(increaseMaxScrap: IncreaseMaxScrap): Disposable {
    return RecycleAllViewsOnDetachDisposable().attach(this, increaseMaxScrap)
}

/**
 * 记录[RecyclerView]的初始状态，用于[IncreaseMaxScrap]
 */
data class InitialState(val childCount: Int)

/**
 * 回收数量超过`viewType`的回收上限，获取`viewType`增加后的回收上限
 */
typealias IncreaseMaxScrap = (viewType: Int, currentMaxScrap: Int, initialState: InitialState) -> Int

private class RecycleAllViewsRunner(
    private val rv: RecyclerView,
    private val increaseMaxScrap: IncreaseMaxScrap
) : RecyclerListener {
    private var state: Any? = null
    private val initialState = InitialState(rv.childCount)
    private val scrap: SparseArray<ScrapData> = rv.recycledViewPool.mScrap

    /**
     * [ViewHolder]在被回收进[RecycledViewPool]之前，
     * 会先执行[onViewRecycled]，尝试增加`viewType`的回收上限，
     * 在回收完成后，恢复`viewType`原本的回收上限，避免后续缓存堆积。
     */
    fun run() {
        val recycler = rv.mRecycler
        rv.addRecyclerListener(this)
        rv.layoutManager?.removeAndRecycleAllViews(recycler)
        recycler.clear()
        rv.removeRecyclerListener(this)
        restoreMaxScrap()
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clearReference()
        val viewType = holder.itemViewType
        val scrapData = scrap[viewType] ?: return

        val currentMaxScrap = scrapData.mMaxScrap
        if (scrapData.mScrapHeap.size < currentMaxScrap) {
            // 还可以继续回收
            return
        }

        val newMaxScrap = increaseMaxScrap(viewType, currentMaxScrap, initialState)
        if (currentMaxScrap >= newMaxScrap) {
            return
        }

        if (!containsMaxScrap(viewType)) {
            // 同一种viewType只会进入该分支逻辑一次
            saveMaxScrap(viewType, currentMaxScrap)
        }
        scrapData.mMaxScrap = newMaxScrap
    }

    private fun ViewHolder.clearReference() {
        val lp = itemView.layoutParams
        if (lp !is StaggeredGridLayoutManager.LayoutParams) return
        // chain：StaggeredGridLayoutManager.mRecyclerView -> Span
        lp.mSpan = null
    }

    private fun containsMaxScrap(viewType: Int): Boolean {
        return when (val state = state) {
            is Pair -> state.viewType == viewType
            is SparseIntArray -> state[viewType] != 0
            else -> false
        }
    }

    private fun saveMaxScrap(viewType: Int, maxScrap: Int) {
        when (val state = state) {
            null -> this.state = Pair(viewType, maxScrap)
            is Pair -> SparseIntArray().also {
                this.state = it
                it.put(state.viewType, state.maxScrap)
                it.put(viewType, maxScrap)
            }
            is SparseIntArray -> state.put(viewType, maxScrap)
        }
    }

    private fun restoreMaxScrap() {
        when (val state = state) {
            is Pair -> restoreMaxScrap(state.viewType, state.maxScrap)
            is SparseIntArray -> {
                for (index in 0 until state.size()) {
                    val viewType = state.keyAt(index)
                    val maxScrap = state.valueAt(index)
                    restoreMaxScrap(viewType, maxScrap)
                }
            }
        }
    }

    private fun restoreMaxScrap(viewType: Int, maxScrap: Int) {
        scrap[viewType]?.mMaxScrap = maxScrap
    }

    private class Pair(val viewType: Int, val maxScrap: Int)
}

private class RecycleAllViewsOnDetachDisposable : OnAttachStateChangeListener, Disposable {
    private var rv: RecyclerView? = null
    private var increaseMaxScrap: IncreaseMaxScrap? = null
    private var pendingSavedState: Parcelable? = null
    override val isDisposed: Boolean
        get() = rv == null && increaseMaxScrap == null

    fun attach(
        rv: RecyclerView,
        increaseMaxScrap: IncreaseMaxScrap
    ): Disposable {
        this.rv = rv
        this.increaseMaxScrap = increaseMaxScrap
        rv.addOnAttachStateChangeListener(this)
        return this
    }

    override fun onViewAttachedToWindow(view: View) {
        if (pendingSavedState != null) {
            (view as? RecyclerView)?.layoutManager
                ?.onRestoreInstanceState(pendingSavedState)
        }
        pendingSavedState = null
    }

    override fun onViewDetachedFromWindow(view: View) {
        val rv = view as? RecyclerView ?: return
        val lm = rv.layoutManager ?: return
        pendingSavedState = lm.onSaveInstanceState()
        // 这是一种取巧的做法，对LayoutManager实现类的mPendingSavedState赋值，
        // 确保Fragment销毁时能保存状态，Fragment重建时恢复RecyclerView的滚动位置。
        pendingSavedState?.let(lm::onRestoreInstanceState)
        increaseMaxScrap?.let(rv::recycleAllViews)
    }

    override fun dispose() {
        rv?.removeOnAttachStateChangeListener(this)
        rv = null
        increaseMaxScrap = null
        pendingSavedState = null
    }
}