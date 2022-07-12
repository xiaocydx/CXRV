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
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.sample.databinding.ActivityViewPager2Binding
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
    private lateinit var adapter: FooListFragmentAdapter
    private val sharedViewModel: FooCategoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPager2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initCollect()
    }

    private fun initView() = with(binding) {
        adapter = FooListFragmentAdapter(this@ViewPager2Activity)
        viewPager2.adapter = adapter
        // getChildAt(0) = RecyclerView
        viewPager2.getChildAt(0).overScrollNever()
        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager2,
            /*autoRefresh*/true,
            /*smoothScroll*/true
        ) { tab, position ->
            tab.text = adapter.getItem(position).title
        }.attach()

        viewPager2.registerOnPageChangeCallback(
            onSelected = sharedViewModel::setCurrentItem
        )
        btnAdd.onClick(sharedViewModel::addItemToLast)
        btnRemove.onClick(sharedViewModel::removeCurrentItem)
        btnMove.onClick(sharedViewModel::moveCurrentItemToFirst)
    }

    private fun initCollect() = with(binding) {
        val state = sharedViewModel
            .categoryState.flowWithLifecycle(lifecycle)

        state.map { it.items }
            .distinctUntilChanged()
            .onEach(adapter::setItems)
            .launchIn(lifecycleScope)

        state.map {
            if (it.pendingItem != NO_ITEM) {
                viewPager2.setCurrentItem(it.pendingItem, false)
            } else if (it.currentItem != viewPager2.currentItem) {
                viewPager2.currentItem = it.currentItem
            }
        }.launchIn(lifecycleScope)
    }

    private class FooListFragmentAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        private var items: List<FooCategory> = emptyList()

        fun setItems(items: List<FooCategory>) {
            this.items = items
            notifyDataSetChanged()
        }

        fun getItem(position: Int): FooCategory = items[position]

        override fun getItemCount(): Int = items.size

        override fun getItemId(position: Int): Long = items[position].id

        override fun containsItem(itemId: Long): Boolean {
            return items.firstOrNull { it.id == itemId } != null
        }

        override fun createFragment(position: Int): Fragment {
            return FooListFragment.newInstance(getItem(position).id)
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            recyclerView.itemAnimator = null
        }
    }
}