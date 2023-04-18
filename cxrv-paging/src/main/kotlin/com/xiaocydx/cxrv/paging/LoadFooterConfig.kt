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

import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.annotation.Px
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.internal.RvDslMarker
import com.xiaocydx.cxrv.list.ListAdapter

/**
 * 加载尾部配置
 *
 * @author xcc
 * @date 2022/5/21
 */
@RvDslMarker
class LoadFooterConfig @PublishedApi internal constructor() {
    private var isComplete = false

    @PublishedApi
    internal var loadingScope: LoadViewScope<out View>? = null
        set(value) {
            checkComplete()
            field = value
        }

    @PublishedApi
    internal var fullyScope: LoadViewScope<out View>? = null
        set(value) {
            checkComplete()
            field = value
        }

    @PublishedApi
    internal var failureScope: LoadViewScope<out View>? = null
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 加载尾部的宽度
     */
    @get:Px
    @setparam:Px
    var width: Int = MATCH_PARENT
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 加载尾部的高度
     */
    @get:Px
    @setparam:Px
    var height: Int = WRAP_CONTENT
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 是否当RecyclerView的滚动范围超过可视范围时，才显示加载中视图
     */
    var isLoadingVisibleWhileExceed: Boolean = false
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 是否当RecyclerView的滚动范围超过可视范围时，才显示加载完全视图
     */
    var isFullyVisibleWhileExceed: Boolean = false
        set(value) {
            checkComplete()
            field = value
        }

    /**
     * 加载中视图
     *
     * ```
     * loading<ProgressBar> {
     *     onCreateView { parent -> ProgressBar(parent.context) }
     *     onVisibleChanged { view, isVisible -> ... }
     * }
     * ```
     */
    inline fun <V : View> loading(block: LoadViewScope<V>.() -> Unit) {
        loadingScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 加载中视图
     *
     * 若[block]为null，则表示不需要加载中视图。
     * ```
     * loadingView { parent -> ProgressBar(parent.context) }
     * ```
     */
    fun loadingView(block: OnCreateView<View>?) {
        if (block == null) {
            loadingScope = null
        } else {
            loading<View> { onCreateView(block) }
        }
    }

    /**
     * 加载完全视图
     *
     * ```
     * fully<TextView> {
     *     onCreateView { parent -> TextView(parent.context) }
     *     onVisibleChanged { view, isVisible -> ... }
     * }
     * ```
     */
    inline fun <V : View> fully(block: LoadViewScope<V>.() -> Unit) {
        fullyScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 加载完全视图
     *
     * 若[block]为null，则表示不需要加载完全视图。
     * ```
     * fullyView { parent -> TextView(parent.context) }
     * ```
     */
    fun fullyView(block: OnCreateView<out View>?) {
        if (block == null) {
            fullyScope = null
        } else {
            fully<View> { onCreateView(block) }
        }
    }

    /**
     * 加载失败视图
     *
     * ```
     * failure<TextView> {
     *     onCreateView { parent -> TextView(parent.context) }
     *     onVisibleChanged { view, isVisible -> exception() }
     * }
     * ```
     */
    inline fun <V : View> failure(block: LoadViewScope<V>.() -> Unit) {
        failureScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 加载失败视图
     *
     * 若[block]为null，则表示不需要加载失败视图。
     * ```
     * failureView { parent -> TextView(parent.context) }
     * ```
     */
    fun failureView(block: OnCreateView<out View>?) {
        if (block == null) {
            failureScope = null
        } else {
            failure<View> { onCreateView(block) }
        }
    }

    internal fun complete(retry: () -> Unit, exception: () -> Throwable?) {
        checkComplete()
        isComplete = true
        loadingScope?.complete(retry, exception)
        fullyScope?.complete(retry, exception)
        failureScope?.complete(retry, exception)
    }

    private fun checkComplete() {
        check(!isComplete) { "已完成加载尾部配置" }
    }
}

/**
 * 构建加载尾部适配器
 *
 * 详细的加载头部配置描述[LoadFooterConfig]。
 */
@Suppress("FunctionName")
inline fun LoadFooterAdapter(
    adapter: ListAdapter<*, *>,
    block: LoadFooterConfig.() -> Unit
): ViewAdapter<*> = LoadFooterAdapter(LoadFooterConfig().apply(block), adapter)