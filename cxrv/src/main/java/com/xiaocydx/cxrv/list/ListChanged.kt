package com.xiaocydx.cxrv.list

/**
 * 列表已更改的监听
 */
fun interface ListChangedListener<in T : Any> {

    /**
     * 列表已更改，此时列表数据修改完成、列表更新操作执行完成
     *
     * @param current 当前的列表
     */
    fun onListChanged(current: List<T>)
}

/**
 * 添加列表已更改的处理程序
 *
 * @param once    为true表示调用一次[handler]后就移除。
 * @param handler 被调用时，表示列表数据修改完成、列表更新操作执行完成。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
fun <T : Any> ListAdapter<T, *>.doOnListChanged(
    once: Boolean = false,
    handler: ListChangedListener<T>
): Disposable = ListChangedDisposable<T>(once).attach(this, handler)

/**
 * 可废弃的列表更改观察者
 */
private class ListChangedDisposable<T : Any>(
    private val once: Boolean
) : ListChangedListener<T>, Disposable {
    private var adapter: ListAdapter<T, *>? = null
    private var handler: ListChangedListener<T>? = null
    override val isDisposed: Boolean
        get() = adapter == null && handler == null

    fun attach(
        adapter: ListAdapter<T, *>,
        handler: ListChangedListener<T>,
    ): Disposable {
        dispose()
        this.adapter = adapter
        this.handler = handler
        adapter.addListChangedListener(this)
        return this
    }

    override fun onListChanged(current: List<T>) {
        val handler = handler ?: return
        if (once) dispose()
        handler.onListChanged(current)
    }

    override fun dispose() {
        adapter?.removeListChangedListener(this)
        adapter = null
        handler = null
    }
}