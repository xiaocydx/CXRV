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
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.sample.R
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2023/8/17
 */
class NormalHomeFragment : Fragment(R.layout.fragment_list_state) {
    private val sharedViewModel: ListStateSharedViewModel by activityViewModels()
    private val normalViewModel: NormalListStateViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(R.id.container1, NormalListStateFragment1())
                add(R.id.container2, NormalListStateFragment2())
            }
        }

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

abstract class AbsNormalListStateFragment : Fragment() {
    private lateinit var fooAdapter: FooListAdapter
    private val normalViewModel: NormalListStateViewModel by viewModels(
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
            .overScrollNever().adapter(fooAdapter)
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        normalViewModel.flow()
            .onEach(fooAdapter.listCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()
    }

    protected abstract fun RecyclerView.initView(): RecyclerView
}

class NormalListStateFragment1 : AbsNormalListStateFragment() {

    override fun RecyclerView.initView() = linear()
        .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFF979EC4.toInt()) }
}

class NormalListStateFragment2 : AbsNormalListStateFragment() {

    override fun RecyclerView.initView() = grid(spanCount = 2)
        .divider(5.dp, 5.dp) { edge(Edge.all()).color(0xFF9DAA8F.toInt()) }
}