package com.xiaocydx.recycler.multitype

import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PROTECTED
import androidx.annotation.WorkerThread
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.list.getItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 多类型适配器
 *
 * @author xcc
 * @date 2021/10/8
 */
@PublishedApi
internal class MultiTypeAdapter<T : Any>(
    private var multiType: MultiType<T> = unregistered(),
    workDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ListAdapter<T, ViewHolder>(workDispatcher) {

    init {
        setMultiType(multiType)
    }

    fun setMultiType(multiType: MultiType<T>) {
        if (multiType == unregistered<T>()) {
            return
        }
        this.multiType = multiType
        multiType.forEach { it.delegate.attachAdapter(this) }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return multiType.getViewTypeDelegate(viewType).onCreateViewHolder(parent)
    }

    override fun getItemViewType(position: Int): Int {
        return multiType.getItemViewType(getItem(position))
    }

    override fun onBindViewHolder(holder: ViewHolder, item: T, payloads: List<Any>) {
        multiType.getViewTypeDelegate(holder).onBindViewHolder(holder, item, payloads)
    }

    override fun onBindViewHolder(holder: ViewHolder, item: T) {
        multiType.getViewTypeDelegate(holder).onBindViewHolder(holder, item)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        multiType.getViewTypeDelegate(holder).onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: ViewHolder): Boolean {
        return multiType.getViewTypeDelegate(holder).onFailedToRecycleView(holder)
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        multiType.getViewTypeDelegate(holder).onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        multiType.getViewTypeDelegate(holder).onViewDetachedFromWindow(holder)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        multiType.forEach { it.delegate.onAttachedToRecyclerView(recyclerView) }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        multiType.forEach { it.delegate.onDetachedFromRecyclerView(recyclerView) }
    }

    override fun fullSpan(position: Int, holder: ViewHolder): Boolean {
        return multiType.getViewTypeDelegate(holder).fullSpan(position, holder)
    }

    override fun getSpanSize(position: Int, spanCount: Int): Int {
        val viewType = getItemViewType(position)
        return multiType.getViewTypeDelegate(viewType).getSpanSize(position, spanCount)
    }

    @WorkerThread
    @VisibleForTesting(otherwise = PROTECTED)
    public override fun areItemsTheSame(oldItem: T, newItem: T): Boolean = with(multiType) {
        if (oldItem.javaClass != newItem.javaClass) {
            // 快路径判断，若两个item的Class不相同，则viewType也不相同
            return false
        }
        val oldViewType = getItemViewType(oldItem)
        val newViewType = getItemViewType(newItem)
        oldViewType == newViewType && getViewTypeDelegate(oldViewType).areItemsTheSame(oldItem, newItem)
    }

    @WorkerThread
    @VisibleForTesting(otherwise = PROTECTED)
    public override fun areContentsTheSame(oldItem: T, newItem: T): Boolean = with(multiType) {
        val viewType = getItemViewType(oldItem)
        getViewTypeDelegate(viewType).areContentsTheSame(oldItem, newItem)
    }

    @WorkerThread
    @VisibleForTesting(otherwise = PROTECTED)
    public override fun getChangePayload(oldItem: T, newItem: T): Any? = with(multiType) {
        val viewType = getItemViewType(oldItem)
        getViewTypeDelegate(viewType).getChangePayload(oldItem, newItem)
    }
}