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
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.foo.FooListAdapter
import com.xiaocydx.sample.foo.FooListViewModel
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.paging.local.MenuAction.CLEAR_ALL
import com.xiaocydx.sample.paging.local.MenuAction.INSERT_ITEM
import com.xiaocydx.sample.paging.local.MenuAction.REFRESH
import com.xiaocydx.sample.paging.local.MenuAction.REMOVE_ITEM
import com.xiaocydx.sample.paging.local.MenuAction.REVERSE
import com.xiaocydx.sample.viewLifecycleScope
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
        rvPaging.enableGestureNavBarEdgeToEdge()
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
                REFRESH -> fooViewModel.refresh()
                REVERSE -> reverseLayout()
                INSERT_ITEM -> fooViewModel.insertItem()
                REMOVE_ITEM -> fooViewModel.removeItem()
                CLEAR_ALL -> fooViewModel.clearAll()
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