package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.Glide
import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.paging.withPaging
import com.xiaocydx.accompanist.paging.withSwipeRefresh
import com.xiaocydx.accompanist.transition.EnterTransitionController
import com.xiaocydx.accompanist.transition.transform.ReceiverEvent
import com.xiaocydx.accompanist.transition.transform.Transform
import com.xiaocydx.accompanist.transition.transform.findViewHolder
import com.xiaocydx.accompanist.transition.transform.receiverEvent
import com.xiaocydx.accompanist.transition.transform.scrollToPosition
import com.xiaocydx.accompanist.transition.transform.setSenderViews
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.overScrollNever
import com.xiaocydx.cxrv.binding.BindingHolder
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
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.systemBarController
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ItemComplexBinding
import com.xiaocydx.sample.paging.complex.ComplexItem.Companion.TYPE_AD
import com.xiaocydx.sample.paging.complex.ComplexItem.Companion.TYPE_VIDEO
import com.xiaocydx.sample.transition.EnterTransitionActivity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * 视频流的过渡动画和分页加载示例
 *
 * 此类交互具有关联性，例如在[VideoStreamFragment]进行滚动，滚动后的选中位置，
 * 会同步回当前页面申请重新布局，注意：申请重新布局不是为了同步选中位置的视图。
 * 同步申请重新布局是为了提前将页面的内容准备好，例如重新布局后加载新url的图片，
 * 让退出[VideoStreamFragment]的过程能看到准备好的内容，而不是一大堆占位图。
 *
 * [AdFragment]沿用了[EnterTransitionController]解决过渡动画卡顿的问题，
 * [EnterTransitionController]全部的示例代码在[EnterTransitionActivity]。
 *
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListActivity : AppCompatActivity(), SystemBar {
    private lateinit var rv: RecyclerView
    private lateinit var adapter: ListAdapter<ComplexItem, *>
    private val viewModel: ComplexListViewModel by viewModels()

    init {
        systemBarController { navigationBarEdgeToEdge = EdgeToEdge.Gesture }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initCollect()
    }

    private fun initView() {
        val requestManager = Glide.with(this)
        adapter = bindingAdapter(
            uniqueId = ComplexItem::id,
            inflate = ItemComplexBinding::inflate
        ) {
            onBindView {
                requestManager.load(it.coverUrl)
                    .sizeMultiplier(0.5f)
                    .placeholder(R.color.placeholder_color)
                    .into(ivCover)
                tvTitle.text = it.name
                tvType.text = it.type
                tvType.setBackgroundColor(it.typeColor)
            }
            doOnItemClick(intervalMs = 200, action = ::showVideoStreamOrAd)
        }

        rv = RecyclerView(this)
            .apply { id = viewModel.rvId }
            .layoutParams(matchParent, matchParent)
            .overScrollNever().grid(spanCount = 2).fixedSize()
            .divider { size(5.dp).edge(Edge.all()) }
            .adapter(adapter.withPaging())

        rv.insets().gestureNavBarEdgeToEdge()
        setContentView(rv.withSwipeRefresh(adapter))
    }

    private fun initCollect() {
        Transform.receiverEvent(
            activity = this, token = viewModel.sharedId
        ).onEach(::setSenderViews).launchIn(lifecycleScope)

        viewModel.complexPagingFlow
            .onEach(adapter.pagingCollector)
            .launchRepeatOnLifecycle(lifecycle)
    }

    private fun showVideoStreamOrAd(holder: ViewHolder?, item: ComplexItem) {
        setSenderViews(holder)
        val args = viewModel.setReceiverState(item)
        when (item.type) {
            TYPE_VIDEO -> VideoStreamFragment.show(this, args)
            TYPE_AD -> AdFragment.show(this, args)
        }
    }

    private fun setSenderViews(event: ReceiverEvent) = with(Transform) {
        val position = adapter.currentList.indexOfFirst { it.id == event.id }
        scrollToPosition(rv, adapter, position)
        rv.doOnLayout { setSenderViews(findViewHolder(rv, adapter, position)) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun setSenderViews(holder: ViewHolder?) = with(Transform) {
        val binding = (holder as? BindingHolder<ItemComplexBinding>)?.binding ?: return
        setSenderViews(activity = this@ComplexListActivity, root = binding.root, image = binding.ivCover)
    }
}