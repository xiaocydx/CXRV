package com.xiaocydx.sample.liststate

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.MenuContainerBinding
import com.xiaocydx.sample.extensions.initMenuList
import com.xiaocydx.sample.liststate.MenuAction.NORMAL
import com.xiaocydx.sample.liststate.MenuAction.PAGING
import com.xiaocydx.sample.snackbar
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [ListState]示例代码
 *
 * [ListState]可用于普通列表和分页列表两种场景，
 * [NormalListStateFragment]展示了普通列表场景，
 * [PagingListStateFragment]展示了分页列表场景。
 *
 * @author xcc
 * @date 2023/8/17
 */
class ListStateActivity : AppCompatActivity() {
    private val sharedViewModel: ListStateSharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        if (savedInstanceState == null) replace<NormalListStateFragment>()
    }

    private fun contentView() = MenuContainerBinding
        .inflate(layoutInflater).initMenuList {
            submitList(MenuAction.values().toList())
            doOnSimpleItemClick(::performMenuAction)
        }.apply {
            sharedViewModel.menuAction.onEach {
                root.closeDrawer(rvMenu)
                root.snackbar().setText(it.text).show()
            }.launchIn(lifecycleScope)
        }.root

    private fun performMenuAction(action: MenuAction) {
        when (action) {
            NORMAL -> replace<NormalListStateFragment>()
            PAGING -> replace<PagingListStateFragment>()
            else -> {}
        }
        sharedViewModel.submitMenuAction(action)
    }

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(R.id.container, T::class.java, null) }
    }
}