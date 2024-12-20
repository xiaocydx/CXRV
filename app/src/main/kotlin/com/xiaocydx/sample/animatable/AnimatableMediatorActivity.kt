package com.xiaocydx.sample.animatable

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.cxrv.animatable.AnimatableMediator
import com.xiaocydx.sample.databinding.ActivityAnimatableMediatorBinding
import com.xiaocydx.sample.viewpager2.shared.FooCategory
import com.xiaocydx.sample.viewpager2.shared.FooCategoryAdapter

/**
 * [AnimatableMediator]示例代码
 *
 * @author xcc
 * @date 2024/12/20
 */
class AnimatableMediatorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAnimatableMediatorBinding.inflate(layoutInflater)
        setContentView(binding.initView().root)
    }

    private fun ActivityAnimatableMediatorBinding.initView() = apply {
        val categoryAdapter = FooCategoryAdapter(
            activity = this@AnimatableMediatorActivity,
            createFragment = { AnimatableMediatorFragment.newInstance(it.id) }
        )
        categoryAdapter.submitList((1..2L).map { FooCategory(id = it) })
        viewPager2.apply {
            offscreenPageLimit = 1
            adapter = categoryAdapter
        }
        TabLayoutMediator(
            tabLayout, viewPager2,
            /* autoRefresh */true,
            /* smoothScroll */true
        ) { tab, position ->
            tab.text = categoryAdapter.getItem(position).title
        }.attach()
    }
}