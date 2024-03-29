/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.xiaocydx.cxrv.paging

import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.xiaocydx.cxrv.concat.Concat
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.internal.RvDslMarker
import com.xiaocydx.cxrv.list.ListAdapter

/**
 * 分页初始化作用域
 *
 * @author xcc
 * @date 2022/3/8
 */
@RvDslMarker
@Deprecated(
    message = "此类的命名、属性、函数不够合理，因此废弃",
    replaceWith = ReplaceWith("PagingConcatScope")
)
open class PagingScope {
    private var isNeedHeader = true
    private var isNeedFooter = true
    private var loadHeader: ViewAdapter<*>? = null
    private var loadFooter: ViewAdapter<*>? = null
    private var initHeader: (LoadHeaderConfig.() -> Unit)? = null
    private var initFooter: (LoadFooterConfig.() -> Unit)? = null

    /**
     * 是否启用item动画
     */
    var enabledItemAnim: Boolean = true

    /**
     * 内容适配器
     */
    var listAdapter: ListAdapter<*, *>? = null

    /**
     * 设置加载头部适配器
     *
     * @param adapter 传入`null`表示不需要加载头部
     */
    fun loadHeader(adapter: ViewAdapter<*>?) {
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
        loadFooter = null
        initFooter = block
        isNeedFooter = true
    }

    protected fun complete(rv: RecyclerView) {
        val sourceAdapter: Adapter<*>? = rv.adapter
        if (sourceAdapter is ConcatAdapter) {
            sourceAdapter.apply {
                getFinalLoadHeader()?.let(::addAdapter)
                addAdapter(getFinalListAdapter())
                getFinalLoadFooter()?.let(::addAdapter)
            }
        } else {
            rv.adapter = Concat
                .headerOrNull(getFinalLoadHeader())
                .content(getFinalListAdapter())
                .footerOrNull(getFinalLoadFooter())
                .concatIfNecessary()
        }
        if (!enabledItemAnim) {
            rv.itemAnimator = null
        }
        clear()
    }

    /**
     * 加载头部默认配置，返回`true`表示有默认配置
     */
    protected open fun LoadHeaderConfig.withDefault(): Boolean = false

    /**
     * 加载尾部默认配置，返回`true`表示有默认配置
     */
    protected open fun LoadFooterConfig.withDefault(): Boolean = false

    protected fun getFinalListAdapter(): ListAdapter<*, *> {
        return requireNotNull(listAdapter) { "未设置或已清除listAdapter" }
    }

    private fun getFinalLoadHeader(): ViewAdapter<*>? {
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
        return LoadHeaderAdapter(config, getFinalListAdapter())
    }

    private fun getFinalLoadFooter(): ViewAdapter<*>? {
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
        return LoadFooterAdapter(config, getFinalListAdapter())
    }

    private fun clear() {
        loadHeader = null
        loadFooter = null
        initFooter = null
        initHeader = null
        listAdapter = null
    }
}