package com.xiaocydx.sample.paging.local

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.xiaocydx.accompanist.lifecycle.viewLifecycleScope
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.overScrollNever
import com.xiaocydx.insets.insets
import com.xiaocydx.sample.common.FooListAdapter
import com.xiaocydx.sample.common.FooListViewModel
import com.xiaocydx.sample.paging.local.MenuAction.ClearAll
import com.xiaocydx.sample.paging.local.MenuAction.InsertItem
import com.xiaocydx.sample.paging.local.MenuAction.Refresh
import com.xiaocydx.sample.paging.local.MenuAction.RemoveItem
import com.xiaocydx.sample.paging.local.MenuAction.Reverse
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * @author xcc
 * @date 2022/2/17
 */
abstract class PagingFragment : Fragment() {
    private val sharedViewModel: PagingSharedViewModel by activityViewModels()
    protected val fooAdapter = FooListAdapter()
    protected lateinit var fooViewModel: FooListViewModel; private set
    protected lateinit var rvPaging: RecyclerView; private set

    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FrameLayout(requireContext()).also {
        fooViewModel = sharedViewModel.getListViewModel(
            key = this.javaClass.canonicalName ?: ""
        )
        rvPaging = RecyclerView(requireContext())
            .apply { id = fooViewModel.rvId }
            .layoutParams(matchParent, matchParent)
            .overScrollNever()

        rvPaging.insets().gestureNavBarEdgeToEdge()
        it.addView(rvPaging)
    }

    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initView()
        initCollect()
    }

    protected abstract fun initView()

    @CallSuper
    protected open fun initCollect() {
        sharedViewModel.menuAction.onEach { action ->
            when (action) {
                Refresh -> fooViewModel.refresh()
                Reverse -> reverseLayout()
                InsertItem -> fooViewModel.insertItem()
                RemoveItem -> fooViewModel.removeItem()
                ClearAll -> fooViewModel.clearAll()
                else -> return@onEach
            }
        }.launchIn(viewLifecycleScope)
    }

    private fun reverseLayout() {
        val reverseLayout = when (val lm = rvPaging.layoutManager) {
            is LinearLayoutManager -> (!lm.reverseLayout).also { lm.reverseLayout = it }
            is StaggeredGridLayoutManager -> (!lm.reverseLayout).also { lm.reverseLayout = it }
            else -> return
        }
        rvPaging.invalidateItemDecorations()
        val parent = rvPaging.parent as? SwipeRefreshLayout ?: return
        parent.isEnabled = !reverseLayout
    }
}