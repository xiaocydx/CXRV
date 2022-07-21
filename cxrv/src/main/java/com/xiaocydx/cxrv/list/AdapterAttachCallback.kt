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

    fun onAttachedToRecyclerView(recyclerView: RecyclerView): Unit = Unit

    fun onDetachedFromRecyclerView(recyclerView: RecyclerView): Unit = Unit
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

@PublishedApi
internal class AdapterAttachDisposable : AdapterAttachCallback, Disposable {
    private var adapter: ListAdapter<*, *>? = null
    private var delegate: ViewTypeDelegate<*, *>? = null
    private var handler: ((RecyclerView) -> Unit)? = null
    override val isDisposed: Boolean
        get() = adapter == null && handler == null

    fun attach(
        adapter: ListAdapter<*, *>,
        handler: (RecyclerView) -> Unit
    ): Disposable {
        dispose()
        this.adapter = adapter
        this.handler = handler
        adapter.addAdapterAttachCallback(this)
        return this
    }

    fun attach(
        delegate: ViewTypeDelegate<*, *>,
        handler: (RecyclerView) -> Unit
    ): Disposable {
        dispose()
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