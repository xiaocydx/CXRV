package com.xiaocydx.sample.liststate

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.removeItem
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.FragmentListStateBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.paging.config.replaceWithSwipeRefresh
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2023/8/17
 */
class PagingListStateFragment : Fragment(R.layout.fragment_list_state) {
    private val sharedViewModel: ListStateSharedViewModel by activityViewModels()
    private val pagingViewModel: PagingListStateViewModel by viewModels()
    private val fooAdapter1 = FooListAdapter()
    private val fooAdapter2 = FooListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FragmentListStateBinding.bind(view).initView().initCollect()
    }

    @SuppressLint("SetTextI18n")
    private fun FragmentListStateBinding.initView() = apply {
        tvFoo1.text = "PagingList-1"
        fooAdapter1.doOnSimpleItemClick(fooAdapter1::removeItem)
        rvFoo1.linear().fixedSize().adapter(fooAdapter1.withPaging())
            .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFF979EC4.toInt()) }
            .replaceWithSwipeRefresh(fooAdapter1)

        tvFoo2.text = "PagingList-2"
        fooAdapter2.doOnSimpleItemClick(fooAdapter2::removeItem)
        rvFoo2.grid(spanCount = 2).fixedSize().adapter(fooAdapter2.withPaging())
            .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFF979EC4.toInt()) }
            .replaceWithSwipeRefresh(fooAdapter2)
    }

    private fun FragmentListStateBinding.initCollect() = apply {
        pagingViewModel.flow
            .onEach(fooAdapter1.pagingCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()

        pagingViewModel.flow
            .onEach(fooAdapter2.pagingCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()

        sharedViewModel.menuAction.onEach { action ->
            when (action) {
                MenuAction.REFRESH -> pagingViewModel.refresh()
                MenuAction.LIST_STATE_INSERT_ITEM -> pagingViewModel.insertItem()
                MenuAction.LIST_STATE_REMOVE_ITEM -> pagingViewModel.deleteItem()
                MenuAction.CLEAR_ODD_ITEM -> pagingViewModel.clearOddItem()
                MenuAction.CLEAR_EVEN_ITEM -> pagingViewModel.clearEvenItem()
                MenuAction.CLEAR_ALL_ITEM -> pagingViewModel.clearAllItem()
                else -> return@onEach
            }
        }.launchIn(viewLifecycleScope)
    }
}