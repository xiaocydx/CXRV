package com.xiaocydx.sample.viewpager2.nested

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.xiaocydx.sample.databinding.ItemNestedInnerBinding

/**
 * @author xcc
 * @date 2022/4/6
 */
class InnerListAdapter : RecyclerView.Adapter<InnerHolder>() {
    private val list = mutableListOf<InnerItem>()

    /**
     * 选择调用[notifyDataSetChanged]而不是局部更新的原因：
     * 1. [submitList]只会在[OuterListAdapter.onBindViewHolder]中被调用，
     * 此时外部RecyclerView的布局流程拦截了[RecyclerView.requestLayout]，
     * 因此[notifyDataSetChanged]不会导致`fixedSize`的优化无效。
     *
     * 2. [submitList]的意图是替换列表且不需要动画，因此不需要启用`stableIds`，
     * 内部RecyclerView的布局流程移除子View，应当全部回收进[RecycledViewPool]。
     *
     * 3. 不启用`stableIds`，调用[notifyDataSetChanged]移除子View产生的影响，
     * 对替换列表而言不需要关注，`viewType`的回收上限在视图初始化阶段已做处理。
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<InnerItem>) {
        if (list != newList) {
            list.clear()
            list.addAll(newList)
            notifyDataSetChanged()
        }
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