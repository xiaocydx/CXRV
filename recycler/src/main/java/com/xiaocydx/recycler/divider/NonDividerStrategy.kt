package com.xiaocydx.recycler.divider

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * 无分割线策略
 *
 * @author xcc
 * @date 2021/10/8
 */
internal object NonDividerStrategy : DividerStrategy {

    override fun getItemOffsets(
        view: View,
        parent: RecyclerView,
        decoration: DividerItemDecoration
    ): Unit = Unit

    override fun onDraw(
        canvas: Canvas,
        parent: RecyclerView,
        decoration: DividerItemDecoration
    ): Unit = Unit
}