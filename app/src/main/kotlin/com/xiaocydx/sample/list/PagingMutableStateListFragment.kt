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
import com.xiaocydx.accompanist.paging.replaceWithSwipeRefresh
import com.xiaocydx.accompanist.paging.withPaging
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.ListOwner
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.asStateFlow
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.removeItem
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.R
import com.xiaocydx.sample.common.FooListAdapter
import com.xiaocydx.sample.databinding.FragmentMutableStateListBinding
import com.xiaocydx.sample.list.MenuAction.ClearAll
import com.xiaocydx.sample.list.MenuAction.ClearEven
import com.xiaocydx.sample.list.MenuAction.ClearOdd
import com.xiaocydx.sample.list.MenuAction.InsertItem
import com.xiaocydx.sample.list.MenuAction.Refresh
import com.xiaocydx.sample.list.MenuAction.RemoveItem
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
class PagingMutableStateListFragment : Fragment(R.layout.fragment_mutable_state_list) {
    private val sharedViewModel: MutableStateListSharedViewModel by activityViewModels()
    private val pagingViewModel: PagingMutableStateListViewModel by viewModels()
    private val fooAdapter1 = FooListAdapter()
    private val fooAdapter2 = FooListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FragmentMutableStateListBinding.bind(view).initView().initCollect()
    }

    @SuppressLint("SetTextI18n")
    private fun FragmentMutableStateListBinding.initView() = apply {
        tvFoo1.text = "PagingList-1"
        fooAdapter1.doOnItemClick { fooAdapter1.removeItem(it) }
        rvFoo1.linear().adapter(fooAdapter1.withPaging())
            .divider { size(5.dp).edge(Edge.all()).color(0xFF979EC4.toInt()) }
            .replaceWithSwipeRefresh(fooAdapter1)

        tvFoo2.text = "PagingList-2"
        fooAdapter2.doOnItemClick { fooAdapter2.removeItem(it) }
        rvFoo2.grid(spanCount = 2).adapter(fooAdapter2.withPaging())
            .divider { size(5.dp).edge(Edge.all()).color(0xFF979EC4.toInt()) }
            .replaceWithSwipeRefresh(fooAdapter2)
    }

    private fun FragmentMutableStateListBinding.initCollect() = apply {
        pagingViewModel.flow
            .onEach(fooAdapter1.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)

        pagingViewModel.flow
            .onEach(fooAdapter2.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)

        sharedViewModel.menuAction.onEach { action ->
            when (action) {
                Refresh -> pagingViewModel.refresh()
                InsertItem -> pagingViewModel.insertItem()
                RemoveItem -> pagingViewModel.removeItem()
                ClearOdd -> pagingViewModel.clearOdd()
                ClearEven -> pagingViewModel.clearEven()
                ClearAll -> pagingViewModel.clearAll()
                else -> return@onEach
            }
        }.launchIn(viewLifecycleScope)
    }
}