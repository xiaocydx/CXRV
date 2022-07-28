package com.xiaocydx.sample.paging.config

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.doOnAttach
import com.xiaocydx.cxrv.paging.LoadFooterConfig
import com.xiaocydx.cxrv.paging.LoadHeaderConfig
import com.xiaocydx.cxrv.paging.OnCreateView
import com.xiaocydx.cxrv.paging.PagingConcatScope
import com.xiaocydx.sample.R
import com.xiaocydx.sample.dp

/**
 * 连接加载头部适配器、内容适配器、加载尾部适配器，返回连接后的结果
 *
 * 1. 通过[PagingConcatScope.loadHeader]设置加载头部的配置，详细配置描述可以看[LoadHeaderConfig]。
 * 2. 通过[PagingConcatScope.loadFooter]设置加载尾部的配置，详细配置描述可以看[LoadFooterConfig]。
 * ```
 * val listAdapter: ListAdapter<*, *> = ...
 * val adapter = listAdapter.withPaging {
 *     loadHeader { ... }
 *     loadFooter { ... }
 * }
 * ```
 */
@CheckResult
inline fun ListAdapter<*, *>.withPaging(block: DefaultPagingConcatScope.() -> Unit = {}): Adapter<*> {
    return DefaultPagingConcatScope().apply(block).complete(this)
}

/**
 * 连接加载头部适配器、内容适配器、加载尾部适配器的分页初始化作用域，
 * 对加载头部适配器和加载尾部适配器添加了默认配置，以及添加初始化属性。
 */
class DefaultPagingConcatScope
@PublishedApi internal constructor() : PagingConcatScope() {

    /**
     * 是否启用item动画，分页场景是否默认启用item动画，由上层决定
     */
    var enabledItemAnimator: Boolean = true
        set(value) {
            checkComplete()
            field = value
        }

    @PublishedApi
    internal fun complete(listAdapter: ListAdapter<*, *>): Adapter<*> {
        if (!enabledItemAnimator) {
            listAdapter.doOnAttach { it.itemAnimator = null }
        }
        return completeConcat(listAdapter)
    }

    override fun LoadHeaderConfig.withDefault(): Boolean {
        loadingView(defaultHeaderLoadingView)
        failureView(defaultHeaderFailureView)
        emptyView(defaultHeaderEmptyView)
        return true
    }

    override fun LoadFooterConfig.withDefault(): Boolean {
        height = 50.dp
        isFullyVisibleWhileExceed = true
        loadingView(defaultFooterLoadingView)
        failureView(defaultFooterFailureView)
        fullyView(defaultFooterFullyView)
        return true
    }

    private companion object {
        val defaultHeaderLoadingView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_header_loading)
        }

        val defaultHeaderFailureView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_header_failure).apply {
                findViewById<View>(R.id.btnRetry).setOnClickListener { retry() }
            }
        }

        val defaultHeaderEmptyView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_header_empty)
        }

        val defaultFooterLoadingView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_footer_loading)
        }

        val defaultFooterFailureView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_footer_failure).apply {
                setOnClickListener { retry() }
            }
        }

        val defaultFooterFullyView: OnCreateView<View> = { parent ->
            parent.inflate(R.layout.load_footer_fully)
        }

        fun ViewGroup.inflate(@LayoutRes resId: Int): View {
            return LayoutInflater.from(context).inflate(resId, this, false)
        }
    }
}