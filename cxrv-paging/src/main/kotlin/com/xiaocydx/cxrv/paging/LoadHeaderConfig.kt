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
import androidx.annotation.Px
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.cxrv.internal.RvDslMarker
import com.xiaocydx.cxrv.list.ListAdapter

/**
 * 加载头部配置
 *
 * @author xcc
 * @date 2022/5/18
 */
@RvDslMarker
class LoadHeaderConfig @PublishedApi internal constructor() {
    private var isCompleted = false

    @PublishedApi
    internal var loadingScope: LoadViewScope<out View>? = null
        set(value) {
            checkCompleted()
            field = value
        }

    @PublishedApi
    internal var emptyScope: LoadViewScope<out View>? = null
        set(value) {
            checkCompleted()
            field = value
        }

    @PublishedApi
    internal var failureScope: LoadViewScope<out View>? = null
        set(value) {
            checkCompleted()
            field = value
        }

    /**
     * 加载头部的宽度
     */
    @get:Px
    @setparam:Px
    var width: Int = MATCH_PARENT
        set(value) {
            checkCompleted()
            field = value
        }

    /**
     * 加载头部的高度
     */
    @get:Px
    @setparam:Px
    var height: Int = MATCH_PARENT
        set(value) {
            checkCompleted()
            field = value
        }

    /**
     * 加载中视图
     *
     * ```
     * loading<ProgressBar> {
     *     onCreateView { parent -> ProgressBar(parent.context) }
     *     onUpdateView { view -> ... }
     * }
     * ```
     */
    inline fun <V : View> loading(block: LoadViewScope<V>.() -> Unit) {
        loadingScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 空结果视图
     *
     * ```
     * empty<TextView> {
     *     onCreateView { parent -> TextView(parent.context) }
     *     onUpdateView { view -> ... }
     * }
     * ```
     */
    inline fun <V : View> empty(block: LoadViewScope<V>.() -> Unit) {
        emptyScope = LoadViewScope<V>().apply(block)
    }

    /**
     * 加载失败视图
     *
     * ```
     * failure<TextView> {
     *     onCreateView { parent -> TextView(parent.context) }
     *     onUpdateView { view -> ... }
     * }
     * ```
     */
    inline fun <V : View> failure(block: LoadViewScope<V>.() -> Unit) {
        failureScope = LoadViewScope<V>().apply(block)
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
     * 空结果视图
     *
     * 若[block]为null，则表示不需要空结果视图。
     * ```
     * emptyView { parent -> TextView(parent.context) }
     * ```
     */
    fun emptyView(block: OnCreateView<out View>?) {
        if (block == null) {
            emptyScope = null
        } else {
            empty<View> { onCreateView(block) }
        }
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
        checkCompleted()
        isCompleted = true
        loadingScope?.complete(retry, exception)
        emptyScope?.complete(retry, exception)
        failureScope?.complete(retry, exception)
    }

    private fun checkCompleted() {
        check(!isCompleted) { "已完成加载头部配置" }
    }
}

/**
 * 构建加载头部适配器
 *
 * 详细的加载头部配置描述[LoadHeaderConfig]。
 */
@Suppress("FunctionName")
inline fun LoadHeaderAdapter(
    adapter: ListAdapter<*, *>,
    block: LoadHeaderConfig.() -> Unit
): ViewAdapter<*> = LoadHeaderAdapter(LoadHeaderConfig().apply(block), adapter)