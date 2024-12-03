package com.xiaocydx.sample.divider

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.cxrv.divider.DividerItemDecoration
import com.xiaocydx.cxrv.divider.addDividerItemDecoration
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.common.initMenuList
import com.xiaocydx.sample.databinding.MenuContainerBinding
import com.xiaocydx.sample.divider.MenuAction.CONCAT
import com.xiaocydx.sample.divider.MenuAction.GIRD
import com.xiaocydx.sample.divider.MenuAction.LINEAR
import com.xiaocydx.sample.divider.MenuAction.STAGGERED
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Divider示例代码
 *
 * 1. [NormalDividerFragment]的子类调用[divider]排除Header和Footer绘制分割线。
 * 2. [ConcatDividerFragment]调用[addDividerItemDecoration]匹配`adapter`绘制分割线。
 * 两个函数的实现都是构建[DividerItemDecoration]，只是适用场景不同。
 *
 * @author xcc
 * @date 2023/10/4
 */
class DividerActivity : AppCompatActivity() {
    private val sharedViewModel: DividerSharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        if (savedInstanceState == null) replace<LinearDividerFragment>()
    }

    private fun contentView() = MenuContainerBinding
        .inflate(layoutInflater).initMenuList {
            submitList(MenuAction.entries.toList())
            doOnItemClick(action = ::performMenuAction)
        }.apply {
            sharedViewModel.menuAction.onEach {
                root.closeDrawer(rvMenu)
                root.snackbar().setText(it.text).show()
            }.launchIn(lifecycleScope)
        }.root

    private fun performMenuAction(action: MenuAction) {
        when (action) {
            LINEAR -> replace<LinearDividerFragment>()
            GIRD -> replace<GridDividerFragment>()
            STAGGERED -> replace<StaggeredDividerFragment>()
            CONCAT -> replace<ConcatDividerFragment>()
            else -> {}
        }
        sharedViewModel.submitMenuAction(action)
    }

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(R.id.container, T::class.java, null) }
    }
}