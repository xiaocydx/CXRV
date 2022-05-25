package com.xiaocydx.sample.nested

import android.os.Parcelable
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.sample.databinding.ItemNestedVerticalBinding
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2022/4/6
 */
class VerticalAdapter : ListAdapter<VerticalItem, VerticalHolder>() {
    private val sharedPool = RecyclerView.RecycledViewPool()
    private val savedStates = mutableMapOf<String, Parcelable>()

    override fun areItemsTheSame(oldItem: VerticalItem, newItem: VerticalItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalHolder {
        val binding = ItemNestedVerticalBinding
            .inflate(parent.inflater, parent, false)
        return VerticalHolder(sharedPool, binding)
    }

    override fun onBindViewHolder(holder: VerticalHolder, item: VerticalItem) {
        holder.onBindView(item)
        holder.onRestoreState(savedStates.remove(item.id))
    }

    override fun onViewRecycled(holder: VerticalHolder) {
        val state = holder.onSaveState() ?: return
        savedStates[holder.item.id] = state
    }
}

class VerticalHolder(
    sharedPool: RecyclerView.RecycledViewPool,
    private val binding: ItemNestedVerticalBinding
) : RecyclerView.ViewHolder(binding.root) {
    private val adapter = HorizontalAdapter()

    init {
        binding.rvHorizontal
            .linear(orientation = HORIZONTAL)
            .fixedSize().divider {
                width = 8.dp
                horizontalEdge = true
            }
            .adapter(adapter)
            .setRecycledViewPool(sharedPool)
    }

    fun onBindView(item: VerticalItem) {
        binding.tvTitle.text = item.title
        adapter.setDataAndNotifyChanged(item.data)
    }

    fun onSaveState(): Parcelable? {
        return binding.rvHorizontal.layoutManager?.onSaveInstanceState()
    }

    fun onRestoreState(state: Parcelable?): Unit = with(binding.rvHorizontal) {
        if (state == null) {
            layoutManager?.scrollToPosition(0)
        } else {
            layoutManager?.onRestoreInstanceState(state)
        }
    }
}