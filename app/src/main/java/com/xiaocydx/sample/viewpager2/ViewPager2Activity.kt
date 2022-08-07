package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.sample.databinding.ActivityViewPager2Binding
import com.xiaocydx.sample.onClick
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.registerOnPageChangeCallback
import com.xiaocydx.sample.repeatOnLifecycle
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2022/2/21
 */
class ViewPager2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityViewPager2Binding
    private lateinit var adapter: FooCategoryAdapter
    private val sharedViewModel: FooCategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPager2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initCollect()
    }

    private fun initView() = with(binding) {
        adapter = FooCategoryAdapter(this@ViewPager2Activity)
        viewPager2.adapter = adapter
        TabLayoutMediator(
            tabLayout,
            viewPager2,
            /* autoRefresh */true,
            /* smoothScroll */true
        ) { tab, position ->
            tab.text = adapter.getItem(position).title
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
                adapter.submitList(it.list)
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

    private class FooCategoryAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        private var list: MutableList<FooCategory> = mutableListOf()

        fun submitList(newList: List<FooCategory>): Boolean {
            val changed = list != newList
            if (changed) {
                list.clear()
                list.addAll(newList)
                notifyDataSetChanged()
            }
            return changed
        }

        fun getItem(position: Int): FooCategory = list[position]

        override fun getItemCount(): Int = list.size

        override fun getItemId(position: Int): Long = list[position].id

        override fun containsItem(itemId: Long): Boolean {
            return list.firstOrNull { it.id == itemId } != null
        }

        override fun createFragment(position: Int): Fragment {
            return FooListFragment.newInstance(getItem(position).id)
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            recyclerView.overScrollNever()
            recyclerView.itemAnimator = null
        }
    }
}