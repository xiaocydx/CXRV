package com.xiaocydx.sample.liststate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commitNow
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.R
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2023/8/17
 */
class PagingHomeFragment : Fragment(R.layout.fragment_list_state) {
    private val sharedViewModel: ListStateSharedViewModel by activityViewModels()
    private val pagingViewModel: PagingListStateViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.container1, PagingListStateFragment1())
                add(R.id.container2, PagingListStateFragment2())
            }
        }

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

abstract class AbsPagingListStateFragment : Fragment() {
    private lateinit var fooAdapter: FooListAdapter
    private val pagingViewModel: PagingListStateViewModel by viewModels(
        ownerProducer = { parentFragment ?: requireActivity() }
    )

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fooAdapter = FooListAdapter()
        return RecyclerView(requireContext())
            .layoutParams(matchParent, matchParent).initView()
            .overScrollNever().adapter(fooAdapter.withPaging())
            .withSwipeRefresh(fooAdapter)
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pagingViewModel.flow
            .onEach(fooAdapter.pagingCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()
    }

    protected abstract fun RecyclerView.initView(): RecyclerView
}

class PagingListStateFragment1 : AbsPagingListStateFragment() {

    override fun RecyclerView.initView() = linear()
        .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFF979EC4.toInt()) }
}

class PagingListStateFragment2 : AbsPagingListStateFragment() {

    override fun RecyclerView.initView() = grid(spanCount = 2)
        .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFF9DAA8F.toInt()) }
}