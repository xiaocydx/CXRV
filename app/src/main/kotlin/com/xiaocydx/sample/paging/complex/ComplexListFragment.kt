package com.xiaocydx.sample.paging.complex

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
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
import com.xiaocydx.sample.databinding.ItemComplexBinding
import com.xiaocydx.sample.doOnStateChanged
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.enableGestureNavBarEdgeToEdge
import com.xiaocydx.sample.launchRepeatOnLifecycle
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.overScrollNever
import com.xiaocydx.sample.paging.complex.ComplexItem.Companion.TYPE_AD
import com.xiaocydx.sample.paging.complex.ComplexItem.Companion.TYPE_VIDEO
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.transition.enter.EnterTransitionActivity
import com.xiaocydx.sample.transition.enter.EnterTransitionController
import com.xiaocydx.sample.transition.transform.SystemBarsContainer
import com.xiaocydx.sample.transition.transform.TransformContainer
import com.xiaocydx.sample.transition.transform.TransformReceiver
import com.xiaocydx.sample.transition.transform.TransformSceneRoot
import com.xiaocydx.sample.transition.transform.TransformSender
import com.xiaocydx.sample.transition.transform.setLightStatusBarOnResume
import com.xiaocydx.sample.transition.transform.setWindowSystemBarsColor
import com.xiaocydx.sample.viewLifecycle

/**
 * 复合列表页面
 *
 * 跳转至实现了[TransformReceiver]的Fragment，例如[VideoStreamFragment]和[AdFragment]，
 * 当前Fragment的生命周期状态会回退至[STARTED]，避免不必要的绘制（循环动画、文本更新等），
 * 不销毁当前Fragment，原因是这类交互可能还需要对跳转后的Fragment视图进行手势拖动缩放，
 * 此时底部显示当前Fragment的内容，若销毁当前Fragment，则这个需求需要通过其他方式实现，
 * 实际上，Fragment的重建代码也不一定容易编写，例如有些视图的状态恢复起来就比较困难。
 *
 * 此外，这类交互还具有关联性，例如在[VideoStreamFragment]进行滚动，滚动后的选中位置，
 * 会同步回当前Fragment申请重新布局，注意：同步申请重新布局不是为了同步选中位置的视图，
 * 如果只是为了同步选中位置的视图，那么在[VideoStreamFragment]退出前，申请重新布局即可，
 * 同步申请重新布局是为了提前将当前Fragment的内容准备好，例如重新布局后加载新url的图片，
 * 让退出[VideoStreamFragment]的过程能看到准备好的内容，而不是一大堆占位图。
 *
 * 如果上述问题已有解决方案，那么可以修改[TransformSceneRoot]的实现逻辑，销毁当前Fragment，
 * [TransformContainer]、[TransformSender]、[TransformReceiver]提供的函数和组成的结构，
 * 只是为这类交互提供一种轻量的过渡动画方案。
 *
 * [AdFragment]沿用了[EnterTransitionController]解决过渡动画卡顿的问题，
 * [EnterTransitionController]全部的示例代码在[EnterTransitionActivity]。
 *
 * @author xcc
 * @date 2023/8/5
 */
class ComplexListFragment : Fragment(), TransformSender {
    private lateinit var rvComplex: RecyclerView
    private lateinit var complexAdapter: ListAdapter<ComplexItem, *>
    private val complexViewModel: ComplexListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // 跳转至其他Fragment，当前Fragment的生命周期状态会回退至STARTED，
        // Glide默认实现的生命周期监听，需要回退至CREATED才会停止图片加载，
        // 这跟当前所期望的结果相符，若需要暂停动图，则另外做处理。
        val requestManager = Glide.with(this)
        complexAdapter = bindingAdapter(
            uniqueId = ComplexItem::id,
            inflate = ItemComplexBinding::inflate
        ) {
            onBindView {
                requestManager.load(it.coverUrl)
                    .centerCrop().sizeMultiplier(0.25f)
                    .placeholder(R.color.placeholder_color)
                    .into(ivCover)
                tvTitle.text = it.title
                tvType.text = it.type
                tvType.setBackgroundColor(it.typeColor)
            }

            doOnItemClick { holder, item ->
                val args = complexViewModel.setReceiverState(item)
                when (item.type) {
                    TYPE_VIDEO -> forwardTransform(holder.itemView, VideoStreamFragment::class, args)
                    TYPE_AD -> forwardTransform(holder.itemView, AdFragment::class, args)
                }
            }
        }

        rvComplex = RecyclerView(requireContext())
            .apply { id = complexViewModel.rvId }
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
        setupDebugLog()
        complexViewModel.complexFlow
            .onEach(complexAdapter.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)

        // 同步选中位置的简化函数
        launchTransformSync(
            recyclerView = rvComplex,
            contentAdapter = complexAdapter,
            position = complexViewModel.complexPosition,
            transformView = ViewHolder::itemView
        )
    }

    private fun setupDebugLog() {
        viewLifecycle.doOnStateChanged { source, event ->
            val currentState = source.lifecycle.currentState
            Log.d("ComplexListFragment", "currentState = ${currentState}, event = $event")
        }
        complexAdapter.pagingCollector.addLoadStatesListener { _, current ->
            Log.d("ComplexListFragment", "loadStates = $current")
        }
    }
}