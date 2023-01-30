package com.xiaocydx.sample.paging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.cxrv.list.addItem
import com.xiaocydx.cxrv.list.clear
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.list.submitTransform
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.*
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.foo.FooListViewModel
import com.xiaocydx.sample.paging.MenuAction.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2022/2/17
 */
abstract class PagingFragment : Fragment() {
    private val sharedViewModel: PagingSharedViewModel by activityViewModels()
    protected val fooAdapter = FooListAdapter()
    protected lateinit var fooViewModel: FooListViewModel
        private set
    protected lateinit var rvPaging: RecyclerView
        private set

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FrameLayout(requireContext()).also {
        fooViewModel = sharedViewModel
            .getListViewModel(key = this.javaClass.simpleName)
        rvPaging = RecyclerView(requireContext()).apply {
            id = fooViewModel.rvId
            overScrollNever()
            withLayoutParams(matchParent, matchParent)
            enableGestureNavBarEdgeToEdge()
        }
        it.addView(rvPaging)
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initCollect()
    }

    protected abstract fun initView()

    @CallSuper
    protected open fun initCollect() {
        sharedViewModel.menuAction.onEach { action ->
            when (action) {
                INCREASE_SPAN_COUNT -> increaseSpanCount()
                DECREASE_SPAN_COUNT -> decreaseSpanCount()
                REVERSE_LAYOUT -> reverseLayout()
                REFRESH -> refresh()
                ADAPTER_INSERT_ITEM -> adapterInsertItem()
                ADAPTER_DELETE_ITEM -> adapterDeleteItem()
                LIST_STATE_INSERT_ITEM -> listStateInsertItem()
                LIST_STATE_DELETE_ITEM -> listStateDeleteItem()
                CLEAR_ODD_ITEM -> clearOddItem()
                CLEAR_EVEN_ITEM -> clearEvenItem()
                CLEAR_ALL_ITEM -> clearAllItem()
                else -> return@onEach
            }
        }.launchIn(viewLifecycleScope)
    }

    private fun increaseSpanCount() {
        when (val lm: LayoutManager? = rvPaging.layoutManager) {
            is GridLayoutManager -> lm.spanCount += 1
            is StaggeredGridLayoutManager -> lm.spanCount += 1
            else -> return
        }
        rvPaging.invalidateItemDecorations()
    }

    private fun decreaseSpanCount() {
        when (val lm: LayoutManager? = rvPaging.layoutManager) {
            is GridLayoutManager -> if (lm.spanCount > 1) lm.spanCount -= 1
            is StaggeredGridLayoutManager -> if (lm.spanCount > 1) lm.spanCount -= 1
            else -> return
        }
        rvPaging.invalidateItemDecorations()
    }

    private fun reverseLayout() {
        when (val lm: LayoutManager? = rvPaging.layoutManager) {
            is LinearLayoutManager -> lm.reverseLayout = !lm.reverseLayout
            is StaggeredGridLayoutManager -> lm.reverseLayout = !lm.reverseLayout
            else -> return
        }
        rvPaging.invalidateItemDecorations()
    }

    private fun refresh() {
        // viewModel.refresh()
        fooAdapter.pagingCollector.refresh()
    }

    private fun adapterInsertItem() {
        val item = fooViewModel.createFoo(
            tag = "Adapter",
            num = fooAdapter.currentList.size
        )
        fooAdapter.addItem(0, item)
    }

    private fun adapterDeleteItem() {
        fooAdapter.removeItemAt(0)
    }

    private fun listStateInsertItem() {
        fooViewModel.insertItem()
    }

    private fun listStateDeleteItem() {
        fooViewModel.deleteItem()
    }

    private fun clearOddItem() {
        fooAdapter.submitTransform {
            filter { it.num % 2 == 0 }
        }
    }

    private fun clearEvenItem() {
        fooAdapter.submitTransform {
            filter { it.num % 2 != 0 }
        }
    }

    private fun clearAllItem() {
        fooAdapter.clear()
    }
}