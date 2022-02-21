@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.recyclerview.widget.RecyclerView.RecycledViewPool.ScrapData
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.recyclerview.widget.RecyclerView.getChildViewHolderInt

fun RecyclerView.tryRecycleAllChild() {
    val childCount = childCount
    for (index in childCount - 1 downTo 0) {
        val child = getChildAt(index) ?: continue
        val holder = getChildViewHolderInt(child)
        if (!tryIncreaseMaxScrap(childCount, holder)) {
            continue
        }
        // 若直接结束item动画，则ItemAnimator的回调可能会回收holder，
        // 移除child时dispatchDetachedFromWindow()可能包含结束动画的操作，
        // 为了避免holder被重复回收，在结束动画之前先将holder设为不可回收状态。
        holder.setIsRecyclable(false)
        removeViewInLayout(child)
        itemAnimator?.endAnimation(holder)
        holder.setIsRecyclable(true)

        // 重置holder并设为无效状态，回收时跳过mCachedViews的处理。
        layoutManager?.stopIgnoringView(child)
        mRecycler.quickRecycleScrapView(child)
    }
    mRecycler.clearScrap()
}

private const val DEFAULT_MAX_SCRAP = 5

private fun RecyclerView.tryIncreaseMaxScrap(childCount: Int, holder: ViewHolder): Boolean {
    val viewType = holder.itemViewType
    // 容器还未创建，不需要增加上限
    val scrapData: ScrapData = recycledViewPool.mScrap.get(viewType) ?: return true
    val scrapHeapSize = scrapData.mScrapHeap.size
    if (scrapHeapSize < scrapData.mMaxScrap) {
        // 还可以继续回收，不需要增加上限
        return true
    }
    if (scrapData.mMaxScrap < DEFAULT_MAX_SCRAP) {
        // 调用者调小了上限，因此不进行修改
        return false
    }
    if (scrapHeapSize < childCount * 2) {
        // 首次增加上限，不能超过子view数量
        // FIXME: 2022/2/21 在回收完之后，直接修改max值进行恢复，对get不会有影响
        scrapData.mMaxScrap += 1
        return true
    }
    return false
}