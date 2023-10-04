package com.xiaocydx.sample.transition.enter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.concat.ViewAdapter
import com.xiaocydx.sample.SimpleViewHolder
import com.xiaocydx.sample.databinding.ItemSlideContentBinding
import com.xiaocydx.sample.databinding.ItemSlideLoadingBinding

class LoadingAdapter : ViewAdapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = ItemSlideLoadingBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return SimpleViewHolder(view.root)
    }

    fun show() = updateItem(show = true, NeedAnim.NOT_ALL)

    fun hide() = updateItem(show = false, NeedAnim.NOT_ALL)
}

class ContentAdapter : RecyclerView.Adapter<ContentAdapter.Holder>() {
    private var itemCount = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = ItemSlideContentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.binding.root.text = position.toString()
    }

    override fun getItemCount(): Int = itemCount

    fun show() {
        if (itemCount > 0) return
        itemCount = 100
        notifyItemRangeInserted(0, itemCount)
    }

    class Holder(val binding: ItemSlideContentBinding) : RecyclerView.ViewHolder(binding.root)
}