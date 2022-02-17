package com.xiaocydx.sample.paging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.extension.PagingScope
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.paging.LoadFooter
import com.xiaocydx.recycler.paging.LoadHeader
import com.xiaocydx.recycler.paging.OnCreateLoadView
import com.xiaocydx.sample.R
import com.xiaocydx.sample.dp

/**
 * 分页场景的初始化函数，可用于链式调用场景
 *
 * 通过[PagingScope.loadHeader]、[PagingScope.loadFooter]设置加载头尾配置，
 * 详细的加载头尾配置描述[LoadHeader.Config]、[LoadFooter.Config]。
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.paging(
 *     listAdapter = adapter
 *     loadHeader { ... }
 *     loadFooter { ... }
 * )
 * ```
 */
@MainThread
inline fun <T : RecyclerView> T.paging(block: PagingScope.() -> Unit): T {
    DefaultPagingScope(this).apply(block).init()
    return this
}

/**
 * 分页拖拽刷新场景的初始化函数，可用于链式调用场景
 *
 * 通过[PagingScope.loadHeader]、[PagingScope.loadFooter]设置加载头尾配置，
 * 详细的加载头尾配置描述[LoadHeader.Config]、[LoadFooter.Config]。
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.pagingDragRefresh(
 *     listAdapter = adapter
 *     loadHeader { ... }
 *     loadFooter { ... }
 * )
 * ```
 */
@MainThread
inline fun <T : RecyclerView> T.pagingDragRefresh(block: DragRefreshPagingScope.() -> Unit): T {
    DragRefreshPagingScope(this).apply(block).init()
    return this
}

/**
 * 分页场景的初始化函数，可用于链式调用场景
 */
@MainThread
fun <T : RecyclerView> T.paging(
    adapter: ListAdapter<*, *>
): T = paging { listAdapter = adapter }

/**
 * 分页拖拽刷新场景的初始化函数，可用于链式调用场景
 */
@MainThread
fun <T : RecyclerView> T.pagingDragRefresh(
    adapter: ListAdapter<*, *>
): T = pagingDragRefresh { listAdapter = adapter }

/**
 * 添加了默认配置的分页拖拽刷新初始化作用域
 */
class DragRefreshPagingScope(
    rv: RecyclerView
) : DefaultPagingScope(rv) {
    // private val _refreshLayout: DragRefreshLayout = rv.replaceWithDragRefresh()
    // val refreshLayout: DragToRefreshBase<*> = _refreshLayout

    override fun init() {
        // _refreshLayout.setAdapter(getFinalListAdapter())
        return super.init()
    }
}

/**
 * 添加了默认配置的分页初始化作用域
 */
open class DefaultPagingScope(
    private val rv: RecyclerView
) : PagingScope() {

    @CallSuper
    open fun init() {
        init(rv)
    }

    override fun LoadHeader.Config.withDefault(): Boolean {
        loadingView(DefaultHeaderLoadingView)
        failureView(DefaultHeaderFailureView)
        emptyView(DefaultHeaderEmptyView)
        return true
    }

    override fun LoadFooter.Config.withDefault(): Boolean {
        footerHeight = 50.dp
        isShowFullyWhileExceed = true
        loadingView(DefaultFooterLoadingView)
        failureView(DefaultFooterFailureView)
        fullyView(DefaultFooterFullyView)
        return true
    }

    private companion object {
        val DefaultHeaderLoadingView: OnCreateLoadView<View> = { parent ->
            parent.inflate(R.layout.load_header_loading)
        }

        val DefaultHeaderFailureView: OnCreateLoadView<View> = { parent ->
            parent.inflate(R.layout.load_header_failure).apply {
                findViewById<View>(R.id.btnRetry).setOnClickListener { retry() }
            }
        }

        val DefaultHeaderEmptyView: OnCreateLoadView<View> = { parent ->
            parent.inflate(R.layout.load_header_empty)
        }

        val DefaultFooterLoadingView: OnCreateLoadView<View> = { parent ->
            parent.inflate(R.layout.load_footer_loading)
        }

        val DefaultFooterFailureView: OnCreateLoadView<View> = { parent ->
            parent.inflate(R.layout.load_footer_failure).apply {
                setOnClickListener { retry() }
            }
        }

        val DefaultFooterFullyView: OnCreateLoadView<View> = { parent ->
            parent.inflate(R.layout.load_footer_fully)
        }

        fun ViewGroup.inflate(@LayoutRes resId: Int): View {
            return LayoutInflater.from(context).inflate(resId, this, false)
        }
    }
}