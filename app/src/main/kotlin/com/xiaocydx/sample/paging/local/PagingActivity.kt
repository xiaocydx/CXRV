package com.xiaocydx.sample.paging.local

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.MenuContainerBinding
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.extensions.initMenuList
import com.xiaocydx.sample.paging.local.MenuAction.GIRD
import com.xiaocydx.sample.paging.local.MenuAction.LINEAR
import com.xiaocydx.sample.paging.local.MenuAction.STAGGERED
import com.xiaocydx.sample.snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        if (savedInstanceState == null) replace<LinearPagingFragment>()
    }

    private fun contentView() = MenuContainerBinding
        .inflate(layoutInflater).initMenuList {
            submitList(MenuAction.values().toList())
            doOnSimpleItemClick(::performMenuAction)
        }.apply {
            window.enableGestureNavBarEdgeToEdge()
            sharedViewModel.menuAction.onEach {
                root.closeDrawer(rvMenu)
                root.snackbar().setText(it.text).show()
            }.launchIn(lifecycleScope)
        }.root

    private fun performMenuAction(action: MenuAction) {
        when (action) {
            LINEAR -> replace<LinearPagingFragment>()
            GIRD -> replace<GridPagingFragment>()
            STAGGERED -> replace<StaggeredPagingFragment>()
            else -> {}
        }
        sharedViewModel.submitMenuAction(action)
    }

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(R.id.container, T::class.java, null) }
    }
}