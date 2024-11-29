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

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.xiaocydx.cxrv.internal.RvDslMarker
import com.xiaocydx.cxrv.internal.isVisible

/**
 * 加载视图的构建作用域
 *
 * @author xcc
 * @date 2022/5/21
 */
@RvDslMarker
class LoadViewScope<V : View> @PublishedApi internal constructor() {
    private var isCompleted = false
    private var retry: (() -> Unit)? = null
    private var exception: (() -> Throwable?)? = null
    private var onCreateView: OnCreateView<V>? = null
    private var onUpdateView: OnUpdateView<V>? = null
    private var onVisibleChanged: OnVisibleChanged<V>? = null

    /**
     * 重新加载，该函数会对加载状态做判断，避免冗余请求
     */
    fun retry() {
        retry?.invoke()
    }

    /**
     * 返回加载失败的异常，若不是加载失败，则返回`null`
     */
    fun exception(): Throwable? {
        return exception?.invoke()
    }

    /**
     * 首次显示加载视图时，调用[block]
     */
    fun onCreateView(block: OnCreateView<V>) {
        checkCompleted()
        onCreateView = block
    }

    /**
     *  创建加载视图后、隐藏后再次显示、显示期间触发更新，调用[block]
     */
    fun onUpdateView(block: OnUpdateView<V>) {
        checkCompleted()
        onUpdateView = block
    }

    /**
     * 显示、隐藏加载视图时，调用[block]
     */
    fun onVisibleChanged(block: OnVisibleChanged<V>) {
        checkCompleted()
        onVisibleChanged = block
    }

    internal fun complete(retry: () -> Unit, exception: () -> Throwable?) {
        checkCompleted()
        isCompleted = true
        this.retry = retry
        this.exception = exception
    }

    internal fun getViewItem(): LoadViewItem<V>? {
        if (onCreateView == null) return null
        return LoadViewItem(this, onCreateView!!, onUpdateView, onVisibleChanged)
    }

    private fun checkCompleted() {
        check(!isCompleted) { "已完成加载视图的配置" }
    }
}

typealias OnCreateView<V> = LoadViewScope<out V>.(parent: ViewGroup) -> V
typealias OnUpdateView<V> = LoadViewScope<out V>.(view: V) -> Unit
typealias OnVisibleChanged<V> = LoadViewScope<out V>.(view: V, isVisible: Boolean) -> Unit

internal class LoadViewItem<V : View>(
    private val scope: LoadViewScope<V>,
    private val onCreateView: OnCreateView<V>,
    private val onUpdateView: OnUpdateView<V>?,
    private val onVisibleChanged: OnVisibleChanged<V>?,
    private var view: V? = null
) {

    fun setVisible(parent: ViewGroup, isVisible: Boolean) {
        val isFirstVisible = isVisible && view == null
        if (isFirstVisible) {
            view = onCreateView(scope, parent).also(parent::addView)
        }
        val view = view ?: return
        val isVisibleChanged = view.isVisible != isVisible
        if (isFirstVisible || isVisibleChanged) {
            view.isVisible = isVisible
            onVisibleChanged?.invoke(scope, view, isVisible)
        }
        if (isVisible) {
            onUpdateView?.invoke(scope, view)
        }
    }
}

@SuppressLint("ViewConstructor")
internal class LoadViewLayout(
    context: Context,
    width: Int,
    height: Int,
    private val loadingItem: LoadViewItem<out View>?,
    private val successItem: LoadViewItem<out View>?,
    private val failureItem: LoadViewItem<out View>?
) : FrameLayout(context) {

    init {
        layoutParams = ViewGroup.LayoutParams(width, height)
    }

    fun loadingVisible() {
        allInvisible()
        loadingItem?.setVisible(true)
    }

    fun successVisible() {
        allInvisible()
        successItem?.setVisible(true)
    }

    fun failureVisible() {
        allInvisible()
        failureItem?.setVisible(true)
    }

    private fun allInvisible() {
        loadingItem?.setVisible(false)
        successItem?.setVisible(false)
        failureItem?.setVisible(false)
    }

    private fun <V : View> LoadViewItem<V>.setVisible(isVisible: Boolean) {
        setVisible(parent = this@LoadViewLayout, isVisible)
    }
}