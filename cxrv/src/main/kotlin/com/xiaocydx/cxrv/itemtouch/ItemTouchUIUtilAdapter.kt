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

@file:Suppress("PackageDirectoryMismatch")

package androidx.recyclerview.widget

import android.graphics.Canvas
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * [ItemTouchUIUtil]的适配器，默认实现沿用[ItemTouchUIUtilImpl]
 *
 * @author xcc
 * @date 2022/12/10
 */
internal object ItemTouchUIUtilAdapter {

    /**
     * 对应[ItemTouchUIUtil.onDraw]
     */
    fun onDraw(c: Canvas, holder: ViewHolder, dX: Float, dY: Float,
               actionState: Int, isCurrentlyActive: Boolean) {
        val view = holder.itemView
        val recyclerView = view.parent as? RecyclerView ?: return
        ItemTouchUIUtilImpl.INSTANCE.onDraw(c, recyclerView, view, dX, dY, actionState, isCurrentlyActive)
    }

    /**
     * 对应[ItemTouchUIUtil.onDrawOver]
     */
    fun onDrawOver(c: Canvas, holder: ViewHolder, dX: Float, dY: Float,
                   actionState: Int, isCurrentlyActive: Boolean) {
        val view = holder.itemView
        val recyclerView = view.parent as? RecyclerView ?: return
        ItemTouchUIUtilImpl.INSTANCE.onDrawOver(c, recyclerView, view, dX, dY, actionState, isCurrentlyActive)
    }

    /**
     * 对应[ItemTouchUIUtil.clearView]
     */
    fun clearView(holder: ViewHolder) {
        ItemTouchUIUtilImpl.INSTANCE.clearView(holder.itemView)
    }

    /**
     * 对应[ItemTouchUIUtil.onSelected]
     */
    fun onSelected(holder: ViewHolder) {
        ItemTouchUIUtilImpl.INSTANCE.onSelected(holder.itemView)
    }
}