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
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.cxrv.list.removeItem
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.FragmentListStateBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2023/8/17
 */
class NormalListStateFragment : Fragment(R.layout.fragment_list_state) {
    private val sharedViewModel: ListStateSharedViewModel by activityViewModels()
    private val normalViewModel: NormalListStateViewModel by viewModels()
    private val fooAdapter1 = FooListAdapter()
    private val fooAdapter2 = FooListAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        FragmentListStateBinding.bind(view).initView().initCollect()
    }

    @SuppressLint("SetTextI18n")
    private fun FragmentListStateBinding.initView() = apply {
        tvFoo1.text = "NormalList-1"
        fooAdapter1.doOnSimpleItemClick(fooAdapter1::removeItem)
        rvFoo1.linear().fixedSize().adapter(fooAdapter1)
            .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFFAAA4A3.toInt()) }

        tvFoo2.text = "NormalList-2"
        fooAdapter2.doOnSimpleItemClick(fooAdapter2::removeItem)
        rvFoo2.grid(spanCount = 2).fixedSize().adapter(fooAdapter2)
            .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFFAAA4A3.toInt()) }
    }

    private fun FragmentListStateBinding.initCollect() = apply {
        normalViewModel.flow
            .onEach(fooAdapter1.listCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()

        normalViewModel.flow
            .onEach(fooAdapter2.listCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()

        sharedViewModel.menuAction.onEach { action ->
            when (action) {
                MenuAction.REFRESH -> normalViewModel.refresh()
                MenuAction.LIST_STATE_INSERT_ITEM -> normalViewModel.insertItem()
                MenuAction.LIST_STATE_REMOVE_ITEM -> normalViewModel.deleteItem()
                MenuAction.CLEAR_ODD_ITEM -> normalViewModel.clearOddItem()
                MenuAction.CLEAR_EVEN_ITEM -> normalViewModel.clearEvenItem()
                MenuAction.CLEAR_ALL_ITEM -> normalViewModel.clearAllItem()
                else -> return@onEach
            }
        }.launchIn(viewLifecycleScope)
    }
}