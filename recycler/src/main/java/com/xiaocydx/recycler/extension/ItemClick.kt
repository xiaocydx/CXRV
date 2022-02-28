package com.xiaocydx.recycler.extension

import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
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
 * [target]默认为`itemView`，若返回`null`则表示不触发点击：
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
            holder.finalTargetView(holder.target(), event)
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
 * [target]默认为`itemView`，若返回`null`则表示不触发长按：
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
            holder.finalTargetView(holder.target(), event)
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
 * [target]默认为`itemView`，若返回`null`则表示不触发点击：
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
 * [target]默认为`itemView`，若返回`null`则表示不触发长按：
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
 * [target]默认为`itemView`，若返回`null`则表示不触发点击：
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
 * [target]默认为`itemView`，若返回`null`则表示不触发长按：
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
internal fun ViewHolder.finalTargetView(targetView: View?, event: MotionEvent): View? = when {
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

@PublishedApi
internal class ItemClickDispatcher(
    private val rv: RecyclerView
) : SimpleOnItemTouchListener(), OnClickListener, OnLongClickListener {
    private var dispatchTargets: ArrayList<DispatchTarget>? = null
    private var pendingClickTargets: LinkedList<DispatchTarget>? = null
    private var pendingLongClickTargets: LinkedList<DispatchTarget>? = null

    init {
        rv.addOnItemTouchListener(this)
    }

    fun addItemClick(
        targetView: (itemView: View, event: MotionEvent) -> View?,
        clickHandler: (itemView: View) -> Unit
    ): Disposable = DispatchTargetObserver(
        dispatcher = this,
        target = DispatchTarget.Click(targetView, clickHandler)
    )

    fun addLongItemClick(
        targetView: (itemView: View, event: MotionEvent) -> View?,
        clickHandler: (itemView: View) -> Boolean
    ): Disposable = DispatchTargetObserver(
        dispatcher = this,
        target = DispatchTarget.LongClick(targetView, clickHandler)
    )

    override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) {
            return false
        }
        val itemView = rv.findChildViewUnder(event.x, event.y) ?: return false
        clearPendingClickTargets()
        clearPendingLongClickTargets()
        dispatchTargets?.accessEach {
            if (!it.findClickTarget(itemView, event)) {
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

    override fun onClick(view: View) {
        val itemView = rv.findContainingItemView(view) ?: return
        pendingClickTargets?.forEach { it.performClick(view, itemView) }
        clearPendingClickTargets()
    }

    override fun onLongClick(view: View): Boolean {
        val itemView = rv.findContainingItemView(view) ?: return false
        val targets = pendingLongClickTargets ?: return false
        var consumed = false
        for (target in targets) {
            consumed = target.performLongClick(view, itemView)
            if (consumed) {
                break
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
            pendingClickTargets = LinkedList()
        }
        pendingClickTargets!!.add(target)
    }

    private fun addPendingLongClickTarget(target: DispatchTarget) {
        if (pendingLongClickTargets == null) {
            pendingLongClickTargets = LinkedList()
        }
        pendingLongClickTargets!!.add(target)
    }

    private fun clearPendingClickTargets() {
        pendingClickTargets.takeIf {
            !it.isNullOrEmpty()
        }?.let { targets ->
            targets.forEach { it.clearListener() }
            targets.clear()
        }
    }

    private fun clearPendingLongClickTargets() {
        pendingLongClickTargets.takeIf {
            !it.isNullOrEmpty()
        }?.let { targets ->
            targets.forEach { it.clearListener() }
            targets.clear()
        }
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

    private class DispatchTarget private constructor(
        private val targetView: (itemView: View, event: MotionEvent) -> View?,
        private val clickHandler: ((itemView: View) -> Unit)? = null,
        private val longClickHandler: ((itemView: View) -> Boolean)? = null
    ) : View.OnAttachStateChangeListener {
        private var clickTarget: View? = null

        fun findClickTarget(itemView: View, event: MotionEvent): Boolean {
            val targetView = targetView(itemView, event)
            clickTarget?.removeOnAttachStateChangeListener(this)
            targetView?.addOnAttachStateChangeListener(this)
            clickTarget = targetView
            return targetView != null
        }

        fun setOnClickListener(listener: OnClickListener): Boolean {
            if (clickHandler == null || clickTarget == null) {
                return false
            }
            clickTarget!!.setOnClickListener(listener)
            return true
        }

        fun setOnLongClickListener(listener: OnLongClickListener): Boolean {
            if (longClickHandler == null || clickTarget == null) {
                return false
            }
            clickTarget!!.setOnLongClickListener(listener)
            return true
        }

        fun performClick(view: View, itemView: View) {
            if (view == clickTarget) {
                clickHandler?.invoke(itemView)
            }
            clearListener()
        }

        fun performLongClick(view: View, itemView: View): Boolean {
            var consumed = false
            if (view == clickTarget && longClickHandler != null) {
                consumed = longClickHandler.invoke(itemView)
            }
            clearListener()
            return consumed
        }

        fun clearListener() {
            val view = clickTarget ?: return
            if (clickHandler != null) {
                view.setOnClickListener(null)
            } else if (longClickHandler != null) {
                view.setOnLongClickListener(null)
            }
            view.removeOnAttachStateChangeListener(this)
            clickTarget = null
        }

        override fun onViewAttachedToWindow(v: View?) {

        }

        override fun onViewDetachedFromWindow(v: View?) {
            clearListener()
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