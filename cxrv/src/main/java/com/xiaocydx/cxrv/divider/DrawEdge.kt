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

package com.xiaocydx.cxrv.divider

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