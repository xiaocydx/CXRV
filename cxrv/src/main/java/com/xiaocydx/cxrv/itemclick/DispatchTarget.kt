package com.xiaocydx.cxrv.itemclick

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool

/**
 * Item点击分发目标
 *
 * @author xcc
 * @date 2022/4/14
 */
internal sealed class DispatchTarget(
    private val targetView: (itemView: View, event: MotionEvent) -> View?
) : OnAttachStateChangeListener {
    protected var currentTargetView: View? = null
        private set

    fun setCurrentTargetView(itemView: View, event: MotionEvent): Boolean {
        val targetView = targetView(itemView, event)
        if (targetView !== currentTargetView) {
            // 当前目标视图改变，清除之前目标视图的全部监听
            clearCurrentTargetViewListeners()
            targetView?.addOnAttachStateChangeListener(this)
            currentTargetView = targetView
        }
        return targetView != null
    }

    override fun onViewAttachedToWindow(v: View?) = Unit

    /**
     * 当[currentTargetView]从Window上分离时，清除监听，
     * 避免共享[RecycledViewPool]场景出现内存泄漏问题。
     */
    override fun onViewDetachedFromWindow(v: View?) {
        clearCurrentTargetViewListeners()
    }

    private fun clearCurrentTargetViewListeners() {
        val current = currentTargetView ?: return
        when (this) {
            is ClickDispatchTarget -> current.setOnClickListener(null)
            is LongClickDispatchTarget -> current.setOnLongClickListener(null)
        }
        current.removeOnAttachStateChangeListener(this)
        currentTargetView = null
    }
}

internal class ClickDispatchTarget(
    private val intervalMs: Long,
    targetView: (itemView: View, event: MotionEvent) -> View?,
    private val clickHandler: (itemView: View) -> Unit,
) : DispatchTarget(targetView) {
    private var lastPerformUptimeMs = 0L

    /**
     * 将[listener]设置给[currentTargetView]，成功则返回`true`，失败则返回`false`
     */
    fun setOnClickListener(listener: OnClickListener): Boolean {
        currentTargetView?.setOnClickListener(listener) ?: return false
        return true
    }

    /**
     * 若[view]等于[currentTargetView]且满足执行间隔，则执行[clickHandler]
     */
    fun tryPerformClickHandler(view: View, itemView: View) {
        if (view === currentTargetView && checkPerformInterval()) {
            clickHandler.invoke(itemView)
        }
    }

    private fun checkPerformInterval(): Boolean {
        val performUptimeMs = SystemClock.uptimeMillis()
        return if (lastPerformUptimeMs + intervalMs < performUptimeMs) {
            lastPerformUptimeMs = performUptimeMs
            true
        } else false
    }
}

internal class LongClickDispatchTarget(
    targetView: (itemView: View, event: MotionEvent) -> View?,
    private val longClickHandler: (itemView: View) -> Boolean,
) : DispatchTarget(targetView) {

    /**
     * 将[listener]设置给[currentTargetView]，成功则返回`true`，失败则返回`false`
     */
    fun setOnLongClickListener(listener: OnLongClickListener): Boolean {
        currentTargetView?.setOnLongClickListener(listener) ?: return false
        return true
    }

    /**
     * 若[view]等于[currentTargetView]，则执行[longClickHandler]，
     * 返回`true`表示执行了[longClickHandler]，并消费了长按，不触发点击，
     * 返回`false`表示未执行[longClickHandler]，或者执行了但不消费长按。
     */
    fun tryPerformLongClickHandler(view: View, itemView: View): Boolean {
        if (view === currentTargetView) {
            return longClickHandler.invoke(itemView)
        }
        return false
    }
}