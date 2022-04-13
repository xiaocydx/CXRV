@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.util.SparseArray
import android.util.SparseIntArray
import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData

/**
 * 在视图控制器销毁阶段，将子View和`Scrap`回收进[RecycledViewPool]
 *
 * [maxScrap]表示回收过程中，每个`viewType`的回收上限，
 * 在回收完成后，会将`viewType`的回收上限恢复为原本的大小。
 *
 * 该函数的实现确保会执行正常回收流程相关的回调，回调包括不限于：
 * 1. [Adapter.onViewRecycled]
 * 2. [Adapter.onViewDetachedFromWindow]
 * 3. [RecyclerListener.onViewRecycled]
 * 4. [OnChildAttachStateChangeListener.onChildViewDetachedFromWindow]
 */
fun RecyclerView.destroyRecycleViews(maxScrap: (viewType: Int) -> Int) {
    // ViewHolder在被回收进RecycledViewPool之前，
    // 会先执行controller.onViewRecycled()，
    // 此时controller会调整viewType的回收上限。
    val controller = MaxScrapController(recycledViewPool.mScrap, maxScrap)
    addRecyclerListener(controller)

    // 由于此时是视图控制器销毁阶段，因此调用startInterceptRequestLayout()，
    // 避免removeAndRecycleViews()移除子View时，执行不必要的requestLayout()。
    startInterceptRequestLayout()

    // 通过removeAndRecycleViews()进行回收，确保执行正常回收流程相关的回调
    removeAndRecycleViews()

    // 回收完成后，恢复viewType原本的回收上限，避免后续出现不必要的缓存堆积
    controller.restoreMaxScrap()
}

private class MaxScrapController(
    private val scrap: SparseArray<ScrapData>,
    private val maxScrap: (viewType: Int) -> Int
) : RecyclerListener {
    private var state: Any? = null

    override fun onViewRecycled(holder: ViewHolder) {
        val viewType = holder.itemViewType
        // scrapData还未创建，不需要调整viewType的回收上限
        val scrapData = scrap[viewType] ?: return
        if (scrapData.mMaxScrap < DEFAULT_MAX_SCRAP) {
            // viewType的回收上限小于默认值DEFAULT_MAX_SCRAP，
            // 说明回收上限被特意调小，一般表示该类型出现的较少，
            // 为了保持原本的意图，不需要调整viewType的回收上限。
            return
        }

        if (scrapData.mScrapHeap.size < scrapData.mMaxScrap) {
            // 还可以继续回收，不需要调整viewType的回收上限
            return
        }

        val maxScrap = maxScrap(viewType)
            .coerceAtLeast(scrapData.mMaxScrap)
        if (scrapData.mMaxScrap < maxScrap) {
            // 回收过程中，同一个viewType只会进入该分支逻辑一次，
            // 记录viewType原本的回收上限，并将上限调整为maxScrap。
            saveMaxScrap(viewType, scrapData.mMaxScrap)
            scrapData.mMaxScrap = maxScrap
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

    fun restoreMaxScrap() {
        when (val state = state) {
            is Pair -> restoreMaxScrap(state.viewType, state.maxScrap)
            is SparseIntArray -> state.forEach { viewType, maxScrap ->
                restoreMaxScrap(viewType, maxScrap)
            }
        }
    }

    private fun restoreMaxScrap(viewType: Int, maxScrap: Int) {
        scrap[viewType]?.mMaxScrap = maxScrap
    }

    private class Pair(val viewType: Int, val maxScrap: Int)

    private companion object {
        /**
         * [RecycledViewPool.DEFAULT_MAX_SCRAP]
         */
        const val DEFAULT_MAX_SCRAP = 5
    }
}