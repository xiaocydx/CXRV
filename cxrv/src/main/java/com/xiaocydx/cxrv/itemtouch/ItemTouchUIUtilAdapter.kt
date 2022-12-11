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