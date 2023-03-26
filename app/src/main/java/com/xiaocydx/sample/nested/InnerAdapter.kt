package com.xiaocydx.sample.nested

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.sample.databinding.ItemNestedInnerBinding

/**
 * @author xcc
 * @date 2022/4/6
 */
class InnerAdapter : RecyclerView.Adapter<InnerHolder>() {
    private var data = emptyList<InnerItem>()

    fun updateData(data: List<InnerItem>) {
        val itemCount = itemCount
        this.data = data
        notifyItemRangeRemoved(0, itemCount)
        notifyItemRangeInserted(0, data.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerHolder {
        val binding = ItemNestedInnerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return InnerHolder(binding)
    }

    override fun onBindViewHolder(holder: InnerHolder, position: Int) {
        holder.binding.root.text = data[position].title
    }

    override fun getItemCount(): Int = data.size
}

class InnerHolder(val binding: ItemNestedInnerBinding) : RecyclerView.ViewHolder(binding.root)