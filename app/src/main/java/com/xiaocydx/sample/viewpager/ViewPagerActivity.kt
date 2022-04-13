package com.xiaocydx.sample.viewpager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.xiaocydx.sample.databinding.ActivityViewPagerBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.viewpager.shared.SharedRecycledFragment
import com.xiaocydx.sample.viewpager.shared.SharedRecycledPagerAdapter

/**
 * @author xcc
 * @date 2022/2/21
 */
class ViewPagerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.viewPager.apply {
            pageMargin = 10.dp
            offscreenPageLimit = 1
            adapter = FooListFragmentAdapter(supportFragmentManager)
        }
        binding.tabLayout.setupWithViewPager(binding.viewPager)
    }

    private class FooListFragmentAdapter(
        fm: FragmentManager
    ) : SharedRecycledPagerAdapter(fm) {
        override fun getCount(): Int = 6

        override fun getItem(position: Int): SharedRecycledFragment {
            return FooListFragment.newInstance(getPageTitle(position).toString())
        }

        override fun getPageTitle(position: Int): CharSequence = "List-${position + 1}"
    }
}