package com.xiaocydx.sample.divider

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.DividerItemDecoration
import com.xiaocydx.cxrv.divider.addDividerItemDecoration
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.databinding.ActivityMenuBinding
import com.xiaocydx.sample.databinding.ItemMenuBinding
import com.xiaocydx.sample.divider.MenuAction.CONCAT
import com.xiaocydx.sample.divider.MenuAction.GIRD
import com.xiaocydx.sample.divider.MenuAction.LINEAR
import com.xiaocydx.sample.divider.MenuAction.STAGGERED
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.snackbar

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
    private lateinit var binding: ActivityMenuBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMenuDrawer()
        if (savedInstanceState == null) replace<LinearDividerFragment>()
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
            LINEAR -> replace<LinearDividerFragment>()
            GIRD -> replace<GridDividerFragment>()
            STAGGERED -> replace<StaggeredDividerFragment>()
            CONCAT -> replace<ConcatDividerFragment>()
            else -> sharedViewModel.submitMenuAction(action)
        }
        binding.root.closeDrawer(binding.rvMenu)
        binding.root.snackbar().setText(action.text).show()
    }

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(binding.container.id, T::class.java, null) }
    }
}