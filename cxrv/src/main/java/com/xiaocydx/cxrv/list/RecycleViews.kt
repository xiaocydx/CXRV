@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.util.SparseArray
import android.util.SparseIntArray
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData

/**
 * 在视图控制器销毁阶段，将子View和`Scrap`回收进[RecycledViewPool]
 *
 * ### 增加回收上限
 * 当回收数量超过viewType对应的`maxScrap`时，
 * 调用[canIncrease]判断是否允许增加`maxScrap`，
 * 若允许增加，则调用[increaseMaxScrap]获取增加后的值。
 *
 * ### 恢复回收上限
 * 若在回收过程中，调用了[increaseMaxScrap]并设为`maxScrap`，
 * 则在回收完成后，会恢复viewType原本的`maxScrap`。
 *
 * ### 确保执行回调
 * 该函数确保执行正常回收流程相关的回调，回调包括不限于：
 * 1. [Adapter.onViewRecycled]
 * 2. [Adapter.onViewDetachedFromWindow]
 * 3. [RecyclerListener.onViewRecycled]
 * 4. [OnChildAttachStateChangeListener.onChildViewDetachedFromWindow]
 */
fun RecyclerView.destroyRecycleViews(
    canIncrease: (viewType: Int, maxScrap: Int) -> Boolean = defaultCanIncrease,
    increaseMaxScrap: (viewType: Int, maxScrap: Int) -> Int
) {
    // ViewHolder在被回收进RecycledViewPool之前，
    // 会先执行controller.onViewRecycled()，尝试增加maxScrap。
    val controller = MaxScrapController(recycledViewPool, canIncrease, increaseMaxScrap)
    addRecyclerListener(controller)

    // 由于此时是视图控制器销毁阶段，因此调用startInterceptRequestLayout()，
    // 避免removeAndRecycleViews()移除子View时，执行不必要的requestLayout()。
    startInterceptRequestLayout()
    removeAndRecycleViews()

    // 恢复viewType原本的maxScrap，避免后续堆积不必要的缓存
    controller.restoreMaxScrap()
}

/**
 * [RecycledViewPool.DEFAULT_MAX_SCRAP]
 */
private const val DEFAULT_MAX_SCRAP = 5

private val defaultCanIncrease: (Int, Int) -> Boolean = { _, maxScrap ->
    // 若maxScrap小于默认值DEFAULT_MAX_SCRAP，
    // 则说明maxScrap被特意调小，一般表示该类型出现的较少，
    // 这种情况不需要增加maxScrap，保持该类型原本的意图。
    maxScrap >= DEFAULT_MAX_SCRAP
}

private class MaxScrapController(
    recycledViewPool: RecycledViewPool,
    private val canIncrease: (viewType: Int, maxScrap: Int) -> Boolean,
    private val increaseMaxScrap: (viewType: Int, maxScrap: Int) -> Int
) : RecyclerListener {
    private var state: Any? = null
    private val scrap: SparseArray<ScrapData> = recycledViewPool.mScrap

    override fun onViewRecycled(holder: ViewHolder) {
        val viewType = holder.itemViewType
        val scrapData = scrap[viewType] ?: return

        val maxScrap = scrapData.mMaxScrap
        if (scrapData.mScrapHeap.size < maxScrap
                || !canIncrease(viewType, maxScrap)) {
            // 还可以继续回收，或者不允许增加maxScrap
            return
        }

        val newValue = increaseMaxScrap(viewType, maxScrap)
        if (maxScrap >= newValue) {
            return
        }

        if (!containsMaxScrap(viewType)) {
            // 同一个viewType只会进入该分支逻辑一次
            saveMaxScrap(viewType, maxScrap)
        }
        scrapData.mMaxScrap = newValue
    }

    fun restoreMaxScrap() {
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

    private fun restoreMaxScrap(viewType: Int, maxScrap: Int) {
        scrap[viewType]?.mMaxScrap = maxScrap
    }

    private class Pair(val viewType: Int, val maxScrap: Int)
}