package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.sample.databinding.ActivityViewPager2Binding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.onClick
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.registerOnPageChangeCallback
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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
            binding.tabLayout,
            binding.viewPager2,
            /*autoRefresh*/true,
            /*smoothScroll*/true
        ) { tab, position ->
            tab.text = adapter.getItem(position).title
        }.attach()

        viewPager2.setPageTransformer(MarginPageTransformer(5.dp))
        viewPager2.registerOnPageChangeCallback(
            onSelected = sharedViewModel::setCurrentItem
        )
        btnAdd.onClick(sharedViewModel::addItemToLast)
        btnRemove.onClick(sharedViewModel::removeCurrentItem)
        btnMove.onClick(sharedViewModel::moveCurrentItemToFirst)
    }

    private fun initCollect() = with(binding) {
        val state = sharedViewModel
            .state.flowWithLifecycle(lifecycle)

        state.map { it.list }
            .distinctUntilChanged()
            .onEach(adapter::submitList)
            .launchIn(lifecycleScope)

        state.map {
            if (it.pendingItem != NO_ITEM) {
                sharedViewModel.consumePendingItem()
                viewPager2.setCurrentItem(it.pendingItem, /*smoothScroll*/false)
            } else if (it.currentItem != viewPager2.currentItem) {
                viewPager2.setCurrentItem(it.currentItem, /*smoothScroll*/true)
            }
        }.launchIn(lifecycleScope)
    }

    private class FooCategoryAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        private var list: List<FooCategory> = emptyList()

        fun submitList(newList: List<FooCategory>) {
            list = newList
            notifyDataSetChanged()
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