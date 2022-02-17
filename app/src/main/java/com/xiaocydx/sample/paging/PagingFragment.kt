package com.xiaocydx.sample.paging

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.xiaocydx.recycler.extension.pagingCollector
import com.xiaocydx.recycler.list.addItem
import com.xiaocydx.recycler.list.clear
import com.xiaocydx.recycler.list.removeItemAt
import com.xiaocydx.recycler.list.submitTransform
import com.xiaocydx.sample.paging.MenuAction.*

/**
 * @author xcc
 * @date 2022/2/17
 */
abstract class PagingFragment : Fragment() {
    private val sharedViewModel: SharedViewModel by activityViewModels()
    protected val viewModel: PagingViewModel by viewModels(
        factoryProducer = { PagingViewModel.Factory }
    )
    protected val adapter = FooAdapter()
    protected lateinit var rvPaging: RecyclerView
        private set

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FrameLayout(requireContext()).apply {
        rvPaging = RecyclerView(requireContext()).apply {
            overScrollMode = OVER_SCROLL_NEVER
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        addView(rvPaging)
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initObserve()
    }

    protected abstract fun initView()

    @CallSuper
    protected open fun initObserve() {
        sharedViewModel.menuAction.observe(viewLifecycleOwner) { action ->
            when (action) {
                INCREASE_SPAN_COUNT -> increaseSpanCount()
                DECREASE_SPAN_COUNT -> decreaseSpanCount()
                REVERSE_LAYOUT -> reverseLayout()
                REFRESH -> refresh()
                ADAPTER_INSERT_ITEM -> adapterInsertItem()
                ADAPTER_DELETE_ITEM -> adapterDeleteItem()
                PAGER_INSERT_ITEM -> pagerInsertItem()
                PAGER_DELETE_ITEM -> pagerDeleteItem()
                CLEAR_ODD_ITEM -> clearOddItem()
                CLEAR_EVEN_ITEM -> clearEvenItem()
                CLEAR_ALL_ITEM -> clearAllItem()
                else -> return@observe
            }
        }
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
        adapter.pagingCollector.refresh()
    }

    private fun adapterInsertItem() {
        var lastNum = adapter.currentList.lastOrNull()?.num ?: 0
        val item = viewModel.createFoo(tag = "Adapter", num = ++lastNum)
        adapter.addItem(0, item)
    }

    private fun adapterDeleteItem() {
        adapter.removeItemAt(0)
    }

    private fun pagerInsertItem() {
        viewModel.insertItem()
    }

    private fun pagerDeleteItem() {
        viewModel.deleteItem()
    }

    private fun clearOddItem() {
        adapter.submitTransform {
            filter { it.num % 2 == 0 }
        }
    }

    private fun clearEvenItem() {
        adapter.submitTransform {
            filter { it.num % 2 != 0 }
        }
    }

    private fun clearAllItem() {
        adapter.clear()
    }
}