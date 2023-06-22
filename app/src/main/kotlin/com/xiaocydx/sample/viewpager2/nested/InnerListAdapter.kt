package com.xiaocydx.sample.viewpager2.nested

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.sample.databinding.ItemNestedInnerBinding

/**
 * @author xcc
 * @date 2022/4/6
 */
class InnerListAdapter : RecyclerView.Adapter<InnerHolder>() {
    private var list = emptyList<InnerItem>()

    fun submitList(newList: List<InnerItem>) {
        val itemCount = itemCount
        list = newList
        notifyItemRangeRemoved(0, itemCount)
        notifyItemRangeInserted(0, newList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerHolder {
        val binding = ItemNestedInnerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return InnerHolder(binding)
    }

    override fun onBindViewHolder(holder: InnerHolder, position: Int) {
        holder.binding.root.text = list[position].title
    }

    override fun getItemCount(): Int = list.size
}

class InnerHolder(val binding: ItemNestedInnerBinding) : RecyclerView.ViewHolder(binding.root)