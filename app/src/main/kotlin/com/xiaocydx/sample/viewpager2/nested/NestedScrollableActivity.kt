package com.xiaocydx.sample.viewpager2.nested

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.cxrv.viewpager2.nested.Vp2NestedScrollableHandler
import com.xiaocydx.sample.databinding.ActivityNestedScrollableBinding

/**
 * ViewPager2滚动冲突处理示例代码
 *
 * [Vp2NestedScrollableHandler]处理[ViewPager2]嵌套[RecyclerView]等滚动控件的滚动冲突，
 * `RecyclerView.isVp2NestedScrollable = true`能便捷的使用[Vp2NestedScrollableHandler]，
 * 示例代码通过设置`RecyclerView.isVp2NestedScrollable = true`，处理以下场景的滚动冲突：
 * 1. [OuterHeader]，水平方向ViewPager2（Parent）和水平方向ViewPager2（Child）。
 * 2. [OuterHolder]，水平方向ViewPager2（Parent）和水平方向RecyclerView（Child）。
 * 3. [NestedPageAdapter]，水平方向ViewPager2（Parent）和垂直方向RecyclerView（Child）。
 *
 * * 处理相同方向的滚动冲突，当Child无法滚动时，才允许Parent拦截触摸事件。
 * * 处理不同方向的滚动冲突，Parent拦截触摸事件的条件更严格，不会那么“灵敏”。
 *
 * @author xcc
 * @date 2022/4/6
 */
class NestedScrollableActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityNestedScrollableBinding.inflate(layoutInflater)
        setContentView(binding.initView().root)
    }

    private fun ActivityNestedScrollableBinding.initView() = apply {
        viewPager2.offscreenPageLimit = 2
        viewPager2.adapter = NestedPageAdapter()
        TabLayoutMediator(
            tabLayout,
            viewPager2,
            /* autoRefresh */true,
            /* smoothScroll */true
        ) { tab, position ->
            tab.text = "Nested-${position + 1}"
        }.attach()
    }
}