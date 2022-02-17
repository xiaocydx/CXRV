package com.xiaocydx.recycler.paging

import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.xiaocydx.recycler.marker.RvDslMarker

/**
 * 加载视图的构建作用域
 */
@RvDslMarker
sealed class LoadViewScope<V : View> {
    private var collector: PagingCollector<*>? = null
    private var onCreateView: OnCreateLoadView<V>? = null

    @PublishedApi
    internal var onBindView: OnBindLoadViewWith<V>? = null

    fun onCreateView(block: OnCreateLoadView<V>) {
        onCreateView = block
    }

    internal fun setCollector(collector: PagingCollector<*>) {
        this.collector = collector
    }

    internal fun getViewItem(): LoadViewItem<V>? {
        if (collector == null || onCreateView == null) {
            return null
        }
        return LoadViewItem(collector!!, onCreateView!!, onBindView)
    }

    class Normal<V : View>
    @PublishedApi internal constructor() : LoadViewScope<V>() {
        inline fun onBindView(crossinline block: OnBindLoadView<V>) {
            onBindView = { view, _ -> block(this, view) }
        }
    }

    class Exception<V : View>
    @PublishedApi internal constructor() : LoadViewScope<V>() {
        inline fun onBindView(crossinline block: OnBindLoadViewWithException<V>) {
            onBindView = { view, exception -> exception?.let { block(this, view, it) } }
        }
    }
}

typealias OnCreateLoadView<V> = PagingCollector<*>.(parent: ViewGroup) -> V
typealias OnBindLoadView<V> = PagingCollector<*>.(view: V) -> Unit
typealias OnBindLoadViewWithException<V> = PagingCollector<*>.(view: V, exception: Throwable) -> Unit
internal typealias OnBindLoadViewWith<V> = PagingCollector<*>.(view: V, exception: Throwable?) -> Unit

/**
 * 加载视图item
 */
internal data class LoadViewItem<V : View>(
    private val collector: PagingCollector<*>,
    private val onCreateView: OnCreateLoadView<V>,
    private val onBindView: OnBindLoadViewWith<V>?
) {
    private var view: V? = null

    fun setVisible(
        parent: ViewGroup,
        isVisible: Boolean,
        exception: Throwable?
    ) {
        val firstVisible = isVisible && view == null
        if (firstVisible) {
            view = onCreateView
                .invoke(collector, parent)
                .also(parent::addView)
        }

        val view = view ?: return
        if (view.isVisible == isVisible && !firstVisible) {
            return
        }
        view.isVisible = isVisible
        if (isVisible) {
            onBindView?.invoke(collector, view, exception)
        }
    }
}