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

package com.xiaocydx.cxrv.list

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.multitype.ViewTypeDelegate

/**
 * 适配器Attach相关的回调
 *
 * @author xcc
 * @date 2021/11/16
 */
interface AdapterAttachCallback {

    fun onAttachedToRecyclerView(recyclerView: RecyclerView) = Unit

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView) = Unit
}

/**
 * 在适配器附加到RecyclerView上时，调用一次[handler]
 */
inline fun ListAdapter<*, *>.doOnAttach(crossinline handler: (RecyclerView) -> Unit): Disposable {
    recyclerView?.apply(handler)?.let { return emptyDisposable() }
    return AdapterAttachDisposable().attach(this) { handler(it) }
}

/**
 * 在适配器附加到RecyclerView上时，调用一次[handler]
 */
inline fun ViewTypeDelegate<*, *>.doOnAttach(crossinline handler: (RecyclerView) -> Unit): Disposable {
    return _adapter?.doOnAttach(handler) ?: AdapterAttachDisposable().attach(this) { handler(it) }
}

/**
 * 当适配器附加到RecyclerView上时，调用[handler]获取[Disposable]，
 * 当适配器从RecyclerView上分离时，调用[Disposable.dispose]执行废弃操作。
 */
@PublishedApi
internal fun ListAdapter<*, *>.repeatOnAttach(handler: (RecyclerView) -> Disposable): Disposable {
    return RepeatOnAttachDisposable().attach(this, handler)
}

@PublishedApi
internal class AdapterAttachDisposable : AdapterAttachCallback, Disposable {
    private var adapter: ListAdapter<*, *>? = null
    private var delegate: ViewTypeDelegate<*, *>? = null
    private var handler: ((RecyclerView) -> Unit)? = null
    override val isDisposed: Boolean
        get() = handler == null

    fun attach(
        adapter: ListAdapter<*, *>,
        handler: (RecyclerView) -> Unit
    ): Disposable {
        this.adapter = adapter
        this.handler = handler
        adapter.addAdapterAttachCallback(this)
        return this
    }

    fun attach(
        delegate: ViewTypeDelegate<*, *>,
        handler: (RecyclerView) -> Unit
    ): Disposable {
        this.delegate = delegate
        this.handler = handler
        delegate.addAdapterAttachCallback(this)
        return this
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        val handler = handler ?: return
        dispose()
        handler(recyclerView)
    }

    override fun dispose() {
        adapter?.removeAdapterAttachCallback(this)
        delegate?.removeAdapterAttachCallback(this)
        adapter = null
        delegate = null
        handler = null
    }
}

private class RepeatOnAttachDisposable : AdapterAttachCallback, Disposable {
    private var adapter: ListAdapter<*, *>? = null
    private var handler: ((RecyclerView) -> Disposable)? = null
    private var disposable: Disposable? = null
    override val isDisposed: Boolean
        get() = handler == null

    fun attach(
        adapter: ListAdapter<*, *>,
        handler: (RecyclerView) -> Disposable
    ): Disposable {
        this.adapter = adapter
        this.handler = handler
        adapter.addAdapterAttachCallback(this)
        return this
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        val handler = handler ?: return
        disposable = handler(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        disposable?.dispose()
        disposable = null
    }

    override fun dispose() {
        adapter?.removeAdapterAttachCallback(this)
        disposable?.dispose()
        adapter = null
        handler = null
        disposable = null
    }
}