package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.sample.databinding.ActivityViewPager2Binding
import com.xiaocydx.sample.onClick
import com.xiaocydx.sample.registerOnPageChangeCallback
import com.xiaocydx.sample.repeatOnLifecycle
import kotlinx.coroutines.flow.onEach

/**
 * ViewPager2共享池示例代码
 *
 * @author xcc
 * @date 2022/2/21
 */
class ViewPager2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityViewPager2Binding
    private lateinit var categoryAdapter: FooCategoryAdapter
    private val sharedViewModel: FooCategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPager2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initCollect()
    }

    private fun initView() = with(binding) {
        categoryAdapter = FooCategoryAdapter(this@ViewPager2Activity)
        viewPager2.adapter = categoryAdapter
        TabLayoutMediator(
            tabLayout,
            viewPager2,
            /* autoRefresh */true,
            /* smoothScroll */true
        ) { tab, position ->
            tab.text = categoryAdapter.getItem(position).title
        }.attach()

        viewPager2.offscreenPageLimit = 1
        viewPager2.registerOnPageChangeCallback(
            onSelected = sharedViewModel::setCurrentItem
        )
        btnAdd.onClick(sharedViewModel::addItemToLast)
        btnRemove.onClick(sharedViewModel::removeCurrentItem)
        btnMove.onClick(sharedViewModel::moveCurrentItemToFirst)
    }

    private fun initCollect() = with(binding) {
        sharedViewModel.state
            .onEach {
                categoryAdapter.submitList(it.list)
                if (it.hasPendingItem) {
                    sharedViewModel.consumePendingItem()
                    viewPager2.setCurrentItem(it.pendingItem, /* smoothScroll */false)
                } else if (it.currentItem != viewPager2.currentItem) {
                    viewPager2.setCurrentItem(it.currentItem, /* smoothScroll */true)
                }
            }
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }
}