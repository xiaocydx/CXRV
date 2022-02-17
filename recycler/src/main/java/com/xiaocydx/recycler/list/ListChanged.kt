package com.xiaocydx.recycler.list

import androidx.annotation.MainThread
import com.xiaocydx.recycler.extension.Disposable
import com.xiaocydx.recycler.extension.assertMainThread
import com.xiaocydx.recycler.extension.runOnMainThread

/**
 * 列表已更改的监听
 */
fun interface ListChangedListener<in T : Any> {

    /**
     * 列表已更改，此时列表数据已修改、列表更新操作已执行
     *
     * @param current 当前的列表
     */
    fun onListChanged(current: List<T>)
}

/**
 * 添加列表已更改的处理程序，可用于视图状态更新场景
 *
 * @param once    为true表示调用一次[handler]后就移除。
 * @param handler 被调用时，表示数据修改、差异计算已完成，并且已触发列表更新操作。
 * @return 调用[Disposable.dispose]可以移除[handler]。
 */
@MainThread
fun <T : Any> ListAdapter<T, *>.doOnListChanged(
    once: Boolean = false,
    handler: ListChangedListener<T>
): Disposable = ListChangedObserver(this, handler, once)

/**
 * 可废弃的列表更改观察者
 */
private class ListChangedObserver<T : Any>(
    adapter: ListAdapter<T, *>,
    handler: ListChangedListener<T>,
    private val once: Boolean
) : ListChangedListener<T>, Disposable {
    private var adapter: ListAdapter<T, *>? = adapter
    private var handler: ListChangedListener<T>? = handler
    override val isDisposed: Boolean
        get() = adapter == null && handler == null

    init {
        assertMainThread()
        adapter.addListChangedListener(this)
    }

    override fun onListChanged(current: List<T>) {
        handler?.let {
            if (once) {
                dispose()
            }
            it.onListChanged(current)
        }
    }

    override fun dispose() = runOnMainThread {
        adapter?.removeListChangedListener(this)
        adapter = null
        handler = null
    }
}