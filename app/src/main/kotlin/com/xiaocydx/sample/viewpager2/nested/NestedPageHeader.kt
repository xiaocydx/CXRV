@file:Suppress("FunctionName")

package com.xiaocydx.sample.viewpager2.nested

import android.content.Context
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.cxrv.viewpager2.loop.isVp2NestedScrollable
import com.xiaocydx.cxrv.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.databinding.ItemNestedHeaderBinding
import com.xiaocydx.sample.databinding.ItemNestedInnerBinding

fun NestedPageHeader(context: Context) = ViewPager2(context).apply {
    // 水平方向ViewPager2（Parent）和水平方向ViewPager2（Child）
    isVp2NestedScrollable = true
    adapter = NestedPageHeaderOuterAdapter(supportLoop = false)
    layoutParams(matchParent, 200.dp)
    setPageTransformer(MarginPageTransformer(8.dp))
}

/**
 * [LoopPagerController]是[ViewPager2]的循环页面控制器，
 * `LoopPagerController.isVp2NestedScrollable = true`处理滚动冲突。
 */
fun LoopNestedPageHeader(context: Context) = ViewPager2(context).apply {
    // 水平方向ViewPager2（Parent）和水平方向ViewPager2（Child）
    val controller = LoopPagerController(this)
    controller.isVp2NestedScrollable = true
    controller.setAdapter(NestedPageHeaderOuterAdapter(supportLoop = true))
    layoutParams(matchParent, 200.dp)
    setPageTransformer(MarginPageTransformer(8.dp))
}

private fun NestedPageHeaderOuterAdapter(
    supportLoop: Boolean
) = bindingAdapter(
    uniqueId = { it: String -> it },
    inflate = ItemNestedHeaderBinding::inflate
) {
    submitList((1..3).map {
        "${if (supportLoop) "LoopPage" else "Page"}-$it"
    })
    onCreateView {
        viewPager2.apply {
            // 水平方向ViewPager2（Parent）和水平方向ViewPager2（Child）
            isVp2NestedScrollable = true
            adapter = NestedPageHeaderInnerAdapter()
            setPageTransformer(MarginPageTransformer(8.dp))
        }
        if (supportLoop) root.setBackgroundColor(0xFFFFB4D5.toInt())
    }
    onBindView { tvTitle.text = it }
}

private fun NestedPageHeaderInnerAdapter() = bindingAdapter(
    uniqueId = { it: String -> it },
    inflate = ItemNestedInnerBinding::inflate
) {
    submitList((1..3).map { it.toString() })
    onCreateView {
        root.layoutParams(matchParent, matchParent)
        root.setBackgroundColor(0xFFA4D3BF.toInt())
    }
    onBindView { root.text = it }
}