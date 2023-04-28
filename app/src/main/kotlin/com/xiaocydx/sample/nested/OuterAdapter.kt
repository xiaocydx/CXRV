package com.xiaocydx.sample.nested

import android.os.Parcelable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.sample.databinding.ItemNestedOuterBinding
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2022/4/6
 */
class OuterAdapter : ListAdapter<OuterItem, OuterHolder>() {
    private val sharedPool = RecyclerView.RecycledViewPool()
    private val savedStates = mutableMapOf<String, Parcelable>()

    override fun areItemsTheSame(oldItem: OuterItem, newItem: OuterItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OuterHolder {
        val binding = ItemNestedOuterBinding.inflate(
            parent.inflater, parent, false)
        return OuterHolder(sharedPool, binding)
    }

    override fun onBindViewHolder(holder: OuterHolder, item: OuterItem) {
        holder.onBindView(item)
    }

    override fun onViewAttachedToWindow(holder: OuterHolder) {
        holder.onRestoreState(savedStates.remove(holder.item.id))
    }

    override fun onViewDetachedFromWindow(holder: OuterHolder) {
        val state = holder.onSaveState() ?: return
        savedStates[holder.item.id] = state
    }
}

class OuterHolder(
    sharedPool: RecyclerView.RecycledViewPool,
    private val binding: ItemNestedOuterBinding
) : RecyclerView.ViewHolder(binding.root) {
    private val adapter = InnerAdapter()

    init {
        binding.rvInner
            .fixedSize()
            .adapter(adapter)
            .linear(orientation = HORIZONTAL) {
                // recycle to sharedPool
                recycleChildrenOnDetach = true
            }
            .divider(width = 8.dp) { edge(Edge.horizontal()) }
            .apply { itemAnimator = null }
            .setRecycledViewPool(sharedPool)
    }

    fun onBindView(item: OuterItem) {
        binding.tvTitle.text = item.title
        adapter.updateData(item.data)
    }

    fun onSaveState(): Parcelable? {
        return binding.rvInner.layoutManager?.onSaveInstanceState()
    }

    fun onRestoreState(state: Parcelable?): Unit = with(binding.rvInner) {
        if (state == null) {
            layoutManager?.scrollToPosition(0)
        } else {
            layoutManager?.onRestoreInstanceState(state)
        }
    }
}