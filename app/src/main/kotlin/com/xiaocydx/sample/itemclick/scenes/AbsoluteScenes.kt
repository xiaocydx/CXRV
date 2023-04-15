package com.xiaocydx.sample.itemclick.scenes

import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.Disposable

/**
 * 通过[RecyclerView]设置点击和长按`action`
 *
 * @author xcc
 * @date 2023/3/25
 */
class AbsoluteScenes : ItemClickScenes() {

    /**
     * 设置点击和长按`action`，其它设置方式在此基础上实现
     *
     * 该设置方式提供`ViewHolder`和`absoluteAdapterPosition`，返回[Disposable]，
     * 当切换[ItemClickScenes]时，会调用[Disposable.dispose]移除点击和长按`action`。
     */
    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    override fun setup(rv: RecyclerView, sub1: Sub, sub2: Sub) {
        rv.doOnItemClick { holder, position ->
            rv.toast("点击", position)
        }.autoDispose()

        rv.doOnLongItemClick { holder, position ->
            rv.toast("长按", position)
            true
        }.autoDispose()
    }

    private fun RecyclerView.toast(action: String, position: Int) {
        toast("$text\n" +
                "$action itemView\n" +
                "absoluteAdapterPosition = $position")
    }
}