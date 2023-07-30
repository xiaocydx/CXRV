package com.xiaocydx.sample.paging.complex

import android.app.SharedElementCallback
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.xiaocydx.cxrv.binding.BindingAdapter
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ItemComplexBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.showToast
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * TODO: 2023/7/30
 *  1. 修正重建数据异常的问题
 *  2. 补充类型过滤的处理
 *  3. 出现内存泄漏检查，原因？
 *
 * @author xcc
 * @date 2023/7/30
 */
class ComplexListActivity : AppCompatActivity() {
    private lateinit var rvComplex: RecyclerView
    private lateinit var complexAdapter: BindingAdapter<ComplexItem, ItemComplexBinding>
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
        }

        rvComplex = RecyclerView(this)
            .apply { id = viewModel.rvId }
            .layoutParams(matchParent, matchParent)
            .overScrollNever().grid(spanCount = 2).fixedSize()
            .divider(width = 5.dp, height = 5.dp) { edge(Edge.all()) }
            .adapter(complexAdapter.withPaging())

        setContentView(rvComplex.withSwipeRefresh(complexAdapter))
    }

    private fun initCollect() {
        viewModel.flow
            .onEach(complexAdapter.pagingCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()

        complexAdapter.doOnItemClick { holder, item ->
            when (item.type) {
                ComplexItem.TYPE_IMAGE,
                ComplexItem.TYPE_AD -> showToast("点击${item.type}${item.title}")
                ComplexItem.TYPE_VIDEO -> VideoStreamHelper.start(
                    activity = this,
                    sharedElement = holder.binding.ivCover,
                    targetItem = item,
                    currentList = viewModel.currentList,
                    nextKey = viewModel.nextKey
                )
            }
        }

        var pendingPosition = -1
        VideoStreamHelper.event(this).onEach { event ->
            when (event) {
                is VideoStreamEvent.Select -> {
                    pendingPosition = viewModel.findPosition(event.id)
                    pendingPosition.takeIf { it >= 0 }?.let(rvComplex::scrollToPosition)
                }
                is VideoStreamEvent.Refresh -> viewModel.syncRefresh(event.data, event.nextKey)
                is VideoStreamEvent.Append -> viewModel.syncAppend(event.data, event.nextKey)
            }
        }.launchIn(lifecycleScope)

        window.sharedElementsUseOverlay = false
        setExitSharedElementCallback(MaterialContainerExitSharedElementCallback {
            @Suppress("UNCHECKED_CAST")
            val sharedElement = complexAdapter.recyclerView
                ?.takeIf { pendingPosition >= 0 }
                ?.findViewHolderForAdapterPosition(pendingPosition)
                ?.let { it as? BindingHolder<ItemComplexBinding> }
                ?.binding?.ivCover
            pendingPosition = -1
            sharedElement
        })
    }

    private fun initEdgeToEdge() {
        window.enableGestureNavBarEdgeToEdge()
        rvComplex.enableGestureNavBarEdgeToEdge()
    }

    private class MaterialContainerExitSharedElementCallback(
        private val exitSharedElement: () -> View?
    ) : SharedElementCallback() {
        override fun onCaptureSharedElementSnapshot(
            sharedElement: View?,
            viewToGlobalMatrix: Matrix?,
            screenBounds: RectF?
        ): Parcelable {
            // TODO: 2023/7/31  重置为1f的时机慢了，原因？
            // sharedElement?.alpha = 1f
            return super.onCaptureSharedElementSnapshot(sharedElement, viewToGlobalMatrix, screenBounds)
        }

        override fun onMapSharedElements(
            names: MutableList<String>,
            sharedElements: MutableMap<String, View>
        ) {
            val name = names.firstOrNull() ?: return
            val sharedElement = exitSharedElement() ?: return
            sharedElements[name] = sharedElement
        }
    }
}