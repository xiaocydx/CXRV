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

package com.xiaocydx.cxrv.paging

import com.xiaocydx.cxrv.list.Disposable

/**
 * 加载状态集合的监听
 */
fun interface LoadStatesListener {

    /**
     * 加载状态集合已更改
     *
     * @param previous 之前的加载状态集合
     * @param current  当前的加载状态集合
     */
    fun onLoadStatesChanged(previous: LoadStates, current: LoadStates)
}

/**
 * 添加加载状态集合已更改的处理程序
 *
 * @param once 为true表示调用一次[handler]后就移除
 * @return 调用[Disposable.dispose]可以移除[handler]
 */
fun PagingCollector<*>.doOnLoadStatesChanged(
    once: Boolean = false,
    handler: LoadStatesListener
): Disposable = LoadStatesChangedDisposable(once).attach(this, handler)

/**
 * 可废弃的加载状态集合观察者
 */
private class LoadStatesChangedDisposable(
    private val once: Boolean
) : LoadStatesListener, Disposable {
    private var collector: PagingCollector<*>? = null
    private var handler: LoadStatesListener? = null
    override val isDisposed: Boolean
        get() = collector == null && handler == null

    fun attach(
        collector: PagingCollector<*>,
        handler: LoadStatesListener,
    ): Disposable {
        this.collector = collector
        this.handler = handler
        collector.addLoadStatesListener(this)
        return this
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        val handler = handler ?: return
        if (once) dispose()
        handler.onLoadStatesChanged(previous, current)
    }

    override fun dispose() {
        collector?.removeLoadStatesListener(this)
        collector = null
        handler = null
    }
}