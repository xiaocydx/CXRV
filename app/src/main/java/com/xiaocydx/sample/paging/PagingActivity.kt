package com.xiaocydx.sample.paging

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.xiaocydx.recycler.binding.bindingAdapter
import com.xiaocydx.recycler.extension.adapter
import com.xiaocydx.recycler.extension.divider
import com.xiaocydx.recycler.extension.doOnSimpleItemClick
import com.xiaocydx.recycler.extension.linear
import com.xiaocydx.recycler.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActivityPagingBinding
import com.xiaocydx.sample.databinding.ItemMenuBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.paging.MenuAction.*
import com.xiaocydx.sample.showToast

/**
 * @author xcc
 * @date 2022/2/17
 */
class PagingActivity : AppCompatActivity() {
    private val viewModel: SharedViewModel by viewModels()
    private lateinit var binding: ActivityPagingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPagingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initMenuDrawer()
        initLinearLayout()
    }

    private fun initMenuDrawer() {
        binding.rvMenu
            .linear()
            .divider {
                height = 0.5.dp
                color = 0xFFD5D5D5.toInt()
            }.adapter(bindingAdapter(
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
            FORWARD_ITEM_EVENT -> forwardItemEvent()
            else -> viewModel.submitMenuAction(action)
        }
        binding.root.closeDrawer(binding.rvMenu)
        showToast(action.text)
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

    private fun forwardItemEvent() {
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.flContainer, fragment)
            .commit()
        supportActionBar?.title = fragment.javaClass.simpleName
    }
}