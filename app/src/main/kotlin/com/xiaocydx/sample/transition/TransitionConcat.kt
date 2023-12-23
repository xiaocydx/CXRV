package com.xiaocydx.sample.transition

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.accompanist.view.SimpleViewHolder
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.sample.databinding.ItemContentBinding
import com.xiaocydx.sample.databinding.ItemLoadingBinding

class LoadingAdapter : ViewAdapter<ViewHolder>() {

    fun show() = updateItem(show = true, NeedAnim.NOT_ALL)

    fun hide() = updateItem(show = false, NeedAnim.NOT_ALL)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemLoadingBinding.inflate(inflater, parent, false)
        return SimpleViewHolder(binding.root)
    }
}

class ContentAdapter : RecyclerView.Adapter<ViewHolder>() {
    private var itemCount = 0

    fun show() {
        if (itemCount > 0) return
        itemCount = 100
        notifyItemRangeInserted(0, itemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemContentBinding.inflate(inflater, parent, false)
        return SimpleViewHolder(binding.root)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder.itemView as? TextView)?.text = position.toString()
    }

    override fun getItemCount(): Int = itemCount
}