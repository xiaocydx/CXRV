package com.xiaocydx.cxrv.paging

import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.concat.withFooter
import com.xiaocydx.cxrv.concat.withHeader
import com.xiaocydx.cxrv.internal.RvDslMarker
import com.xiaocydx.cxrv.list.ListAdapter

/**
 * 连接加载头部适配器、内容适配器、加载尾部适配器的分页初始化作用域
 *
 * @author xcc
 * @date 2022/7/27
 */
@RvDslMarker
open class PagingConcatScope {
    private var isNeedHeader = true
    private var isNeedFooter = true
    private var loadHeader: ViewAdapter<*>? = null
    private var loadFooter: ViewAdapter<*>? = null
    private var initHeader: (LoadHeaderConfig.() -> Unit)? = null
    private var initFooter: (LoadFooterConfig.() -> Unit)? = null
    private var isComplete = false

    /**
     * 设置加载头部适配器
     *
     * @param adapter 传入`null`表示不需要加载头部
     */
    fun loadHeader(adapter: ViewAdapter<*>?) {
        checkComplete()
        loadHeader = adapter
        initHeader = null
        isNeedHeader = adapter != null
    }

    /**
     * 设置加载头部配置
     *
     * 详细的加载头部配置描述[LoadHeaderConfig]
     * ```
     * loadHeader {
     *     loadingView { parent -> ProgressBar(parent.context) }
     *     emptyView { parent -> TextView(parent.context) }
     *     failureView { parent -> TextView(parent.context) }
     *
     *     // 在显示视图时执行某些操作，可以用以下写法
     *     loading<ProgressBar> {
     *         onCreateView { parent -> ProgressBar(parent.context) }
     *         onVisibleChanged { view, isVisible -> ... }
     *     }
     *     empty<TextView> {
     *         onCreateView { parent -> TextView(parent.context) }
     *         onVisibleChanged { view, isVisible -> ... }
     *     }
     *     failure<TextView> {
     *         onCreateView { parent -> TextView(parent.context) }
     *         onVisibleChanged { view, isVisible -> exception() }
     *     }
     * }
     * ```
     */
    fun loadHeader(block: LoadHeaderConfig.() -> Unit) {
        checkComplete()
        loadHeader = null
        initHeader = block
        isNeedHeader = true
    }

    /**
     * 设置加载尾部适配器
     *
     * @param adapter 传入`null`表示不需要加载尾部
     */
    fun loadFooter(adapter: ViewAdapter<*>?) {
        checkComplete()
        loadFooter = adapter
        initFooter = null
        isNeedFooter = adapter != null
    }

    /**
     * 设置加载尾部配置
     *
     * 详细的加载尾部配置描述[LoadFooterConfig]
     * ```
     * loadFooter {
     *     loadingView { parent -> ProgressBar(parent.context) }
     *     fullyView { parent -> TextView(parent.context) }
     *     failureView { parent -> TextView(parent.context) }
     *
     *     // 在显示视图时执行某些操作，可以用以下写法
     *     loading<ProgressBar> {
     *         onCreateView { parent -> ProgressBar(parent.context) }
     *         onVisibleChanged { view, isVisible -> ... }
     *     }
     *     fully<TextView> {
     *         onCreateView { parent -> TextView(parent.context) }
     *         onVisibleChanged { view, isVisible -> ... }
     *     }
     *     failure<TextView> {
     *         onCreateView { parent -> TextView(parent.context) }
     *         onVisibleChanged { view, isVisible -> exception() }
     *     }
     * }
     * ```
     */
    fun loadFooter(block: LoadFooterConfig.() -> Unit) {
        checkComplete()
        loadFooter = null
        initFooter = block
        isNeedFooter = true
    }

    /**
     * 加载头部默认配置，返回`true`表示有默认配置
     */
    protected open fun LoadHeaderConfig.withDefault(): Boolean = false

    /**
     * 加载尾部默认配置，返回`true`表示有默认配置
     */
    protected open fun LoadFooterConfig.withDefault(): Boolean = false

    protected fun checkComplete() {
        check(!isComplete) { "已完成适配器的连接工作" }
    }

    /**
     * 完成适配器的连接工作，返回连接后的结果，该函数只能被调用一次
     */
    protected fun completeConcat(adapter: ListAdapter<*, *>): Adapter<*> {
        checkComplete()
        isComplete = true
        var outcome: Adapter<*> = adapter
        getLoadHeader(adapter)?.let {
            outcome = outcome.withHeader(it)
        }
        getLoadFooter(adapter)?.let {
            outcome = outcome.withFooter(it)
        }
        clear()
        return outcome
    }

    private fun getLoadHeader(adapter: ListAdapter<*, *>): ViewAdapter<*>? {
        if (!isNeedHeader) {
            return null
        }
        loadHeader?.let { return it }
        val config = LoadHeaderConfig()
        val withDefault = config.withDefault()
        if (!withDefault && initHeader == null) {
            return null
        }
        initHeader?.invoke(config)
        return LoadHeaderAdapter(config, adapter)
    }

    private fun getLoadFooter(adapter: ListAdapter<*, *>): ViewAdapter<*>? {
        if (!isNeedFooter) {
            return null
        }
        loadFooter?.let { return it }
        val config = LoadFooterConfig()
        val withDefault = config.withDefault()
        if (!withDefault && initFooter == null) {
            return null
        }
        initFooter?.invoke(config)
        return LoadFooterAdapter(config, adapter)
    }

    private fun clear() {
        loadHeader = null
        loadFooter = null
        initFooter = null
        initHeader = null
    }
}