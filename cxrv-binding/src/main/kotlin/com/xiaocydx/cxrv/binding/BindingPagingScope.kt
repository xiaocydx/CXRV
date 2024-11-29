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

package com.xiaocydx.cxrv.binding

import android.view.LayoutInflater
import android.view.View
import androidx.viewbinding.ViewBinding
import com.xiaocydx.cxrv.paging.LoadFooterConfig
import com.xiaocydx.cxrv.paging.LoadHeaderConfig
import com.xiaocydx.cxrv.paging.LoadViewScope
import com.xiaocydx.cxrv.paging.OnCreateView

/**
 * 加载中视图
 *
 * ```
 * loadingView(LoadingBinding::Inflate) {...}
 * ```
 */
inline fun <VB : ViewBinding> LoadHeaderConfig.loadingView(
    noinline inflate: Inflate<VB>,
    crossinline block: VB.(completed: LoadCompleted) -> Unit = {}
) = loadingView(onCreateView(inflate, block))

/**
 * 空结果视图
 *
 * ```
 * emptyView(EmptyBinding::Inflate) {...}
 * ```
 */
inline fun <VB : ViewBinding> LoadHeaderConfig.emptyView(
    noinline inflate: Inflate<VB>,
    crossinline block: VB.(completed: LoadCompleted) -> Unit = {}
) = emptyView(onCreateView(inflate, block))

/**
 * 加载失败视图
 *
 * ```
 * failureView(FailureBinding::Inflate) { completed ->
 *     completed.retry() // 重新加载
 *     completed.exception() // 加载失败的异常
 *     ...
 * }
 * ```
 */
inline fun <VB : ViewBinding> LoadHeaderConfig.failureView(
    noinline inflate: Inflate<VB>,
    crossinline block: VB.(completed: LoadCompleted) -> Unit = {}
) = failureView(onCreateView(inflate, block))

/**
 * 加载中视图
 *
 * ```
 * loadingView(LoadingBinding::Inflate) {...}
 * ```
 */
inline fun <VB : ViewBinding> LoadFooterConfig.loadingView(
    noinline inflate: Inflate<VB>,
    crossinline block: VB.(completed: LoadCompleted) -> Unit = {}
) = loadingView(onCreateView(inflate, block))

/**
 * 加载完全视图
 *
 * ```
 * fullyView(FullyBinding::Inflate) {...}
 * ```
 */
inline fun <VB : ViewBinding> LoadFooterConfig.fullyView(
    noinline inflate: Inflate<VB>,
    crossinline block: VB.(completed: LoadCompleted) -> Unit = {}
) = fullyView(onCreateView(inflate, block))

/**
 * 加载失败视图
 *
 * ```
 * failureView(FailureBinding::Inflate) { completed ->
 *     completed.retry() // 重新加载
 *     completed.exception() // 加载失败的异常
 *     ...
 * }
 * ```
 */
inline fun <VB : ViewBinding> LoadFooterConfig.failureView(
    noinline inflate: Inflate<VB>,
    crossinline block: VB.(completed: LoadCompleted) -> Unit = {}
) = failureView(onCreateView(inflate, block))

typealias LoadCompleted = LoadViewScope<out View>

@PublishedApi
internal inline fun <VB : ViewBinding> onCreateView(
    noinline inflate: Inflate<VB>,
    crossinline block: VB.(completed: LoadCompleted) -> Unit = {}
): OnCreateView<out View> = { parent ->
    val completed = this
    val inflater = LayoutInflater.from(parent.context)
    inflate(inflater, parent, false).apply { block(completed) }.root
}