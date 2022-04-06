package com.xiaocydx.sample.nestedlist

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import com.xiaocydx.recycler.extension.adapter
import com.xiaocydx.recycler.extension.divider
import com.xiaocydx.recycler.extension.linear
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.sample.databinding.ItemNestedVerticalBinding
import com.xiaocydx.sample.dp

/**
 * @author xcc
 * @date 2022/4/6
 */
class VerticalAdapter : ListAdapter<VerticalItem, VerticalHolder>() {
    private val sharedPool = RecyclerView.RecycledViewPool()

    override fun areItemsTheSame(oldItem: VerticalItem, newItem: VerticalItem): Boolean {
        return oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalHolder {
        val binding = ItemNestedVerticalBinding
            .inflate(parent.inflater, parent, false)
        return VerticalHolder(binding).apply { setSharedPool(sharedPool) }
    }

    override fun onBindViewHolder(holder: VerticalHolder, item: VerticalItem) {
        holder.onBindView(item)
    }
}

class VerticalHolder(
    private val binding: ItemNestedVerticalBinding
) : RecyclerView.ViewHolder(binding.root) {
    private val adapter = HorizontalAdapter()

    init {
        binding.rvHorizontal
            .linear(orientation = HORIZONTAL)
            .divider {
                width = 8.dp
                horizontalEdge = true
            }
            .adapter(adapter)
    }

    fun setSharedPool(pool: RecyclerView.RecycledViewPool) {
        binding.rvHorizontal.setRecycledViewPool(pool)
    }

    fun onBindView(item: VerticalItem) {
        binding.tvTitle.text = item.title
        // FIXME: 2022/4/6 恢复滚动位置
        binding.rvHorizontal.scrollToPosition(0)
        adapter.setDataAndNotifyChanged(item.data)
    }
}