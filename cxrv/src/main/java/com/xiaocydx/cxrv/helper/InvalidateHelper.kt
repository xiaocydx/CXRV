package com.xiaocydx.cxrv.helper

import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.internal.doOnPreDraw

/**
 * 列表更新时调用[RecyclerView.invalidateItemDecorations]的帮助类
 *
 * @author xcc
 * @date 2022/4/27
 */
class InvalidateHelper : ListUpdateHelper() {
    private var isInvalid = false
    private val resetInvalid = ResetInvalid()
    private val isStaggered: Boolean
        get() = rv?.layoutManager is StaggeredGridLayoutManager
    private val currentItemCount: Int
        get() = adapter?.itemCount ?: 0

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        when {
            // notifyItemRangeChanged()可以用于首次插入item，这种情况不做处理
            previousItemCount == 0 -> return
            isStaggered -> invalidateItemDecorations()
        }
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        when {
            previousItemCount == 0 -> return
            isStaggered -> invalidateItemDecorations()
            positionStart == currentItemCount - itemCount -> {
                // 插入到最后
                invalidateItemDecorations()
            }
        }
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if (isStaggered) {
            invalidateItemDecorations()
        }
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (isStaggered) {
            invalidateItemDecorations()
        }
    }

    private fun invalidateItemDecorations() {
        val rv = rv ?: return
        if (isInvalid || previousItemCount == 0 || rv.itemDecorationCount < 1) {
            return
        }
        if (isStaggered) {
            // 在下一帧rv布局完成，瀑布流布局的spanIndex准确后，
            // 才调用invalidateItemDecorations()申请下一帧绘制。
            rv.doOnPreDraw { rv.invalidateItemDecorations() }
        } else {
            rv.invalidateItemDecorations()
        }
        // 下一帧重置isInvalid
        isInvalid = true
        Choreographer.getInstance().postFrameCallback(resetInvalid)
    }

    private inner class ResetInvalid : FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            isInvalid = false
        }
    }
}