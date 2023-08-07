package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.R
import com.xiaocydx.sample.awaitPreDraw
import com.xiaocydx.sample.databinding.ItemComplexBinding
import com.xiaocydx.sample.doOnStateChanged
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.paging.complex.transform.SystemBarsContainer
import com.xiaocydx.sample.paging.complex.transform.TransformSender
import com.xiaocydx.sample.paging.complex.transform.setLightStatusBarOnResume
import com.xiaocydx.sample.paging.complex.transform.setWindowSystemBarsColor
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.viewLifecycle
import com.xiaocydx.sample.viewLifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * // TODO: 2023/8/6 测试嵌套的Fragment的周期状态
 *
 * @author xcc
 * @date 2023/8/5
 */
class ComplexListFragment : Fragment(), TransformSender {
    private lateinit var rvComplex: RecyclerView
    private lateinit var complexAdapter: ListAdapter<ComplexItem, *>
    private val viewModel: ComplexListViewModel by viewModels(
        ownerProducer = { parentFragment ?: requireActivity() }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val requestManager = Glide.with(this)
        complexAdapter = bindingAdapter(
            uniqueId = ComplexItem::id,
            inflate = ItemComplexBinding::inflate
        ) {
            onBindView {
                requestManager.load(it.coverUrl).centerCrop()
                    .placeholder(R.color.placeholder_color)
                    .into(ivCover)
                tvTitle.text = it.title
                tvType.text = it.type
                tvType.setBackgroundColor(it.typeColor)
            }

            doOnItemClick { holder, item ->
                when (item.type) {
                    ComplexItem.TYPE_VIDEO -> {
                        viewModel.setPendingInitialState(item.id)
                        forwardTransform(holder.itemView, VideoStreamFragment::class)
                    }
                    ComplexItem.TYPE_AD -> {
                        val args = AdFragment.createArgs(item.id)
                        forwardTransform(holder.itemView, AdFragment::class, args)
                    }
                }
            }
        }

        rvComplex = RecyclerView(requireContext())
            .apply { id = viewModel.rvId }
            .layoutParams(matchParent, matchParent)
            .apply { enableGestureNavBarEdgeToEdge() }
            .overScrollNever().grid(spanCount = 2).fixedSize()
            .divider(width = 5.dp, height = 5.dp) { edge(Edge.all()) }
            .adapter(complexAdapter.withPaging())

        return SystemBarsContainer(requireContext())
            .setLightStatusBarOnResume(this)
            .setWindowSystemBarsColor(this)
            .setGestureNavBarEdgeToEdge(true)
            .attach(rvComplex.withSwipeRefresh(complexAdapter))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycle.doOnStateChanged { source, event ->
            val currentState = source.lifecycle.currentState
            Log.d("ComplexListFragment", "currentState = ${currentState}, event = $event")
        }

        viewModel.complexFlow
            .onEach(complexAdapter.pagingCollector)
            .repeatOnLifecycle(viewLifecycle)
            .launchInLifecycleScope()

        // 下一帧非平滑滚动布局完成后，再查找指定位置的目标view
        viewModel.scrollEvent
            .onEach(rvComplex::scrollToPosition)
            .onEach { rvComplex.awaitPreDraw() }
            .mapNotNull(rvComplex::findViewHolderForAdapterPosition)
            .onEach { setTransformView(it.itemView) }
            .launchIn(viewLifecycleScope)
    }
}