package com.xiaocydx.sample.nestedlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.sample.databinding.ItemNestedHorizontalBinding

/**
 * @author xcc
 * @date 2022/4/6
 */
class HorizontalAdapter : RecyclerView.Adapter<HorizontalHolder>() {
    private var data = emptyList<HorizontalItem>()

    fun setDataAndNotifyChanged(data: List<HorizontalItem>) {
        val itemCount = itemCount
        this.data = data
        notifyItemRangeChanged(0, itemCount, javaClass.simpleName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalHolder {
        val binding = ItemNestedHorizontalBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return HorizontalHolder(binding)
    }

    override fun onBindViewHolder(holder: HorizontalHolder, position: Int) {
        holder.binding.root.text = data[position].title
    }

    override fun getItemCount(): Int = data.size
}

class HorizontalHolder(
    val binding: ItemNestedHorizontalBinding
) : RecyclerView.ViewHolder(binding.root)