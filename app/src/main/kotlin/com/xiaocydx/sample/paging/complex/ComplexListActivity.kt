package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.paging.complex.transform.TransformContainer
import com.xiaocydx.sample.paging.complex.transform.TransformSender
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.repeatOnLifecycle
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach

/**
 * TODO: 2023/7/30
 *   1. 添加TransformLifecycle
 *   2. 实现退出动画完成才清除图片
 *
 * 分页数据同步示例（视频流）
 *
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListActivity : AppCompatActivity(), TransformContainer, TransformSender {
    private lateinit var rvComplex: RecyclerView
    private lateinit var complexAdapter: ListAdapter<ComplexItem, *>
    private val viewModel: ComplexListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initCollect()
        initEdgeToEdge()
    }

    private fun initView() {
        complexAdapter = bindingAdapter(
            uniqueId = ComplexItem::id,
            inflate = ItemComplexBinding::inflate
        ) {
            val requestManager = Glide.with(this@ComplexListActivity)
            onBindView {
                requestManager.load(it.coverUrl).centerCrop()
                    .placeholder(R.color.placeholder_color)
                    .into(ivCover)
                tvType.text = it.type
                tvType.setBackgroundColor(it.typeColor)
                tvTitle.text = it.title
            }

            doOnItemClick { holder, item ->
                when (item.type) {
                    ComplexItem.TYPE_VIDEO -> {
                        viewModel.setPendingInitialState(item.id)
                        forwardTransform(holder.itemView, VideoStreamFragment::class)
                    }
                    ComplexItem.TYPE_AD -> {
                        // 沿用EnterTransitionController的过渡动画卡顿优化方案
                        forwardTransform(holder.itemView, AdFragment::class)
                    }
                }
            }
        }

        rvComplex = RecyclerView(this)
            .apply { id = viewModel.rvId }
            .layoutParams(matchParent, matchParent)
            .overScrollNever().grid(spanCount = 2).fixedSize()
            .divider(width = 5.dp, height = 5.dp) { edge(Edge.all()) }
            .adapter(complexAdapter.withPaging())
    }

    private fun initCollect() {
        viewModel.complexFlow
            .onEach(complexAdapter.pagingCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()

        // 下一帧非平滑滚动布局完成后，再查找指定位置的目标view
        viewModel.scrollEvent
            .onEach(rvComplex::scrollToPosition)
            .onEach { rvComplex.awaitPreDraw() }
            .mapNotNull(rvComplex::findViewHolderForAdapterPosition)
            .onEach { setTransformView(it.itemView) }
            .launchIn(lifecycleScope)
    }

    private fun initEdgeToEdge() {
        setTransformContentView(
            view = SystemBarsContainer(this)
                .disableDecorFitsSystemWindows(window)
                .setGestureNavBarEdgeToEdge(true)
                .setWindowSystemBarsColor(window)
                .attach(rvComplex.withSwipeRefresh(complexAdapter))
        )
        rvComplex.enableGestureNavBarEdgeToEdge()
    }
}