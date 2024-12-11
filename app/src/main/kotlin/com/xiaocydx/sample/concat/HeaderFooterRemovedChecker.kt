@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import androidx.core.util.forEach
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.xiaocydx.cxrv.concat.ViewAdapter

/**
 * HeaderFooter的实现确保从RecyclerView的缓存中清除已移除的Header和Footer，
 * 移除Header和Footer后，用[HeaderFooterRemovedChecker]检查是否从缓存中清除，
 * 若Header和Footer还在缓存中，则抛出[IllegalStateException]异常。
 *
 * @author xcc
 * @date 2022/12/14
 */
class HeaderFooterRemovedChecker(private val rv: RecyclerView, viewAdapter: ViewAdapter<*>) {
    private val removedViewType = viewAdapter.getItemViewType()

    fun check() {
        // remove动画结束后，Header和Footer才被回收进缓存
        rv.itemAnimator?.isRunning(::performCheck) ?: performCheck()
    }

    private fun performCheck() {
        checkCacheViews()
        checkRecycledViewPool()
    }

    /**
     * HeaderFooter的实现确保从离屏缓存中清除已移除的Header或Footer
     */
    private fun checkCacheViews() {
        rv.mRecycler.mCachedViews.forEach {
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
        rv.recycledViewPool.mScrap.forEach { viewType, scrapData ->
            check(viewType != removedViewType) {
                "已移除的Header或Footer，未从RecycledViewPool中清除"
            }
        }
    }
}