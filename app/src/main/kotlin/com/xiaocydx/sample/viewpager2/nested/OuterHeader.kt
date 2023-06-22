package com.xiaocydx.sample.viewpager2.nested

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.databinding.ItemNestedInnerBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.withLayoutParams

@Suppress("FunctionName")
fun OuterHeader(context: Context) = ViewPager2(context).apply {
    // 水平方向ViewPager（Parent）和水平方向ViewPager（Child）
    isVp2NestedScrollable = true
    adapter = HeaderListAdapter()
    withLayoutParams(matchParent, 200.dp)
    setPageTransformer(MarginPageTransformer(8.dp))
}.toAdapter()

@SuppressLint("SetTextI18n")
private class HeaderListAdapter : RecyclerView.Adapter<HeaderHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        val binding = ItemNestedInnerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        binding.root.apply { withLayoutParams(matchParent, matchParent) }
        return HeaderHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        holder.binding.root.text = "Page-${position + 1}"
    }

    override fun getItemCount(): Int = 3
}

private class HeaderHolder(val binding: ItemNestedInnerBinding) : RecyclerView.ViewHolder(binding.root)