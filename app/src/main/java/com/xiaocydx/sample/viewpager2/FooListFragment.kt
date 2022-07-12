package com.xiaocydx.sample.viewpager2

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnAttach
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle.State.*
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.destroyRecycleViews
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.*
import com.xiaocydx.sample.paging.FooAdapter
import com.xiaocydx.sample.paging.FooListViewModel
import com.xiaocydx.sample.paging.config.paging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.viewpager2.nested.isVp2NestedScrollable
import com.xiaocydx.sample.viewpager2.shared.findParentViewPager2
import com.xiaocydx.sample.viewpager2.shared.sharedRecycledViewPool
import kotlinx.coroutines.flow.launchIn

/**
 * @author xcc
 * @date 2022/2/21
 */
class FooListFragment : Fragment() {
    @Suppress("PrivatePropertyName")
    private val TAG = this::class.java.simpleName
    private val sharedViewModel: FooSharedViewModel by activityViewModels()
    private lateinit var listViewModel: FooListViewModel
    private val fooAdapter = FooAdapter()
    private val key: String
        get() = requireNotNull(arguments?.getString(KEY_FOO))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: key = $key")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        listViewModel = sharedViewModel.getListViewModel(key)
        id = listViewModel.rvId
        linear().fixedSize().divider {
            height = 2.dp
            color = 0xFF9DAA8F.toInt()
        }
        paging(fooAdapter)
        overScrollNever()
        withLayoutParams(matchParent, matchParent)
        doOnAttachToViewPager2()
    }.withSwipeRefresh(fooAdapter)

    /**
     * 若[FooListViewModel.isLoaded] = `false`，则当[viewLifecycle]状态为[RESUMED]时，
     * 才收集[FooListViewModel.flow]，开始列表分页加载，达到首次懒加载的目的。
     * 否则当[viewLifecycle]状态为[STARTED]时，收集[FooListViewModel.flow]，
     * 确保[ViewPager2]滚动过程[RecyclerView]及时添加`itemView`。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycle.doOnStateChanged(
            targetState = if (listViewModel.isLoaded) STARTED else RESUMED
        ) {
            listViewModel.flow
                .onEach(fooAdapter.pagingCollector)
                .flowWithLifecycle(viewLifecycle)
                .launchIn(viewLifecycleScope)
        }
    }

    /**
     * 在[RecyclerView.isAttachedToWindow] = `true`之后，向上查找[ViewPager2]父级：
     * 1. 处理[ViewPager2]嵌套[RecyclerView]的滚动冲突。
     * 2. 对[RecyclerView]设置[ViewPager2.sharedRecycledViewPool]。
     * 3. Fragment视图销毁时，将[RecyclerView]的子View、`Scrap`、离屏缓存，
     * 回收进`sharedRecycledViewPool`。
     */
    private fun RecyclerView.doOnAttachToViewPager2() = doOnAttach {
        val vp2 = findParentViewPager2() ?: return@doOnAttach
        isVp2NestedScrollable = true
        setRecycledViewPool(vp2.sharedRecycledViewPool)
        viewLifecycle.doOnStateChanged(targetState = DESTROYED) {
            // 回收进sharedRecycledViewPool的上限，是当前子View数量的2倍，
            // 这是一个简易的策略，意图是最多回收2页满数量的View，供重建复用。
            val maxScrap = childCount * 2
            destroyRecycleViews { _, _ -> maxScrap }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy: key = $key")
    }

    companion object {
        private const val KEY_FOO = "KEY_FOO"

        fun newInstance(key: String): FooListFragment {
            return FooListFragment().apply {
                arguments = Bundle(1).apply { putString(KEY_FOO, key) }
            }
        }
    }
}