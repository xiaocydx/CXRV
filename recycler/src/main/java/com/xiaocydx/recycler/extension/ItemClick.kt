package com.xiaocydx.recycler.extension

import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnAttachStateChangeListener
import android.view.View.OnLongClickListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.*
import androidx.recyclerview.widget.isTouched
import com.xiaocydx.recycler.R
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.doOnAttach
import com.xiaocydx.recycler.list.getItem
import com.xiaocydx.recycler.multitype.ViewTypeDelegate
import java.util.*
import kotlin.collections.ArrayList

/**
 * 若触发点击的[target]的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[block]
 *
 * [block]的Receiver为[adapter]，当[block]被调用时，可以根据[adapter]自身特性获取item：
 * ```
 * recyclerView.doOnItemClick(
 *     adapter = adapter,
 *     target = { targetView }
 * ) { holder, position ->
 *     val item = getItem(position)
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发点击：
 * ```
 * recyclerView.doOnItemClick(adapter) { holder, position ->
 *     val item = getItem(position)
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 */
inline fun <AdapterT, VH, RV> RV.doOnItemClick(
    adapter: AdapterT,
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: AdapterT.(holder: VH, position: Int) -> Unit
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    itemClickDispatcher.addItemClick(
        targetView = view@{ itemView, event ->
            val holder = adapter.getValidViewHolder(itemView) ?: return@view null
            holder.optimizeTargetView(holder.target(), event)
        },
        clickHandler = handler@{ itemView ->
            val holder = adapter.getValidViewHolder(itemView) ?: return@handler
            adapter.block(holder, holder.bindingAdapterPosition)
        }
    )
    return this
}

/**
 * 若触发长按的[target]的[ViewHolder.mBindingAdapter]跟[adapter]相同，则调用[block]
 *
 * * [block]返回`true`表示消费了长按，松手时不会触发点击。
 * * [block]的Receiver为[adapter]，当[block]被调用时，可以根据[adapter]自身特性获取item：
 * ```
 * recyclerView.doOnLongItemClick(
 *     adapter = adapter,
 *     target = { targetView }
 * ) { holder, position ->
 *     val item = getItem(position)
 *     true
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发长按：
 * ```
 * recyclerView.doOnLongItemClick(adapter) { holder, position ->
 *     val item = getItem(position)
 *     true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 */
inline fun <AdapterT, VH, RV> RV.doOnLongItemClick(
    adapter: AdapterT,
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: AdapterT.(holder: VH, position: Int) -> Boolean
): RV
    where AdapterT : Adapter<out VH>, VH : ViewHolder, RV : RecyclerView {
    itemClickDispatcher.addLongItemClick(
        targetView = view@{ itemView, event ->
            val holder = adapter.getValidViewHolder(itemView) ?: return@view null
            holder.optimizeTargetView(holder.target(), event)
        },
        clickHandler = handler@{ itemView ->
            val holder = adapter.getValidViewHolder(itemView) ?: return@handler false
            adapter.block(holder, holder.bindingAdapterPosition)
        }
    )
    return this
}

/**
 * 若触发了[target]的点击，则调用[block]
 *
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发点击：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnItemClick { holder, item ->
 *     ...
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnItemClick(adapter = this, target) { holder, position ->
            block(holder, getItem(position))
        }
    }
    return this
}

/**
 * [ListAdapter.doOnItemClick]的简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnSimpleItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo) {
 *     ...
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick { _, item -> block(item) }
}

/**
 * 若触发了[target]的长按，则调用[block]
 *
 * [block]返回`true`表示消费了长按，松手时不会触发点击
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnLongItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 *     true
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发长按：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnLongItemClick { holder, item ->
 *     ...
 *     true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnLongItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv ->
        rv.doOnLongItemClick(adapter = this, target) { holder, position ->
            block(holder, getItem(position))
        }
    }
    return this
}

/**
 * [ListAdapter.doOnLongItemClick]的简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用：
 * ```
 * val adapter: ListAdapter<Foo, *> = ...
 * adapter.doOnSimpleLongItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo): Boolean {
 *     ...
 *     return true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 */
inline fun <AdapterT, ITEM, VH> AdapterT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): AdapterT
    where AdapterT : ListAdapter<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}

/**
 * 若触发了[target]的点击，则调用[block]
 *
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发点击：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnItemClick { holder, item ->
 *     ...
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnClickListener]。
 */
@Suppress("UNCHECKED_CAST")
inline fun <DelegateT, ITEM, VH> DelegateT.doOnItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttachAdapter { adapter ->
        adapter.doOnItemClick(
            target = {
                if (this.itemViewType == viewType) {
                    target(this as VH)
                } else null
            },
            block = { holder, item ->
                if (holder.itemViewType == viewType) {
                    block(holder as VH, item as ITEM)
                }
            }
        )
    }
    return this
}

/**
 * [ViewTypeDelegate.doOnItemClick]的[block]简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleItemClick]和函数引用结合使用：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnSimpleItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo) {
 *     ...
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnClickListener]。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleItemClick(
    crossinline block: (item: ITEM) -> Unit
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnItemClick { _, item -> block(item) }
}

/**
 * 若触发了[target]的长按，则调用[block]
 *
 * [block]返回`true`表示消费了长按，松手时不会触发点击
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnLongItemClick(
 *     target = { targetView }
 * ) { holder, item ->
 *     ...
 *     true
 * }
 * ```
 *
 * [target]默认返回`itemView`，若返回`null`则表示不触发长按：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnLongItemClick { holder, item ->
 *     ...
 *     true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖[target]已经设置的[OnLongClickListener]。
 */
@Suppress("UNCHECKED_CAST")
inline fun <DelegateT, ITEM, VH> DelegateT.doOnLongItemClick(
    crossinline target: VH.() -> View? = { itemView },
    crossinline block: (holder: VH, item: ITEM) -> Boolean
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    doOnAttachAdapter { adapter ->
        adapter.doOnLongItemClick(
            target = {
                if (this.itemViewType == viewType) {
                    target(this as VH)
                } else null
            },
            block = block@{ holder, item ->
                if (holder.itemViewType == viewType) {
                    return@block block(holder as VH, item as ITEM)
                }
                false
            }
        )
    }
    return this
}

/**
 * [ViewTypeDelegate.doOnLongItemClick]的简洁版本
 *
 * 业务场景只关注item，可以将[doOnSimpleLongItemClick]和函数引用结合使用：
 * ```
 * val delegate: ViewTypeDelegate<Foo, *> = ...
 * delegate.doOnSimpleLongItemClick(::forwardFooDetail)
 *
 * fun forwardFooDetail(item: Foo): Boolean {
 *     ...
 *     return true
 * }
 * ```
 * **注意**：[block]的逻辑会覆盖`itemView`已经设置的[OnLongClickListener]。
 */
inline fun <DelegateT, ITEM, VH> DelegateT.doOnSimpleLongItemClick(
    crossinline block: (item: ITEM) -> Boolean
): DelegateT
    where DelegateT : ViewTypeDelegate<out ITEM, out VH>, ITEM : Any, VH : ViewHolder {
    return doOnLongItemClick { _, item -> block(item) }
}

@PublishedApi
@Suppress("UNCHECKED_CAST")
internal fun <VH : ViewHolder> Adapter<VH>.getValidViewHolder(itemView: View): VH? {
    val parent = itemView.parent as? RecyclerView ?: return null
    val holder = parent.getChildViewHolder(itemView) ?: return null
    return if (holder.bindingAdapter == this) holder as VH else null
}

@PublishedApi
internal fun ViewHolder.optimizeTargetView(targetView: View?, event: MotionEvent): View? = when {
    targetView == itemView -> targetView
    targetView?.isTouched(event.rawX, event.rawY) == true -> targetView
    else -> null
}

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
 * 因此遍历待处理集合，调用[DispatchTarget.tryPerformClick]或者[DispatchTarget.tryPerformLongClick]。
 */
@PublishedApi
internal class ItemClickDispatcher(
    private val rv: RecyclerView
) : SimpleOnItemTouchListener(), OnClickListener, OnLongClickListener {
    private var dispatchTargets: ArrayList<DispatchTarget>? = null
    private var pendingClickTargets: ArrayList<DispatchTarget>? = null
    private var pendingLongClickTargets: ArrayList<DispatchTarget>? = null

    init {
        rv.addOnItemTouchListener(this)
    }

    /**
     * 添加点击分发目标
     *
     * @param targetView   返回需要触发点击的目标视图
     * @param clickHandler 触发目标视图点击时的执行程序
     * @return 调用[Disposable.dispose]移除添加的点击分发目标
     */
    fun addItemClick(
        targetView: (itemView: View, event: MotionEvent) -> View?,
        clickHandler: (itemView: View) -> Unit
    ): Disposable = DispatchTargetObserver(
        dispatcher = this,
        target = DispatchTarget.Click(targetView, clickHandler)
    )

    /**
     * 添加长按分发目标
     *
     * @param targetView   返回需要触发长按的目标视图
     * @param clickHandler 触发目标视图长按时的执行程序，返回`true`表示消费了长按，松手时不会触发点击
     * @return 调用[Disposable.dispose]移除添加的长按分发目标
     */
    fun addLongItemClick(
        targetView: (itemView: View, event: MotionEvent) -> View?,
        clickHandler: (itemView: View) -> Boolean
    ): Disposable = DispatchTargetObserver(
        dispatcher = this,
        target = DispatchTarget.LongClick(targetView, clickHandler)
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
        dispatchTargets?.accessEach {
            if (!it.setCurrentTargetView(itemView, event)) {
                // continue
                return@accessEach
            }
            if (it.setOnClickListener(this)) {
                addPendingClickTarget(it)
                // continue
                return@accessEach
            }
            if (it.setOnLongClickListener(this)) {
                addPendingLongClickTarget(it)
            }
        }
        return false
    }

    /**
     * 触发了点击，说明[pendingClickTargets]中有符合条件的[DispatchTarget]
     */
    override fun onClick(view: View) {
        val itemView = rv.findContainingItemView(view) ?: return
        pendingClickTargets?.accessEach { it.tryPerformClick(view, itemView) }
        clearPendingClickTargets()
    }

    /**
     * 触发了长按，说明[pendingLongClickTargets]中有符合条件的[DispatchTarget]
     */
    override fun onLongClick(view: View): Boolean {
        val itemView = rv.findContainingItemView(view) ?: return false
        var consumed = false
        pendingLongClickTargets?.accessEach {
            if (it.tryPerformLongClick(view, itemView)) {
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

    private fun addPendingClickTarget(target: DispatchTarget) {
        if (pendingClickTargets == null) {
            pendingClickTargets = ArrayList(2)
        }
        pendingClickTargets!!.add(target)
    }

    private fun addPendingLongClickTarget(target: DispatchTarget) {
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

    /**
     * 分发目标
     *
     * ### 分发目标类型
     * [DispatchTarget]只包含[clickHandler]和[longClickHandler]的其中一个，
     * [DispatchTarget.Click]创建点击分发目标，[DispatchTarget.LongClick]创建长按分发目标。
     *
     * ### 分发目标设置
     * 1.触摸事件为[ACTION_DOWN]时，调用[setCurrentTargetView]设置[currentTargetView]。
     * 2.调用[setOnClickListener]和[setOnLongClickListener]对[currentTargetView]设置点击、长按监听。
     */
    private class DispatchTarget private constructor(
        private val targetView: (itemView: View, event: MotionEvent) -> View?,
        private val clickHandler: ((itemView: View) -> Unit)? = null,
        private val longClickHandler: ((itemView: View) -> Boolean)? = null
    ) : OnAttachStateChangeListener {
        private var currentTargetView: View? = null

        /**
         * 设置[currentTargetView]
         */
        fun setCurrentTargetView(itemView: View, event: MotionEvent): Boolean {
            val targetView = targetView(itemView, event)
            if (targetView != currentTargetView) {
                // 当前目标视图改变，清除之前目标视图的全部监听
                clearCurrentTargetViewListeners()
                targetView?.addOnAttachStateChangeListener(this)
                currentTargetView = targetView
            }
            return targetView != null
        }

        /**
         * 将[listener]设置给[currentTargetView]，成功则返回`true`，失败则返回`false`
         */
        fun setOnClickListener(listener: OnClickListener): Boolean {
            if (clickHandler == null || currentTargetView == null) {
                return false
            }
            currentTargetView!!.setOnClickListener(listener)
            return true
        }

        /**
         * 将[listener]设置给[currentTargetView]，成功则返回`true`，失败则返回`false`
         */
        fun setOnLongClickListener(listener: OnLongClickListener): Boolean {
            if (longClickHandler == null || currentTargetView == null) {
                return false
            }
            currentTargetView!!.setOnLongClickListener(listener)
            return true
        }

        /**
         * 若[view]等于[currentTargetView]，则执行[clickHandler]
         */
        fun tryPerformClick(view: View, itemView: View) {
            if (view == currentTargetView) {
                clickHandler?.invoke(itemView)
            }
        }

        /**
         * 若[view]等于[currentTargetView]，则执行[longClickHandler]，
         * 返回`true`表示执行了[longClickHandler]，并消费了长按，不触发点击，
         * 返回`false`表示未执行[longClickHandler]，或者执行了但不消费长按。
         */
        fun tryPerformLongClick(view: View, itemView: View): Boolean {
            var consumed = false
            if (view == currentTargetView && longClickHandler != null) {
                consumed = longClickHandler.invoke(itemView)
            }
            return consumed
        }

        override fun onViewAttachedToWindow(v: View?) {

        }

        /**
         * [currentTargetView]从Window上分离时，清除全部监听，
         * 避免共享[RecycledViewPool]场景出现内存泄漏的情况。
         */
        override fun onViewDetachedFromWindow(v: View?) {
            clearCurrentTargetViewListeners()
        }

        private fun clearCurrentTargetViewListeners() {
            val current = currentTargetView ?: return
            if (clickHandler != null) {
                current.setOnClickListener(null)
            } else if (longClickHandler != null) {
                current.setOnLongClickListener(null)
            }
            current.removeOnAttachStateChangeListener(this)
            currentTargetView = null
        }

        @Suppress("FunctionName")
        companion object {
            fun Click(
                targetView: (itemView: View, event: MotionEvent) -> View?,
                clickHandler: (itemView: View) -> Unit
            ): DispatchTarget = DispatchTarget(targetView, clickHandler = clickHandler)

            fun LongClick(
                targetView: (itemView: View, event: MotionEvent) -> View?,
                clickHandler: (itemView: View) -> Boolean
            ): DispatchTarget = DispatchTarget(targetView, longClickHandler = clickHandler)
        }
    }
}