package com.xiaocydx.sample.paging.local

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ActivityMenuBinding
import com.xiaocydx.sample.databinding.ItemMenuBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.paging.local.MenuAction.GIRD
import com.xiaocydx.sample.paging.local.MenuAction.LINEAR
import com.xiaocydx.sample.paging.local.MenuAction.STAGGERED
import com.xiaocydx.sample.showToast

/**
 * Paging示例代码（本地测试）
 *
 * 页面配置发生变更时（例如旋转屏幕），保留分页加载数据、列表滚动位置。
 *
 * @author xcc
 * @date 2022/2/17
 */
class PagingActivity : AppCompatActivity() {
    private val sharedViewModel: PagingSharedViewModel by viewModels()
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMenuDrawer()
        if (savedInstanceState == null) replace<LinearPagingFragment>()
    }

    private fun initMenuDrawer() {
        binding.rvMenu
            .linear().fixedSize()
            .divider(height = 0.5f.dp) {
                color(0xFFD5D5D5.toInt())
            }
            .adapter(bindingAdapter(
                uniqueId = MenuAction::text,
                inflate = ItemMenuBinding::inflate
            ) {
                submitList(MenuAction.values().toList())
                doOnSimpleItemClick(::performMenuAction)
                onBindView { root.text = it.text }
            })
        window.enableGestureNavBarEdgeToEdge()
    }

    private fun performMenuAction(action: MenuAction) {
        when (action) {
            LINEAR -> replace<LinearPagingFragment>()
            GIRD -> replace<GridPagingFragment>()
            STAGGERED -> replace<StaggeredPagingFragment>()
            else -> sharedViewModel.submitMenuAction(action)
        }
        binding.root.closeDrawer(binding.rvMenu)
        showToast(action.text)
    }

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(binding.container.id, T::class.java, null) }
    }
}