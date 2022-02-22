@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData
import androidx.recyclerview.widget.RecyclerView.ViewHolder

@Deprecated("实现逻辑有问题，需要重新编写。")
fun RecyclerView.tryRecycleAllChild() {
    // FIXME: 2022/2/22 maxScrap需要更合理的计算方式
    val maxScrap = childCount * 2
    addRecyclerListener { holder ->
        tryIncreaseMaxScrap(maxScrap, holder)
    }
    // 该函数在视图销毁阶段才被调用，因此通过suppressLayout(true)抑制布局，
    // 避免removeAndRecycleViews()移除View时，触发不必要的requestLayout()。
    suppressLayout(true)
    removeAndRecycleViews()
    // FIXME: 2022/2/22 回收完之后，恢复原本的maxScrap
}

private const val DEFAULT_MAX_SCRAP = 5

@Deprecated("实现逻辑有问题，需要重新编写。")
private fun RecyclerView.tryIncreaseMaxScrap(maxScrap: Int, holder: ViewHolder): Boolean {
    val viewType = holder.itemViewType
    // 容器还未创建，不需要增加上限
    val scrapData: ScrapData = recycledViewPool.mScrap.get(viewType) ?: return true
    val scrapHeapSize = scrapData.mScrapHeap.size
    if (scrapHeapSize < scrapData.mMaxScrap) {
        // 还可以继续回收，不需要增加上限
        return true
    }
    if (scrapData.mMaxScrap < DEFAULT_MAX_SCRAP) {
        // 调用者调小了上限，说该类型出现的较少，因此不增加上限
        return false
    }
    if (scrapHeapSize < maxScrap) {
        scrapData.mMaxScrap += 1
        return true
    }
    return false
}