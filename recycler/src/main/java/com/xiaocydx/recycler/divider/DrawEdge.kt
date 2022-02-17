package com.xiaocydx.recycler.divider

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView.LayoutParams
import kotlin.math.roundToInt

/**
 * 分割线绘制边缘
 *
 * @author xcc
 * @date 2021/10/28
 */
internal class DrawEdge {
    private val bounds: Rect = Rect()
    var left = false
        private set
    var top = false
        private set
    var right = false
        private set
    var bottom = false
        private set

    fun set(left: Boolean, top: Boolean, right: Boolean, bottom: Boolean) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }

    fun reset() {
        bounds.set(0, 0, 0, 0)
        left = false
        top = false
        right = false
        bottom = false
    }

    fun getBounds(child: View, withAnimOffset: Boolean): Rect {
        val lp = child.layoutParams as LayoutParams
        bounds.set(
            child.left - lp.leftMargin,
            child.top - lp.topMargin,
            child.right + lp.rightMargin,
            child.bottom + lp.bottomMargin
        )
        if (withAnimOffset) {
            // 执行默认item动画时会有X/Y偏移
            val translationX = child.translationX.roundToInt()
            val translationY = child.translationY.roundToInt()
            bounds.left += translationX
            bounds.top += translationY
            bounds.right += translationX
            bounds.bottom += translationY
        }
        return bounds
    }
}