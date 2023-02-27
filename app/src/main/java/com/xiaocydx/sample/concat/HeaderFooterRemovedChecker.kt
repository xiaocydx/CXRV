@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool

/**
 * 检查Header和Footer移除后是否从缓存中清除
 *
 * @author xcc
 * @date 2022/12/14
 */
class HeaderFooterRemovedChecker(
    private val view: RecyclerView,
    private val removedViewType: Int
) {

    @Throws(IllegalStateException::class)
    fun check() {
        checkCacheViews()
        checkRecycledViewPool()
    }

    /**
     * HeaderFooter的实现确保从离屏缓存中清除已移除的Header或Footer
     */
    private fun checkCacheViews() {
        view.mRecycler.mCachedViews.forEach {
            check(it.itemViewType != removedViewType) {
                "已移除的Header或Footer，未从CacheViews中清除"
            }
        }
    }

    /**
     * HeaderFooter的实现确保从RecycledViewPool中清除已移除的Header或Footer，
     * 这里的清除指的是彻底清除，连[RecycledViewPool.ScrapData]都不会保留。
     */
    private fun checkRecycledViewPool() {
        view.recycledViewPool.mScrap.forEach { viewType, scrapData ->
            check(viewType != removedViewType) {
                "已移除的Header或Footer，未从RecycledViewPool中清除"
            }
        }
    }
}