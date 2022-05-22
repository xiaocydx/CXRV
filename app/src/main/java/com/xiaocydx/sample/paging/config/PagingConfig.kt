package com.xiaocydx.sample.paging.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.paging.LoadFooterConfig
import com.xiaocydx.recycler.paging.LoadHeaderConfig
import com.xiaocydx.recycler.paging.OnCreateView
import com.xiaocydx.recycler.paging.PagingScope
import com.xiaocydx.sample.R
import com.xiaocydx.sample.dp

/**
 * 分页场景的初始化函数，可用于链式调用场景
 *
 * 通过[PagingScope.loadHeader]、[PagingScope.loadFooter]设置加载头尾配置，
 * 详细的加载头尾配置描述[LoadHeaderConfig]、[LoadFooterConfig]。
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.paging(
 *     listAdapter = adapter
 *     loadHeader { ... }
 *     loadFooter { ... }
 * )
 * ```
 */
inline fun <T : RecyclerView> T.paging(
    block: PagingScope.() -> Unit
): T {
    DefaultPagingScope(this).apply(block).init()
    return this
}

/**
 * 分页拖拽刷新场景的初始化函数，可用于链式调用场景
 *
 * 通过[PagingScope.loadHeader]、[PagingScope.loadFooter]设置加载头尾配置，
 * 详细的加载头尾配置描述[LoadHeaderConfig]、[LoadFooterConfig]。
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.pagingSwipeRefresh(
 *     listAdapter = adapter
 *     loadHeader { ... }
 *     loadFooter { ... }
 * )
 * ```
 */
inline fun <T : RecyclerView> T.pagingSwipeRefresh(
    block: SwipeRefreshPagingScope.() -> Unit
): T {
    SwipeRefreshPagingScope(this).apply(block).init()
    return this
}

/**
 * 分页场景的初始化函数，可用于链式调用场景
 */
infix fun <T : RecyclerView> T.paging(
    adapter: ListAdapter<*, *>
): T = paging { listAdapter = adapter }

/**
 * 分页拖拽刷新场景的初始化函数，可用于链式调用场景
 */
infix fun <T : RecyclerView> T.pagingSwipeRefresh(
    adapter: ListAdapter<*, *>
): T = pagingSwipeRefresh { listAdapter = adapter }

/**
 * 添加了默认配置的分页拖拽刷新初始化作用域
 */
class SwipeRefreshPagingScope(
    rv: RecyclerView
) : DefaultPagingScope(rv) {
    private val _refreshLayout: DefaultSwipeRefreshLayout = rv.replaceWithSwipeRefresh()
    val refreshLayout: SwipeRefreshLayout = _refreshLayout

    override fun init() {
        _refreshLayout.setAdapter(getFinalListAdapter())
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

    override fun LoadHeaderConfig.withDefault(): Boolean {
        loadingView(DefaultHeaderLoadingView)
        failureView(DefaultHeaderFailureView)
        emptyView(DefaultHeaderEmptyView)
        return true
    }

    override fun LoadFooterConfig.withDefault(): Boolean {
        height = 50.dp
        isFullyVisibleWhileExceed = true
        loadingView(DefaultFooterLoadingView)
        failureView(DefaultFooterFailureView)
        fullyView(DefaultFooterFullyView)
        return true
    }

    private companion object {
        val DefaultHeaderLoadingView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_header_loading)
        }

        val DefaultHeaderFailureView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_header_failure).apply {
                findViewById<View>(R.id.btnRetry).setOnClickListener { retry() }
            }
        }

        val DefaultHeaderEmptyView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_header_empty)
        }

        val DefaultFooterLoadingView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_footer_loading)
        }

        val DefaultFooterFailureView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_footer_failure).apply {
                setOnClickListener { retry() }
            }
        }

        val DefaultFooterFullyView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_footer_fully)
        }

        fun ViewGroup.inflate(@LayoutRes resId: Int): View {
            return LayoutInflater.from(context).inflate(resId, this, false)
        }
    }
}