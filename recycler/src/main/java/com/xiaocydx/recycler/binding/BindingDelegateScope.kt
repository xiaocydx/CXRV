package com.xiaocydx.recycler.binding

import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.viewbinding.ViewBinding
import com.xiaocydx.recycler.marker.RvDslMarker
import com.xiaocydx.recycler.widget.SpanSizeProvider

/**
 * [BindingDelegate]的构建函数，适用于简单列表场景
 *
 * ```
 * bindingDelegate(
 *     inflate = ItemFooBinding::inflate,
 *     areItemsTheSame { oldItem: Foo, newItem: Foo ->
 *         oldItem.id == newItem.id
 *     }
 * ) {
 *     onCreateView {
 *         ...
 *     }
 *     onBindView { item ->
 *         ...
 *     }
 * }
 * ```
 * @param inflate         函数引用`VB::inflate`
 * @param areItemsTheSame 对应[ItemCallback.areItemsTheSame]
 */
inline fun <ITEM : Any, VB : ViewBinding> bindingDelegate(
    noinline inflate: Inflate<VB>,
    noinline areItemsTheSame: (oldItem: ITEM, newItem: ITEM) -> Boolean,
    block: BindingDelegateScope<ITEM, VB>.() -> Unit
): BindingDelegate<ITEM, VB> {
    // viewType的值是类的JavaClass的hashCode，
    // 因此需要内联到调用处，在调用处生成匿名内部类。
    return object : BindingDelegateScope<ITEM, VB>(inflate, areItemsTheSame) {}.apply(block)
}

/**
 * [BindingDelegate]的构建函数，适用于简单列表场景
 *
 * ```
 * bindingDelegate(
 *     uniqueId = Foo::id,
 *     inflate = ItemFooBinding::inflate
 * ) {
 *     onCreateView {
 *         ...
 *     }
 *     onBindView { item ->
 *         ...
 *     }
 * }
 * ```
 * @param inflate  函数引用`VB::inflate`
 * @param uniqueId item唯一id，是[ItemCallback.areItemsTheSame]的简化函数
 */
inline fun <ITEM : Any, VB : ViewBinding> bindingDelegate(
    noinline inflate: Inflate<VB>,
    crossinline uniqueId: (item: ITEM) -> Any?,
    block: BindingDelegateScope<ITEM, VB>.() -> Unit
): BindingDelegate<ITEM, VB> = bindingDelegate(
    inflate = inflate,
    areItemsTheSame = { oldItem: ITEM, newItem: ITEM ->
        uniqueId(oldItem) == uniqueId(newItem)
    },
    block = block
)

/**
 * [BindingDelegate]的构建作用域
 */
@RvDslMarker
abstract class BindingDelegateScope<ITEM : Any, VB : ViewBinding>
@PublishedApi internal constructor(
    private val inflate: Inflate<VB>,
    private val areItemsTheSame: (oldItem: ITEM, newItem: ITEM) -> Boolean
) : BindingDelegate<ITEM, VB>() {
    private var areContentsTheSame: ((oldItem: ITEM, newItem: ITEM) -> Boolean)? = null
    private var getChangePayload: ((oldItem: ITEM, newItem: ITEM) -> Any?)? = null
    private var onCreateView: (VB.() -> Unit)? = null
    private var onBindView: (VB.(item: ITEM, payloads: List<Any>) -> Unit)? = null
    private var onViewRecycled: (VB.() -> Unit)? = null
    private var onFailedToRecycleView: (VB.() -> Boolean)? = null
    private var onViewAttachedToWindow: (VB.() -> Unit)? = null
    private var onViewDetachedFromWindow: (VB.() -> Unit)? = null
    private var onAttachedToRecyclerView: ((rv: RecyclerView) -> Unit)? = null
    private var onDetachedFromRecyclerView: ((rv: RecyclerView) -> Unit)? = null
    private var fullSpan: ((position: Int, BindingHolder<VB>) -> Boolean)? = null
    private var getSpanSize: ((position: Int, spanCount: Int) -> Int)? = null

    /**
     * 对应[ItemCallback.areContentsTheSame]
     *
     * 仅当[areItemsTheSame]的`block`返回true时才调用[block]。
     */
    fun areContentsTheSame(@WorkerThread block: (oldItem: ITEM, newItem: ITEM) -> Boolean) {
        areContentsTheSame = block
    }

    /**
     * 对应[ItemCallback.getChangePayload]
     *
     * 仅当[areItemsTheSame]的`block`返回true、
     * [areContentsTheSame]的`block`返回false时才调用[block]。
     */
    fun getChangePayload(@WorkerThread block: (oldItem: ITEM, newItem: ITEM) -> Any?) {
        getChangePayload = block
    }

    /**
     * 对应[Adapter.onCreateViewHolder]
     *
     * 可以在[block]中完成初始化工作，例如设置点击监听。
     */
    fun onCreateView(block: VB.() -> Unit) {
        onCreateView = block
    }

    /**
     * 对应Adapter.onBindViewHolder(holder, position, payloads)
     */
    fun onBindViewPayloads(block: VB.(item: ITEM, payloads: List<Any>) -> Unit) {
        onBindView = block
    }

    /**
     * 对应[Adapter.onBindViewHolder]
     */
    inline fun onBindView(crossinline block: VB.(item: ITEM) -> Unit) {
        onBindViewPayloads { item, _ -> block(this, item) }
    }

    /**
     * 对应[Adapter.onViewRecycled]
     */
    fun onViewRecycled(block: VB.() -> Unit) {
        onViewRecycled = block
    }

    /**
     * 对应[Adapter.onFailedToRecycleView]
     */
    fun onFailedToRecycleView(block: VB.() -> Boolean) {
        onFailedToRecycleView = block
    }

    /**
     * 对应[Adapter.onViewAttachedToWindow]
     */
    fun onViewAttachedToWindow(block: VB.() -> Unit) {
        onViewAttachedToWindow = block
    }

    /**
     * 对应[Adapter.onViewDetachedFromWindow]
     */
    fun onViewDetachedFromWindow(block: VB.() -> Unit) {
        onViewDetachedFromWindow = block
    }

    /**
     * 对应[Adapter.onAttachedToRecyclerView]
     */
    fun onAttachedToRecyclerView(block: (rv: RecyclerView) -> Unit) {
        onAttachedToRecyclerView = block
    }

    /**
     * 对应[Adapter.onDetachedFromRecyclerView]
     */
    fun onDetachedFromRecyclerView(block: (rv: RecyclerView) -> Unit) {
        onDetachedFromRecyclerView = block
    }

    /**
     * 对应[SpanSizeProvider.fullSpan]
     */
    fun fullSpan(block: (position: Int, BindingHolder<VB>) -> Boolean) {
        fullSpan = block
    }

    /**
     * 对应[SpanSizeProvider.getSpanSize]
     */
    fun getSpanSize(block: (position: Int, spanCount: Int) -> Int) {
        getSpanSize = block
    }

    override fun inflate(): Inflate<VB> = inflate

    override fun areItemsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
        return areItemsTheSame.invoke(oldItem, newItem)
    }

    override fun areContentsTheSame(oldItem: ITEM, newItem: ITEM): Boolean {
        return areContentsTheSame?.invoke(oldItem, newItem) ?: oldItem == newItem
    }

    override fun getChangePayload(oldItem: ITEM, newItem: ITEM): Any? {
        return getChangePayload?.invoke(oldItem, newItem)
    }

    override fun VB.onCreateView() {
        onCreateView?.invoke(this)
    }

    override fun VB.onBindView(item: ITEM) {
        onBindView?.invoke(this, item, emptyList())
    }

    override fun VB.onBindView(item: ITEM, payloads: List<Any>) {
        onBindView?.invoke(this, item, payloads)
    }

    override fun VB.onViewRecycled() {
        onViewRecycled?.invoke(this)
    }

    override fun VB.onFailedToRecycleView(): Boolean {
        return onFailedToRecycleView?.invoke(this) ?: false
    }

    override fun VB.onViewAttachedToWindow() {
        onViewAttachedToWindow?.invoke(this)
    }

    override fun VB.onViewDetachedFromWindow() {
        onViewDetachedFromWindow?.invoke(this)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        onAttachedToRecyclerView?.invoke(recyclerView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        onDetachedFromRecyclerView?.invoke(recyclerView)
    }

    override fun fullSpan(position: Int, holder: BindingHolder<VB>): Boolean {
        return fullSpan?.invoke(position, holder) ?: false
    }

    override fun getSpanSize(position: Int, spanCount: Int): Int {
        return getSpanSize?.invoke(position, spanCount) ?: 1
    }
}