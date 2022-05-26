package com.xiaocydx.cxrv.itemclick

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.View.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.cxrv.R
import com.xiaocydx.cxrv.internal.accessEach
import com.xiaocydx.cxrv.internal.assertMainThread
import com.xiaocydx.cxrv.internal.runOnMainThread
import com.xiaocydx.cxrv.list.Disposable
import java.util.*
import kotlin.collections.ArrayList

/**
 * Item点击分发器，支持`itemView`及其子view的点击、长按
 */
@PublishedApi
internal val RecyclerView.itemClickDispatcher: ItemClickDispatcher
    get() {
        var dispatcher: ItemClickDispatcher? =
                getTag(R.id.tag_item_click_dispatcher) as? ItemClickDispatcher
        if (dispatcher == null) {
            dispatcher = ItemClickDispatcher(this)
            setTag(R.id.tag_item_click_dispatcher, dispatcher)
        }
        return dispatcher
    }

/**
 * Item点击分发器，支持`itemView`及其子view的点击、长按
 *
 * 1.当RecyclerView的[onInterceptTouchEvent]的触摸事件为[ACTION_DOWN]时，
 * 找到可能触发点击或者长按的[DispatchTarget]，对其设置点击、长按监听，并添加到待处理集合中。
 * 2.若[onClick]或者[onLongClick]被调用，则说明第1步的待处理集合中有符合条件的[DispatchTarget]，
 * 因此遍历待处理集合，尝试执行点击或者长按的处理程序。
 */
@PublishedApi
internal class ItemClickDispatcher(
    private val rv: RecyclerView
) : SimpleOnItemTouchListener(), OnClickListener, OnLongClickListener {
    private var dispatchTargets: ArrayList<DispatchTarget>? = null
    private var pendingClickTargets: ArrayList<ClickDispatchTarget>? = null
    private var pendingLongClickTargets: ArrayList<LongClickDispatchTarget>? = null

    init {
        rv.addOnItemTouchListener(this)
    }

    /**
     * 添加点击分发目标
     *
     * @param intervalMs   执行[clickHandler]的间隔时间
     * @param targetView   返回需要触发点击的目标视图
     * @param clickHandler 触发目标视图点击时的执行程序
     * @return 调用[Disposable.dispose]移除添加的点击分发目标
     */
    fun addItemClick(
        intervalMs: Long,
        targetView: (itemView: View, event: MotionEvent) -> View?,
        clickHandler: (itemView: View) -> Unit
    ): Disposable = DispatchTargetObserver(
        dispatcher = this,
        target = ClickDispatchTarget(intervalMs, targetView, clickHandler)
    )

    /**
     * 添加长按分发目标
     *
     * @param targetView       返回需要触发长按的目标视图
     * @param longClickHandler 触发目标视图长按时的执行程序，返回`true`表示消费了长按，松手时不会触发点击
     * @return 调用[Disposable.dispose]移除添加的长按分发目标
     */
    fun addLongItemClick(
        targetView: (itemView: View, event: MotionEvent) -> View?,
        longClickHandler: (itemView: View) -> Boolean
    ): Disposable = DispatchTargetObserver(
        dispatcher = this,
        target = LongClickDispatchTarget(targetView, longClickHandler)
    )

    /**
     * 找到可能触发点击或者长按的[DispatchTarget]，对其设置点击、长按监听，并添加到待处理集合中
     */
    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        if (event.actionMasked != ACTION_DOWN) {
            return false
        }
        clearPendingClickTargets()
        clearPendingLongClickTargets()

        val itemView = rv.findChildViewUnder(event.x, event.y) ?: return false
        dispatchTargets?.accessEach action@{
            if (!it.setCurrentTargetView(itemView, event)) {
                return@action
            }
            when {
                it is ClickDispatchTarget && it.setOnClickListener(this) -> {
                    addPendingClickTarget(it)
                }
                it is LongClickDispatchTarget && it.setOnLongClickListener(this) -> {
                    addPendingLongClickTarget(it)
                }
            }
        }
        return false
    }

    /**
     * 触发了点击，说明[pendingClickTargets]中有符合条件的[ClickDispatchTarget]
     */
    override fun onClick(view: View) {
        val itemView = rv.findContainingItemView(view) ?: return
        pendingClickTargets?.accessEach { it.tryPerformClickHandler(view, itemView) }
        clearPendingClickTargets()
    }

    /**
     * 触发了长按，说明[pendingLongClickTargets]中有符合条件的[LongClickDispatchTarget]
     */
    override fun onLongClick(view: View): Boolean {
        val itemView = rv.findContainingItemView(view) ?: return false
        var consumed = false
        pendingLongClickTargets?.accessEach {
            if (it.tryPerformLongClickHandler(view, itemView)) {
                consumed = true
            }
        }
        clearPendingLongClickTargets()
        return consumed
    }

    private fun addDispatchTarget(target: DispatchTarget) {
        if (dispatchTargets == null) {
            dispatchTargets = ArrayList(2)
        }
        if (!dispatchTargets!!.contains(target)) {
            dispatchTargets!!.add(target)
        }
    }

    private fun removeDispatchTarget(target: DispatchTarget) {
        dispatchTargets?.remove(target)
    }

    private fun addPendingClickTarget(target: ClickDispatchTarget) {
        if (pendingClickTargets == null) {
            pendingClickTargets = ArrayList(2)
        }
        pendingClickTargets!!.add(target)
    }

    private fun addPendingLongClickTarget(target: LongClickDispatchTarget) {
        if (pendingLongClickTargets == null) {
            pendingLongClickTargets = ArrayList(2)
        }
        pendingLongClickTargets!!.add(target)
    }

    private fun clearPendingClickTargets() {
        pendingClickTargets.takeIf { !it.isNullOrEmpty() }?.clear()
    }

    private fun clearPendingLongClickTargets() {
        pendingLongClickTargets.takeIf { !it.isNullOrEmpty() }?.clear()
    }

    private class DispatchTargetObserver(
        target: DispatchTarget,
        dispatcher: ItemClickDispatcher
    ) : Disposable {
        private var target: DispatchTarget? = target
        private var dispatcher: ItemClickDispatcher? = dispatcher
        override val isDisposed: Boolean
            get() = target == null

        init {
            assertMainThread()
            dispatcher.addDispatchTarget(target)
        }

        override fun dispose() = runOnMainThread {
            target ?: return@runOnMainThread
            dispatcher?.removeDispatchTarget(target!!)
            target = null
            dispatcher = null
        }
    }
}