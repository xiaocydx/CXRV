package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.sample.*
import com.xiaocydx.sample.databinding.ActivityViewPager2Binding
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
        sharedViewModel.state
            .onEach {
                if (adapter.submitList(it.list)) {
                    viewPager2.ensureTransform()
                }
                if (it.currentItem != viewPager2.currentItem) {
                    viewPager2.scrollToCurrentItem(it.currentItem)
                }
            }
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }

    /**
     * ?????????????????????????????????[ViewPager2.requestTransform]???
     * ????????????[ViewPager2.PageTransformer]????????????????????????
     * ??????????????????????????????[MarginPageTransformer]??????????????????
     */
    private fun ViewPager2.ensureTransform() {
        doOnPreDraw { requestTransform() }
    }

    private fun ViewPager2.scrollToCurrentItem(currentItem: Int) {
        setCurrentItem(currentItem, /*smoothScroll*/false)
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