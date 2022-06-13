package com.xiaocydx.sample.paging

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActivityPagingBinding
import com.xiaocydx.sample.databinding.ItemMenuBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.navigationBarEdgeToEdge
import com.xiaocydx.sample.paging.MenuAction.*
import com.xiaocydx.sample.showToast

/**
 * 分页加载示例代码（本地测试）
 *
 * 页面配置发生变更时（例如旋转屏幕），保留分页加载数据、列表滚动位置。
 *
 * @author xcc
 * @date 2022/2/17
 */
class PagingActivity : AppCompatActivity() {
    private val viewModel: SharedViewModel by viewModels()
    private lateinit var binding: ActivityPagingBinding
    private val fragmentTag = PagingFragment::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMenuDrawer()
        initPagingFragment()
        window.navigationBarEdgeToEdge()
    }

    private fun initMenuDrawer() {
        binding.rvMenu
            .linear()
            .fixedSize()
            .divider {
                height = 0.5f.dp
                color = 0xFFD5D5D5.toInt()
            }
            .adapter(bindingAdapter(
                uniqueId = MenuAction::text,
                inflate = ItemMenuBinding::inflate
            ) {
                onBindView { root.text = it.text }
                doOnSimpleItemClick(::executeMenuAction)
                submitList(values().toList())
            })
    }

    private fun executeMenuAction(action: MenuAction) {
        when (action) {
            LINEAR_LAYOUT -> initLinearLayout()
            GIRD_LAYOUT -> initGridLayout()
            STAGGERED_LAYOUT -> initStaggeredLayout()
            else -> viewModel.submitMenuAction(action)
        }
        binding.root.closeDrawer(binding.rvMenu)
        showToast(action.text)
    }

    private fun initPagingFragment() {
        val fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment == null) {
            initLinearLayout()
        } else {
            setActionBarTitle(fragment)
        }
    }

    private fun initLinearLayout() {
        replaceFragment(LinearLayoutFragment())
    }

    private fun initGridLayout() {
        replaceFragment(GridLayoutFragment())
    }

    private fun initStaggeredLayout() {
        replaceFragment(StaggeredLayoutFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.flContainer, fragment, fragmentTag)
            .commit()
        setActionBarTitle(fragment)
    }

    private fun setActionBarTitle(fragment: Fragment) {
        supportActionBar?.title = fragment.javaClass.simpleName
    }
}