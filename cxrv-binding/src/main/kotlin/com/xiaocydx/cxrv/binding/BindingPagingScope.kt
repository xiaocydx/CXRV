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

/**
 * 加载中视图
 *
 * ```
 * // 简写
 * loading(LoadingBinding::Inflate) {...}
 *
 * // 或者
 * loading(
 *     inflate = LoadingBinding::Inflate,
 *     onCreate = {...}
 *     onUpdate = {...}
 * )
 * ```
 */
fun <VB : ViewBinding> LoadHeaderConfig.loading(
    inflate: Inflate<VB>,
    onCreate: (VB.(completed: LoadCompleted) -> Unit)? = null,
    onUpdate: (VB.(completed: LoadCompleted) -> Unit)? = null,
) = loading(scopeBlock(inflate, onCreate, onUpdate))

/**
 * 空结果视图
 *
 * ```
 * // 简写
 * empty(EmptyBinding::Inflate) {...}
 *
 * // 或者
 * empty(
 *     inflate = EmptyBinding::Inflate,
 *     onCreate = {...}
 *     onUpdate = {...}
 * )
 * ```
 */
fun <VB : ViewBinding> LoadHeaderConfig.empty(
    inflate: Inflate<VB>,
    onCreate: (VB.(completed: LoadCompleted) -> Unit)? = null,
    onUpdate: (VB.(completed: LoadCompleted) -> Unit)? = null,
) = empty(scopeBlock(inflate, onCreate, onUpdate))

/**
 * 加载失败视图
 *
 * ```
 * // 简写
 * failure(FailureBinding::Inflate) { completed ->
 *     completed.exception() // 加载失败的异常
 *     root.setOnClickListener { completed.retry() } // 点击重新加载
 * }
 *
 * // 或者
 * failure(
 *     inflate = FailureBinding::Inflate,
 *     onCreate = { completed -> ... }
 *     onUpdate = { completed -> ... }
 * )
 * ```
 */
fun <VB : ViewBinding> LoadHeaderConfig.failure(
    inflate: Inflate<VB>,
    onCreate: (VB.(completed: LoadCompleted) -> Unit)? = null,
    onUpdate: (VB.(completed: LoadCompleted) -> Unit)? = null,
) = failure(scopeBlock(inflate, onCreate, onUpdate))

/**
 * 加载中视图
 *
 * ```
 * // 简写
 * loading(LoadingBinding::Inflate) {...}
 *
 * // 或者
 * loading(
 *     inflate = LoadingBinding::Inflate,
 *     onCreate = {...}
 *     onUpdate = {...}
 * )
 * ```
 */
fun <VB : ViewBinding> LoadFooterConfig.loading(
    inflate: Inflate<VB>,
    onCreate: (VB.(completed: LoadCompleted) -> Unit)? = null,
    onUpdate: (VB.(completed: LoadCompleted) -> Unit)? = null,
) = loading(scopeBlock(inflate, onCreate, onUpdate))

/**
 * 加载完全视图
 *
 * ```
 * // 简写
 * fully(FullyBinding::Inflate) {...}
 *
 * // 或者
 * fully(
 *     inflate = EmptyBinding::Inflate,
 *     onCreate = {...}
 *     onUpdate = {...}
 * )
 * ```
 */
fun <VB : ViewBinding> LoadFooterConfig.fully(
    inflate: Inflate<VB>,
    onCreate: (VB.(completed: LoadCompleted) -> Unit)? = null,
    onUpdate: (VB.(completed: LoadCompleted) -> Unit)? = null,
) = fully(scopeBlock(inflate, onCreate, onUpdate))

/**
 * 加载失败视图
 *
 * ```
 * // 简写
 * failure(FailureBinding::Inflate) { completed ->
 *     completed.exception() // 加载失败的异常
 *     root.setOnClickListener { completed.retry() } // 点击重新加载
 * }
 *
 * // 或者
 * failure(
 *     inflate = FailureBinding::Inflate,
 *     onCreate = { completed -> ... }
 *     onUpdate = { completed -> ... }
 * )
 * ```
 */
fun <VB : ViewBinding> LoadFooterConfig.failure(
    inflate: Inflate<VB>,
    onCreate: (VB.(completed: LoadCompleted) -> Unit)? = null,
    onUpdate: (VB.(completed: LoadCompleted) -> Unit)? = null,
) = failure(scopeBlock(inflate, onCreate, onUpdate))

typealias LoadCompleted = LoadViewScope<out View>

private fun <VB : ViewBinding> scopeBlock(
    inflate: Inflate<VB>,
    onCreate: (VB.(completed: LoadCompleted) -> Unit)? = null,
    onUpdate: (VB.(completed: LoadCompleted) -> Unit)? = null,
): LoadViewScope<View>.() -> Unit = {
    onCreateView { parent ->
        val inflater = LayoutInflater.from(parent.context)
        val binding = inflate(inflater, parent, false)
        binding.root.setTag(R.id.tag_view_binding, binding)
        onCreate?.invoke(binding, this)
        binding.root
    }
    if (onUpdate != null) {
        onUpdateView { root ->
            @Suppress("UNCHECKED_CAST")
            val binding = requireNotNull(
                value = root.getTag(R.id.tag_view_binding) as? VB,
                lazyMessage = { "root还未关联ViewBinding" }
            )
            onUpdate(binding, this)
        }
    }
}