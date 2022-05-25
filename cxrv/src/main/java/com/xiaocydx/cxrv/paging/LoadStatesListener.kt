package com.xiaocydx.cxrv.paging

import androidx.annotation.MainThread
import com.xiaocydx.cxrv.extension.Disposable
import com.xiaocydx.cxrv.extension.runOnMainThread

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
@MainThread
fun PagingCollector<*>.doOnLoadStatesChanged(
    once: Boolean = false,
    handler: LoadStatesListener
): Disposable = LoadStatesObserver(this, handler, once)

/**
 * 可废弃的加载状态集合观察者
 */
private class LoadStatesObserver(
    collector: PagingCollector<*>,
    handler: LoadStatesListener,
    private val once: Boolean
) : LoadStatesListener, Disposable {
    private var collector: PagingCollector<*>? = collector
    private var handler: LoadStatesListener? = handler
    override val isDisposed: Boolean
        get() = collector == null && handler == null

    init {
        collector.addLoadStatesListener(this)
    }

    override fun onLoadStatesChanged(previous: LoadStates, current: LoadStates) {
        handler?.let {
            if (once) {
                dispose()
            }
            it.onLoadStatesChanged(previous, current)
        }
    }

    override fun dispose() = runOnMainThread {
        collector?.removeLoadStatesListener(this)
        collector = null
        handler = null
    }
}