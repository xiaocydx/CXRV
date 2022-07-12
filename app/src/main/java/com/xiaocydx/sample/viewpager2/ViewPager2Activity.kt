package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.xiaocydx.sample.databinding.ActivityViewPager2Binding
import com.xiaocydx.sample.overScrollNever

/**
 * @author xcc
 * @date 2022/2/21
 */
class ViewPager2Activity : AppCompatActivity() {
    private lateinit var binding: ActivityViewPager2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPager2Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.initView()
    }

    private fun ActivityViewPager2Binding.initView() {
        val adapter = FooListFragmentAdapter(this@ViewPager2Activity)
        viewPager2.adapter = adapter
        // getChildAt(0) = RecyclerView
        viewPager2.getChildAt(0).overScrollNever()
        TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager2,
            /*autoRefresh*/true,
            /*smoothScroll*/true
        ) { tab, position ->
            tab.text = "List-${adapter.getItem(position)}"
        }.attach()
    }

    private class FooListFragmentAdapter(
        fragmentActivity: FragmentActivity
    ) : FragmentStateAdapter(fragmentActivity) {
        private val data = (1L..10L).toList()

        fun getItem(position: Int): Long = data[position]

        override fun getItemCount(): Int = data.size

        override fun getItemId(position: Int): Long = data[position]

        override fun containsItem(itemId: Long): Boolean = data.contains(itemId)

        override fun createFragment(position: Int): Fragment {
            return FooListFragment.newInstance(getItem(position).toString())
        }

        override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            recyclerView.itemAnimator = null
        }
    }
}