package com.xiaocydx.sample.liststate

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActivityListStateBinding
import com.xiaocydx.sample.databinding.ItemMenuBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.liststate.MenuAction.NORMAL
import com.xiaocydx.sample.liststate.MenuAction.PAGING
import com.xiaocydx.sample.showToast

/**
 * [ListState]示例代码
 *
 * @author xcc
 * @date 2023/8/17
 */
class ListStateActivity : AppCompatActivity() {
    private val sharedViewModel: ListStateSharedViewModel by viewModels()
    private lateinit var binding: ActivityListStateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListStateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMenuDrawer()
        if (savedInstanceState == null) initNormalHome()
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
    }

    private fun performMenuAction(action: MenuAction) {
        when (action) {
            NORMAL -> initNormalHome()
            PAGING -> initPagingHome()
            else -> sharedViewModel.submitMenuAction(action)
        }
        binding.root.closeDrawer(binding.rvMenu)
        showToast(action.text)
    }

    private fun initNormalHome() {
        replaceFragment(NormalListStateFragment())
    }

    private fun initPagingHome() {
        replaceFragment(PagingListStateFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit { replace(R.id.container, fragment) }
    }
}