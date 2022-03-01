package com.xiaocydx.recycler.extension

import android.graphics.Canvas
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.View
import androidx.recyclerview.widget.*
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView.*
import com.xiaocydx.recycler.R
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.doOnAttach
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.list.swapItem
import com.xiaocydx.recycler.marker.RV_HIDE_MARKER
import com.xiaocydx.recycler.marker.RvDslMarker

/**
 * 设置item触摸回调
 *
 * **注意**：[ItemTouchCallback]是[ItemTouchHelper.Callback]的简化类，
 * 仅用于简化一般业务场景的模板代码，若对item触摸效果有更精细的要求，
 * 则自行创建[ItemTouchHelper]，完成[ItemTouchHelper.Callback]的配置。
 */
fun <T : RecyclerView> T.addItemTouchCallback(callback: ItemTouchCallback): T {
    itemTouchDispatcher.addItemTouchCallback(callback)
    return this
}

/**
 * 移除item触摸回调
 */
fun <T : RecyclerView> T.removeItemTouchCallback(callback: ItemTouchCallback): T {
    itemTouchDispatcher.removeItemTouchCallback(callback)
    return this
}

/**
 * item触摸，详细的属性及函数描述[ItemTouchScope]
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     // 拖动时交换item
 *     onDrag { from, to ->
 *         swapItem(from, to)
 *         true
 *     }
 *     // 拖动开始时放大itemView
 *     onSelected { holder ->
 *         holder.itemView.scaleX = 1.1f
 *         holder.itemView.scaleY = 1.1f
 *     ｝
 *     // 拖动结束时恢复itemView
 *     clearView { holder ->
 *         holder.itemView.scaleX = 1.0f
 *         holder.itemView.scaleY = 1.0f
 *     ｝
 * }
 * ```
 */
inline fun <AdapterT, VH, RV> RV.itemTouch(
    adapter: AdapterT,
    block: ItemTouchScope<AdapterT, VH>.() -> Unit
): RV
    where AdapterT : Adapter<VH>, VH : ViewHolder, RV : RecyclerView {
    return addItemTouchCallback(ItemTouchScope(adapter, this).apply(block))
}

/**
 * item触摸，详细的属性及函数描述[ItemTouchScope]
 *
 * ```
 * adapter.itemTouch {
 *     // 拖动时交换item
 *     onDragSwapItem()
 *     // 拖动开始时放大itemView
 *     onSelected { holder ->
 *         holder.itemView.scaleX = 1.1f
 *         holder.itemView.scaleY = 1.1f
 *     ｝
 *     // 拖动结束时恢复itemView
 *     clearView { holder ->
 *         holder.itemView.scaleX = 1.0f
 *         holder.itemView.scaleY = 1.0f
 *     ｝
 * }
 * ```
 */
inline fun <AdapterT, ITEM, VH> AdapterT.itemTouch(
    crossinline block: ItemTouchScope<AdapterT, VH>.() -> Unit
): AdapterT
    where AdapterT : ListAdapter<ITEM, VH>, ITEM : Any, VH : ViewHolder {
    doOnAttach { rv -> rv.itemTouch(this, block) }
    return this
}

/**
 * 拖动时交换item
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     onDragSwapItem()
 * }
 * ```
 */
fun <AdapterT, ITEM, VH> ItemTouchScope<AdapterT, VH>.onDragSwapItem()
    where AdapterT : ListAdapter<ITEM, VH>, ITEM : Any, VH : ViewHolder {
    onDrag { from, to ->
        swapItem(from, to)
        true
    }
}

/**
 * 侧滑时移除item
 *
 * ```
 * val adapter: ListAdapter<*, *> = ...
 * recyclerView.itemTouch(adapter) {
 *     onSwipeRemoveItem()
 * }
 * ```
 */
fun <AdapterT, ITEM, VH> ItemTouchScope<AdapterT, VH>.onSwipeRemoveItem()
    where AdapterT : ListAdapter<ITEM, VH>, ITEM : Any, VH : ViewHolder {
    onSwipe { position, _ -> removeItemAt(position) }
}

private val RecyclerView.itemTouchDispatcher: ItemTouchDispatcher
    get() {
        var dispatcher: ItemTouchDispatcher? =
                getTag(R.id.tag_item_touch_dispatcher) as? ItemTouchDispatcher
        if (dispatcher == null) {
            dispatcher = ItemTouchDispatcher(this)
            setTag(R.id.tag_item_touch_dispatcher, dispatcher)
        }
        return dispatcher
    }

/**
 * [ItemTouchCallback]的分发器
 */
private class ItemTouchDispatcher(
    private val recyclerView: RecyclerView
) : ItemTouchHelper.Callback() {
    private var callbacks: ArrayList<ItemTouchCallback>? = null
    private var intercepting: ItemTouchCallback? = null
    private val touchHelper = ItemTouchHelper(this)
        .apply { attachToRecyclerView(recyclerView) }

    override fun getMovementFlags(rv: RecyclerView, holder: ViewHolder): Int {
        intercepting = callbacks?.findIntercepting(holder)
        return intercepting?.getMovementFlags(holder) ?: return 0
    }

    override fun isLongPressDragEnabled(): Boolean {
        return intercepting?.isLongPressDragEnabled ?: true
    }

    override fun isItemViewSwipeEnabled(): Boolean {
        return intercepting?.isItemViewSwipeEnabled ?: true
    }

    override fun onMove(rv: RecyclerView, holder: ViewHolder, target: ViewHolder): Boolean {
        val callback = intercepting ?: return false
        return if (callback.onIntercept(target)) callback.onDrag(holder, target) else false
    }

    override fun onSwiped(holder: ViewHolder, direction: Int) {
        intercepting?.onSwipe(holder, direction)
    }

    override fun onSelectedChanged(holder: ViewHolder?, actionState: Int) {
        super.onSelectedChanged(holder, actionState)
        if (holder != null) {
            intercepting?.onSelected(holder)
        }
    }

    override fun onChildDraw(
        canvas: Canvas, rv: RecyclerView, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        super.onChildDraw(canvas, rv, holder, dX, dY, actionState, isCurrentlyActive)
        intercepting?.onDraw(canvas, holder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun onChildDrawOver(
        canvas: Canvas, rv: RecyclerView, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        super.onChildDrawOver(canvas, rv, holder, dX, dY, actionState, isCurrentlyActive)
        intercepting?.onDrawOver(canvas, holder, dX, dY, actionState, isCurrentlyActive)
    }

    override fun clearView(rv: RecyclerView, holder: ViewHolder) {
        super.clearView(rv, holder)
        intercepting?.clearView(holder)
    }

    private fun ArrayList<ItemTouchCallback>.findIntercepting(holder: ViewHolder): ItemTouchCallback? {
        accessEach { if (it.onIntercept(holder)) return it }
        return null
    }

    fun addItemTouchCallback(callback: ItemTouchCallback) {
        if (callbacks == null) {
            callbacks = ArrayList(2)
        }
        if (!callbacks!!.contains(callback)) {
            callbacks!!.add(callback)
            callback.attach(touchHelper, recyclerView)
        }
    }

    fun removeItemTouchCallback(callback: ItemTouchCallback) {
        callbacks?.remove(callback)
        if (this.intercepting == callback) {
            this.intercepting = null
        }
    }
}

/**
 * [ItemTouchCallback]是[ItemTouchHelper.Callback]的简化类，
 * 仅定义了业务场景中常用的函数，用于简化模板代码，例如[getDragFlags]、[onDrag]等等，
 * 这些常用函数对应[ItemTouchHelper.Callback]中的同名函数，通过[onIntercept]完成回调分发。
 *
 * [onSelected]、[onDraw]、[clearView]参考自[ItemTouchUIUtil]的命名，可以看作是触摸开始、触摸中、触摸结束的回调。
 */
abstract class ItemTouchCallback {
    var touchHelper: ItemTouchHelper? = null
        private set
    var recyclerView: RecyclerView? = null
        private set
    val LayoutManager.isVertical: Boolean
        get() = if (this is LinearLayoutManager) orientation == VERTICAL else false

    /**
     * 默认移动标志
     */
    val defaultFlags = -1

    /**
     * 禁止移动标志
     */
    val disallowFlags = 0

    /**
     * 对应[Callback.isLongPressDragEnabled]
     */
    open var isLongPressDragEnabled = true

    /**
     * 对应[Callback.isItemViewSwipeEnabled]
     */
    open var isItemViewSwipeEnabled = true

    /**
     * 拦截触摸回调
     *
     * 例如bindingAdapter相同时才拦截触摸回调：
     * ```
     * override fun onIntercept(holder: ViewHolder): Boolean {
     *     return holder.bindingAdapter == adapter
     * }
     * ```
     */
    abstract fun onIntercept(holder: ViewHolder): Boolean

    /**
     * 对应[Callback.getMovementFlags]，当[onIntercept]返回`true`时才被调用
     */
    internal fun getMovementFlags(holder: ViewHolder): Int {
        val dragFlags = getDragFlags(holder)
            .takeIf { it != defaultFlags } ?: getDefaultDragFlags()
        val swipeFlags = getSwipeFlags(holder)
            .takeIf { it != defaultFlags } ?: getDefaultSwipeFlags()
        return Callback.makeMovementFlags(dragFlags, swipeFlags)
    }

    /**
     * 获取拖动的移动标志，当[onIntercept]返回`true`时才被调用
     *
     * 返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止拖动，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     *
     * **注意**：若需要重写[getDragFlags]，可以参考[getDefaultDragFlags]的匹配逻辑。
     */
    open fun getDragFlags(holder: ViewHolder): Int = disallowFlags

    /**
     * 获取侧滑的移动标志，当[onIntercept]返回`true`时才被调用
     *
     * 返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止侧滑，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     *
     * **注意**：若需要重写[getSwipeFlags]，可以参考[getDefaultSwipeFlags]的匹配逻辑。
     */
    open fun getSwipeFlags(holder: ViewHolder): Int = disallowFlags

    /**
     * 对应[Callback.onMove]，当[onIntercept]返回`true`时才被调用
     */
    open fun onDrag(from: ViewHolder, to: ViewHolder): Boolean = false

    /**
     * 对应[Callback.onSwiped]，当[onIntercept]返回`true`时才被调用
     */
    open fun onSwipe(holder: ViewHolder, direction: Int): Unit = Unit

    /**
     * 选中回调
     *
     * 对应[Callback.onSelectedChanged]，可以看作是触摸开始，当[onIntercept]返回`true`时才被调用。
     */
    open fun onSelected(holder: ViewHolder): Unit = Unit

    /**
     * 绘制回调，在itemView之下绘制内容
     *
     * 对应[Callback.onChildDraw]，可以看作是触摸中，当[onIntercept]返回`true`时才被调用。
     */
    open fun onDraw(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ): Unit = Unit

    /**
     * 绘制回调，在itemView之上绘制内容
     *
     * 对应[Callback.onChildDrawOver]，可以看作是触摸中，当[onIntercept]返回`true`时才被调用。
     */
    open fun onDrawOver(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ): Unit = Unit

    /**
     * 恢复在[onSelected]、[onDraw]、[onDrawOver]修改的视图状态
     *
     * 对应[Callback.clearView]，可以看作是触摸结束，当[onIntercept]返回`true`时才被调用。
     */
    open fun clearView(holder: ViewHolder): Unit = Unit

    internal fun attach(
        touchHelper: ItemTouchHelper,
        recyclerView: RecyclerView
    ) {
        this.touchHelper = touchHelper
        this.recyclerView = recyclerView
    }

    private fun getDefaultDragFlags(): Int {
        return when (val lm: LayoutManager? = recyclerView?.layoutManager) {
            is GridLayoutManager -> UP or DOWN or LEFT or RIGHT
            is LinearLayoutManager -> if (lm.isVertical) UP or DOWN else LEFT or RIGHT
            else -> 0
        }
    }

    private fun getDefaultSwipeFlags(): Int {
        return when (val lm: LayoutManager? = recyclerView?.layoutManager) {
            is GridLayoutManager -> when {
                lm.spanCount != 1 -> 0
                lm.isVertical -> LEFT or RIGHT
                else -> UP or DOWN
            }
            is LinearLayoutManager -> if (lm.isVertical) LEFT or RIGHT else UP or DOWN
            else -> 0
        }
    }
}

/**
 * 比[ItemTouchCallback]更进一步的简化类，用于简化触摸回调的配置流程，
 * 提供了[disallowDrag]、[disallowSwipe]、[startDragView]等简化函数。
 */
@RvDslMarker
@Suppress("UNCHECKED_CAST", "NEWER_VERSION_IN_SINCE_KOTLIN")
class ItemTouchScope<AdapterT : Adapter<VH>, VH : ViewHolder>
@PublishedApi internal constructor(
    private val adapter: AdapterT,
    private val rv: RecyclerView
) : ItemTouchCallback() {
    private var handler: StartDragHandler? = null
    private var dragFlags: (AdapterT.(holder: VH) -> Int)? = null
    private var swipeFlags: (AdapterT.(holder: VH) -> Int)? = null
    private var onMove: (AdapterT.(from: Int, to: Int) -> Boolean)? = null
    private var onSwiped: (AdapterT.(position: Int, direction: Int) -> Unit)? = null
    private var onSelected: (AdapterT.(holder: VH) -> Unit)? = null
    private var onDraw: OnDraw<AdapterT, VH>? = null
    private var onDrawOver: OnDraw<AdapterT, VH>? = null
    private var clearView: (AdapterT.(holder: VH) -> Unit)? = null

    /**
     * 获取拖动的移动标志
     *
     * [block]的返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止拖动，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     */
    fun dragFlags(block: AdapterT.(holder: VH) -> Int) {
        dragFlags = block
    }

    /**
     * 获取侧滑的移动标志
     *
     * [block]的返回值跟[ItemTouchHelper]下的[UP]、[DOWN]、[LEFT]、[RIGHT]含义一致，
     * 返回[disallowFlags]表示禁止侧滑，返回[defaultFlags]表示根据当前布局类型，设置合适的移动标志。
     */
    fun swipeFlags(block: AdapterT.(holder: VH) -> Int) {
        swipeFlags = block
    }

    /**
     * 触发开始拖动的View
     *
     * [ACTION_DOWN]触摸到View就开始拖动，不用通过长按itemView触发拖动，
     * [withLongPress]为`true`表示继续启用长按拖动，`false`表示禁用长按拖动。
     */
    fun startDragView(withLongPress: Boolean = false, block: AdapterT.(holder: VH) -> View) {
        isLongPressDragEnabled = withLongPress
        handler?.let(rv::removeOnItemTouchListener)
        handler = StartDragHandler(block).also(rv::addOnItemTouchListener)
    }

    /**
     * 对应[Callback.onMove]
     *
     * 将[ViewHolder]形参简化为[ViewHolder.getBindingAdapterPosition]。
     */
    fun onDrag(block: AdapterT.(from: Int, to: Int) -> Boolean) {
        onMove = block
    }

    /**
     * 对应[Callback.onSwiped]
     *
     * 将[ViewHolder]形参简化为[ViewHolder.getBindingAdapterPosition]。
     */
    fun onSwipe(block: AdapterT.(position: Int, direction: Int) -> Unit) {
        onSwiped = block
    }

    /**
     * 选中回调
     *
     * 对应[Callback.onSelectedChanged]，可以看作是触摸开始。
     */
    fun onSelected(block: AdapterT.(holder: VH) -> Unit) {
        onSelected = block
    }

    /**
     * 绘制回调，在itemView之下绘制内容
     *
     * 对应[Callback.onChildDraw]，可以看作是触摸中。
     */
    fun onDraw(block: OnDraw<AdapterT, VH>) {
        onDraw = block
    }

    /**
     * 绘制回调，在itemView之上绘制内容
     *
     * 对应[Callback.onChildDrawOver]，可以看作是触摸中。
     */
    fun onDrawOver(block: OnDraw<AdapterT, VH>) {
        onDrawOver = block
    }

    /**
     * 恢复在[onSelected]、[onDraw]、[onDrawOver]修改的视图状态
     *
     * 对应[Callback.clearView]，可以看作是触摸结束。
     */
    fun clearView(block: AdapterT.(holder: VH) -> Unit) {
        clearView = block
    }

    /**
     * [block]返回`true`表示禁止拖动
     */
    inline fun disallowDrag(crossinline block: AdapterT.(holder: VH) -> Boolean) {
        dragFlags { holder ->
            if (block(this, holder)) disallowFlags else defaultFlags
        }
    }

    /**
     * [block]返回`true`表示禁止侧滑
     */
    inline fun disallowSwipe(crossinline block: AdapterT.(holder: VH) -> Boolean) {
        swipeFlags { holder ->
            if (block(this, holder)) disallowFlags else defaultFlags
        }
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onIntercept(holder: ViewHolder): Boolean {
        return holder.bindingAdapter == adapter
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun getDragFlags(holder: ViewHolder): Int {
        if (onMove == null) {
            return disallowFlags
        }
        return dragFlags?.invoke(adapter, holder as VH) ?: defaultFlags
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun getSwipeFlags(holder: ViewHolder): Int {
        if (onSwiped == null) {
            return disallowFlags
        }
        return swipeFlags?.invoke(adapter, holder as VH) ?: defaultFlags
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onDrag(from: ViewHolder, to: ViewHolder): Boolean {
        val fromPosition = from.bindingAdapterPosition
        val toPosition = to.bindingAdapterPosition
        return onMove?.invoke(adapter, fromPosition, toPosition) ?: false
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onSwipe(holder: ViewHolder, direction: Int) {
        onSwiped?.invoke(adapter, holder.bindingAdapterPosition, direction)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onSelected(holder: ViewHolder) {
        onSelected?.invoke(adapter, holder as VH)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onDraw(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        onDraw?.invoke(adapter, canvas, holder as VH, dX, dY, actionState, isCurrentlyActive)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun onDrawOver(
        canvas: Canvas, holder: ViewHolder,
        dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
    ) {
        onDrawOver?.invoke(adapter, canvas, holder as VH, dX, dY, actionState, isCurrentlyActive)
    }

    @SinceKotlin(RV_HIDE_MARKER)
    override fun clearView(holder: ViewHolder) {
        clearView?.invoke(adapter, holder as VH)
    }

    private inner class StartDragHandler(
        private val block: AdapterT.(holder: VH) -> View
    ) : SimpleOnItemTouchListener() {

        override fun onInterceptTouchEvent(rv: RecyclerView, event: MotionEvent): Boolean {
            if (touchHelper == null || event.actionMasked != ACTION_DOWN) {
                return false
            }
            val holder = rv.findChildViewUnder(event.x, event.y)
                ?.holder?.takeIf { it.bindingAdapter == adapter } ?: return false
            val view = block(adapter, holder as VH)
            if (view.isTouched(event.rawX, event.rawY)) {
                touchHelper?.startDrag(holder)
            }
            return false
        }
    }
}

typealias OnDraw<T, VH> = T.(canvas: Canvas, holder: VH, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) -> Unit