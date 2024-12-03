package com.xiaocydx.sample.list

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.common.initMenuList
import com.xiaocydx.sample.databinding.MenuContainerBinding
import com.xiaocydx.sample.list.MenuAction.NORMAL
import com.xiaocydx.sample.list.MenuAction.PAGING
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [MutableStateList]示例代码
 *
 * [MutableStateList]可用于普通列表和分页列表两种场景，
 * [MutableStateListFragment]展示了普通列表场景，
 * [PagingMutableStateListFragment]展示了分页列表场景。
 *
 * @author xcc
 * @date 2023/8/17
 */
class MutableStateListActivity : AppCompatActivity() {
    private val sharedViewModel: MutableStateListSharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        if (savedInstanceState == null) replace<MutableStateListFragment>()
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
            NORMAL -> replace<MutableStateListFragment>()
            PAGING -> replace<PagingMutableStateListFragment>()
            else -> {}
        }
        sharedViewModel.submitMenuAction(action)
    }

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(R.id.container, T::class.java, null) }
    }
}