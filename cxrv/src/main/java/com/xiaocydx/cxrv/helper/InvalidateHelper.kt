package com.xiaocydx.cxrv.helper

import android.view.Choreographer
import android.view.Choreographer.FrameCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.extension.doOnPreDraw
import com.xiaocydx.cxrv.extension.hasDisplayItem

/**
 * 列表更新时调用[RecyclerView.invalidateItemDecorations]的帮助类
 *
 * @author xcc
 * @date 2022/4/27
 */
class InvalidateHelper : ListUpdateHelper() {
    private var isInvalid = false
    private var previousEmpty = true
    private val resetInvalid = ResetInvalid()
    private val isStaggered: Boolean
        get() = rv?.layoutManager is StaggeredGridLayoutManager
    private val adapterItemCount: Int
        get() = adapter?.itemCount ?: 0

    override fun register(rv: RecyclerView, adapter: Adapter<*>) {
        super.register(rv, adapter)
        saveDisplayState()
    }

    override fun onChanged() {
        saveDisplayState()
    }

    override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        saveDisplayState()
    }

    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        if (isStaggered) {
            invalidateItemDecorations()
        } else if (positionStart == adapterItemCount - itemCount) {
            // 插入到最后
            invalidateItemDecorations()
        }
        saveDisplayState()
    }

    override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        if (isStaggered) {
            invalidateItemDecorations()
        }
        saveDisplayState()
    }

    override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        if (isStaggered) {
            invalidateItemDecorations()
        }
        saveDisplayState()
    }

    private fun saveDisplayState() {
        val adapter = adapter
        previousEmpty = if (adapter == null) true else !adapter.hasDisplayItem
    }

    private fun invalidateItemDecorations() {
        val rv = rv ?: return
        if (isInvalid || previousEmpty || rv.itemDecorationCount < 1) {
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