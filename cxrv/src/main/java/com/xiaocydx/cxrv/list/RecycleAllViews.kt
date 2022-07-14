@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.os.Parcelable
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData
import com.xiaocydx.cxrv.list.StaggeredGridLayoutManagerCompat

/**
 * 将[RecyclerView]的子View和离屏缓存回收进[RecycledViewPool]
 *
 * ### 增加回收上限
 * 当回收数量超过`viewType`的回收上限时，
 * 调用[canIncrease]判断是否允许增加回收上限，
 * 若允许增加，则调用[increaseMaxScrap]获取增加后的值。
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
 * @param canIncrease      是否允许增加`viewType`的回收上限。
 * @param increaseMaxScrap [canIncrease]返回`true`时，获取`viewType`增加后的回收上限。
 */
fun RecyclerView.recycleAllViews(
    canIncrease: CanIncrease = defaultCanIncrease,
    increaseMaxScrap: IncreaseMaxScrap
) {
    RecycleAllViewsRunner(this, canIncrease, increaseMaxScrap).run()
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
 * recyclerView.setRecycleAllViewsOnDetach(canIncrease, increaseMaxScrap)
 * ```
 *
 * @param canIncrease      是否允许增加`viewType`的回收上限。
 * @param increaseMaxScrap [canIncrease]返回`true`时，获取`viewType`增加后的回收上限。
 */
fun RecyclerView.setRecycleAllViewsOnDetach(
    canIncrease: CanIncrease = defaultCanIncrease,
    increaseMaxScrap: IncreaseMaxScrap
): View.OnAttachStateChangeListener = object : View.OnAttachStateChangeListener {
    private var pendingSavedState: Parcelable? = null

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
        rv.recycleAllViews(canIncrease, increaseMaxScrap)
    }
}.also(::addOnAttachStateChangeListener)

/**
 * 记录[RecyclerView]的初始状态，用于[CanIncrease]和[IncreaseMaxScrap]
 */
data class InitialState(val childCount: Int)

/**
 * [ViewHolder]回收进[RecycledViewPool]之前，是否允许增加`viewType`的回收上限
 */
typealias CanIncrease = (viewType: Int, currentMaxScrap: Int, initialState: InitialState) -> Boolean

/**
 * [ViewHolder]回收进[RecycledViewPool]之前，获取`viewType`增加后的回收上限
 */
typealias IncreaseMaxScrap = (viewType: Int, currentMaxScrap: Int, initialState: InitialState) -> Int

/**
 * [RecycledViewPool.DEFAULT_MAX_SCRAP]
 */
private const val DEFAULT_MAX_SCRAP = 5

/**
 * 若`currentMaxScrap`小于默认值[DEFAULT_MAX_SCRAP]，
 * 则说明回收上限被特意调小，一般表示该类型出现的较少，
 * 这种情况不需要增加回收上限，保持该类型原本的意图。
 */
private val defaultCanIncrease: CanIncrease = { _, currentMaxScrap, _ ->
    currentMaxScrap >= DEFAULT_MAX_SCRAP
}

private class RecycleAllViewsRunner(
    private val rv: RecyclerView,
    private val canIncrease: CanIncrease,
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
        val viewType = holder.itemViewType
        val scrapData = scrap[viewType] ?: return

        val currentMaxScrap = scrapData.mMaxScrap
        if (scrapData.mScrapHeap.size < currentMaxScrap
                || !canIncrease(viewType, currentMaxScrap, initialState)) {
            // 还可以继续回收，或者不允许增加maxScrap
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