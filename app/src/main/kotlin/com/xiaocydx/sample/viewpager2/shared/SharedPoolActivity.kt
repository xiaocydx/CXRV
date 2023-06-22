package com.xiaocydx.sample.viewpager2.shared

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.sample.databinding.ActivitySharedPoolBinding
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
class SharedPoolActivity : AppCompatActivity() {
    private lateinit var categoryAdapter: FooCategoryAdapter
    private val sharedViewModel: FooCategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySharedPoolBinding.inflate(layoutInflater)
        setContentView(binding.initView().initCollect().root)
    }

    private fun ActivitySharedPoolBinding.initView() = apply {
        categoryAdapter = FooCategoryAdapter(this@SharedPoolActivity)
        viewPager2.apply {
            offscreenPageLimit = 1
            adapter = categoryAdapter
            registerOnPageChangeCallback(
                onSelected = sharedViewModel::setCurrentItem
            )
        }
        btnAdd.onClick(sharedViewModel::addItemToLast)
        btnRemove.onClick(sharedViewModel::removeCurrentItem)
        btnMove.onClick(sharedViewModel::moveCurrentItemToFirst)

        TabLayoutMediator(
            tabLayout,
            viewPager2,
            /* autoRefresh */true,
            /* smoothScroll */true
        ) { tab, position ->
            tab.text = categoryAdapter.getItem(position).title
        }.attach()
    }

    private fun ActivitySharedPoolBinding.initCollect() = apply {
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