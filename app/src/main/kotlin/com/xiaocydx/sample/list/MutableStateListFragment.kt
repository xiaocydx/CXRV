package com.xiaocydx.sample.list

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycleScope
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.asStateFlow
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.cxrv.list.removeItem
import com.xiaocydx.sample.R
import com.xiaocydx.sample.common.FooListAdapter
import com.xiaocydx.sample.databinding.FragmentMutableStateListBinding
import com.xiaocydx.sample.list.MenuAction.CLEAR_ALL
import com.xiaocydx.sample.list.MenuAction.CLEAR_EVEN
import com.xiaocydx.sample.list.MenuAction.CLEAR_ODD
import com.xiaocydx.sample.list.MenuAction.INSERT_ITEM
import com.xiaocydx.sample.list.MenuAction.REFRESH
import com.xiaocydx.sample.list.MenuAction.REMOVE_ITEM
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * [MutableStateList]和[ListAdapter]建立基于[ListOwner]的双向通信，
 * [MutableStateList.asStateFlow]可以同时被多个收集器收集，共享列表状态。
 *
 * **注意**：虽然支持[MutableStateList]和[ListAdapter]之间的双向通信，
 * 但是建议仅通过[MutableStateList]更新列表，这能提高代码的可读性和可维护性。
 *
 * ### 为什么不用[LiveData]或[StateFlow]？
 * [LiveData]和[StateFlow]只能替换列表，让视图控制器执行一次差异计算，不支持细粒度的更新操作。
 * [MutableStateList]支持细粒度的更新操作，例如在视图控制器处于活跃状态时，调用[MutableStateList.add]，
 * 只需要将更新操作以事件的形式发送到视图控制器即可，不需要执行一次差异计算。
 *
 * @author xcc
 * @date 2023/8/17
 */
class MutableStateListFragment : Fragment(R.layout.fragment_mutable_state_list) {
    private val sharedViewModel: MutableStateListSharedViewModel by activityViewModels()
    private val normalViewModel: NormalMutableStateListViewModel by viewModels()
    private val fooAdapter1 = FooListAdapter()
    private val fooAdapter2 = FooListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FragmentMutableStateListBinding.bind(view).initView().initCollect()
    }

    @SuppressLint("SetTextI18n")
    private fun FragmentMutableStateListBinding.initView() = apply {
        tvFoo1.text = "NormalList-1"
        fooAdapter1.doOnSimpleItemClick(fooAdapter1::removeItem)
        rvFoo1.linear().adapter(fooAdapter1).divider {
            size(5.dp).edge(Edge.all()).color(0xFFAAA4A3.toInt())
        }

        tvFoo2.text = "NormalList-2"
        fooAdapter2.doOnSimpleItemClick(fooAdapter2::removeItem)
        rvFoo2.grid(spanCount = 2).adapter(fooAdapter2).divider {
            size(5.dp).edge(Edge.all()).color(0xFFAAA4A3.toInt())
        }
    }

    private fun FragmentMutableStateListBinding.initCollect() = apply {
        normalViewModel.flow
            .onEach(fooAdapter1.listCollector)
            .launchRepeatOnLifecycle(viewLifecycle)

        normalViewModel.flow
            .onEach(fooAdapter2.listCollector)
            .launchRepeatOnLifecycle(viewLifecycle)

        sharedViewModel.menuAction.onEach { action ->
            when (action) {
                REFRESH -> normalViewModel.refresh()
                INSERT_ITEM -> normalViewModel.insertItem()
                REMOVE_ITEM -> normalViewModel.removeItem()
                CLEAR_ODD -> normalViewModel.clearOdd()
                CLEAR_EVEN -> normalViewModel.clearEven()
                CLEAR_ALL -> normalViewModel.clearAll()
                else -> return@onEach
            }
        }.launchIn(viewLifecycleScope)
    }
}