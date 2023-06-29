package com.xiaocydx.sample.viewpager2.nested

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.concat.toAdapter
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.databinding.ItemNestedInnerBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent

@Suppress("FunctionName")
fun OuterHeader(context: Context) = ViewPager2(context).apply {
    // 水平方向ViewPager2（Parent）和水平方向ViewPager2（Child）
    isVp2NestedScrollable = true
    adapter = HeaderListAdapter(false)
    layoutParams(matchParent, 200.dp)
    setPageTransformer(MarginPageTransformer(8.dp))
}.toAdapter()

/**
 * [LoopPagerController]是[ViewPager2]循环页面的控制器，
 * `LoopPagerController.isVp2NestedScrollable = true`处理滚动冲突。
 */
@Suppress("FunctionName")
fun LoopOuterHeader(context: Context) = ViewPager2(context).apply {
    // 水平方向ViewPager2（Parent）和水平方向ViewPager2（Child）
    val controller = LoopPagerController(this)
    controller.isVp2NestedScrollable = true
    controller.setAdapter(HeaderListAdapter(true))
    layoutParams(matchParent, 200.dp)
    setPageTransformer(MarginPageTransformer(8.dp))
}.toAdapter()

@SuppressLint("SetTextI18n")
private class HeaderListAdapter(private val supportLoop: Boolean) : RecyclerView.Adapter<HeaderHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderHolder {
        val binding = ItemNestedInnerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        binding.root.layoutParams(matchParent, matchParent)
        if (supportLoop) binding.root.setBackgroundColor(0xFFFFB4D5.toInt())
        return HeaderHolder(binding)
    }

    override fun onBindViewHolder(holder: HeaderHolder, position: Int) {
        val pageText = if (supportLoop) "LoopPage" else "Page"
        holder.binding.root.text = "$pageText-${position + 1}"
    }

    override fun getItemCount(): Int = 3
}

private class HeaderHolder(val binding: ItemNestedInnerBinding) : RecyclerView.ViewHolder(binding.root)