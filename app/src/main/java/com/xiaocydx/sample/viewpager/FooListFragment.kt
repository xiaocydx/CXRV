package com.xiaocydx.sample.viewpager

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.lifecycle.flowWithLifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import com.xiaocydx.recycler.extension.divider
import com.xiaocydx.recycler.extension.linear
import com.xiaocydx.recycler.extension.onEach
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.paging.FooAdapter
import com.xiaocydx.sample.paging.PagingViewModel
import com.xiaocydx.sample.paging.config.paging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import com.xiaocydx.sample.viewmodel.activityViewModels
import com.xiaocydx.sample.viewpager.shared.SharedRecycledFragment
import kotlinx.coroutines.flow.launchIn

/**
 * @author xcc
 * @date 2022/2/21
 */
class FooListFragment : SharedRecycledFragment() {
    @Suppress("PrivatePropertyName")
    private val TAG = this::class.java.simpleName
    private val key: String
        get() = requireNotNull(arguments?.getString(KEY_FOO))

    private val viewModel: PagingViewModel by activityViewModels(
        key = { key },
        factoryProducer = { PagingViewModel.Factory }
    )
    private val fooAdapter = FooAdapter().apply {
        stateRestorationPolicy = PREVENT_WHEN_EMPTY
    }
    override lateinit var rv: RecyclerView
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "onCreate: key = $key")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = RecyclerView(requireContext()).apply {
        id = viewModel.rvId
        linear()
        divider {
            height = 2.dp
            color = 0xFF9DAA8F.toInt()
        }
        paging(fooAdapter)
        overScrollMode = View.OVER_SCROLL_NEVER
        layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }.let {
        rv = it
        it.withSwipeRefresh(fooAdapter)
    }

    override fun onLazyInitialize() {
        viewModel.flow
            .onEach(fooAdapter)
            .flowWithLifecycle(viewLifecycle)
            .launchIn(viewLifecycleScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(TAG, "onDestroy: key = $key")
    }

    companion object {
        private const val KEY_FOO = "KEY_FOO"

        fun newInstance(key: String): FooListFragment {
            return FooListFragment().apply {
                arguments = Bundle().apply { putString(KEY_FOO, key) }
            }
        }
    }
}