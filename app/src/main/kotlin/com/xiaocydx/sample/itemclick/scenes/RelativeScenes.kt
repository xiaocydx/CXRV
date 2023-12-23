package com.xiaocydx.sample.itemclick.scenes

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.Disposable
import com.xiaocydx.cxrv.list.getItem
import com.xiaocydx.sample.common.TextItem

/**
 * 通过[RecyclerView]设置匹配`bindingAdapter`的点击和长按`action`
 *
 * @author xcc
 * @date 2023/3/25
 */
class RelativeScenes : ItemClickScenes() {

    /**
     * 设置匹配`bindingAdapter`的点击和长按`action`，其它设置方式在此基础上实现
     *
     * 该设置方式提供`ViewHolder`和`bindingAdapterPosition`，返回[Disposable]，
     * 当切换[ItemClickScenes]时，会调用[Disposable.dispose]移除点击和长按`action`。
     */
    override fun setup(rv: RecyclerView, sub1: Sub, sub2: Sub) {
        sub1.setup(rv)
        sub2.setup(rv)
    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun Sub.setup(rv: RecyclerView) {
        rv.doOnItemClick(listAdapter) { holder, position ->
            show("点击", "itemView", position, getItem(position))
        }.autoDispose()
        rv.doOnLongItemClick(listAdapter) { holder, position ->
            show("长按", "itemView", position, getItem(position))
            true
        }.autoDispose()

        rv.doOnItemClick(listAdapter, target = { targetView }) { holder, position ->
            show("点击", "targetView", position, getItem(position))
        }.autoDispose()
        rv.doOnLongItemClick(listAdapter, target = { targetView }) { holder, position ->
            show("长按", "targetView", position, getItem(position))
            true
        }.autoDispose()
    }

    private fun Sub.show(action: String, view: String, position: Int, item: TextItem) {
        show("""
            |   $text
            |   $action $view
            |   item.text = ${item.text}
            |   bindingAdapter = ListAdapter${num}
            |   bindingAdapterPosition = $position
        """.trimMargin())
    }
}