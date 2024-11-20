package com.xiaocydx.sample.paging.complex

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_VERTICAL
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.xiaocydx.accompanist.launchSafely
import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycle
import com.xiaocydx.accompanist.lifecycle.viewLifecycleScope
import com.xiaocydx.accompanist.paging.loadStatesFlow
import com.xiaocydx.accompanist.paging.withSwipeRefresh
import com.xiaocydx.accompanist.transition.transform.ImageTransform
import com.xiaocydx.accompanist.transition.transform.Transform
import com.xiaocydx.accompanist.transition.transform.createTransitionProvider
import com.xiaocydx.accompanist.transition.transform.postponeEnterTransition
import com.xiaocydx.accompanist.transition.transform.setReceiverEventEmitter
import com.xiaocydx.accompanist.transition.transform.setTransformTransition
import com.xiaocydx.accompanist.videostream.VideoStreamItem
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.accompanist.viewpager2.registerOnPageChangeCallback
import com.xiaocydx.cxrv.binding.BindingHolder
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.Pager
import com.xiaocydx.cxrv.paging.broadcastIn
import com.xiaocydx.cxrv.paging.isSuccess
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.navigationBars
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.systemBarController
import com.xiaocydx.sample.databinding.ItemVideoStreamBinding
import kotlinx.coroutines.flow.first

/**
 * 视频流页面
 *
 * 在实际场景中，视频流页面可能会供多处业务复用，视频流的数据通过其他业务数据过滤、转换而来，
 * 分页加载是处理起来比较麻烦的场景，需要调用处页面和视频流页面共享分页数据来源和加载状态，
 * 共享分页数据来源和加载状态的做法，能彻底解决两个页面分开加载再进行同步而产生的一致性问题。
 *
 * 示例代码是构建一个分页数据流，它会发射分页数据容器，其中包含分页初始配置和分页事件流，
 * 分页事件流发射加载过程产生的事件，分页事件携带加载状态和列表数据，加载状态保存在[Pager]，
 * 列表数据保存在[MutableStateList]。
 *
 * [Pager]提供原始分页数据流和加载状态，通过[storeIn]得到的最终分页数据流，
 * 支持共享分页数据流、加载状态、列表状态，不满足两个页面分离列表状态的需求，
 * 在[storeIn]之前调用[broadcastIn]，可以将原始分页数据流转换为广播发射，
 * 满足分离列表状态的需求。
 *
 * 若两个页面的[MutableStateList]还需要同步，例如在视频流页面的删除操作，需要同步到调用处页面，
 * 则通过发送事件完成[MutableStateList]的同步即可，这种不涉及分页加载的同步需求，并不难处理。
 *
 * 视频流的数据通过其他业务数据过滤、转换而来，因此存在一页数据过滤完后，没有视频流数据的问题，
 * `AppendTrigger`的实现已解决这个问题，当视频流页面收集到一页空数据时，会自动触发下一页加载，
 * 将[ComplexSource.adKeyRange]的默认值修改为`true`，连续几页不包含视频流数据，可以验证效果。
 *
 * @author xcc
 * @date 2023/7/30
 */
class VideoStreamFragment : Fragment(), SystemBar {
    private lateinit var requestManager: RequestManager
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: ListAdapter<VideoStreamItem, *>
    private val viewModel: VideoStreamViewModel by viewModels()

    init {
        systemBarController {
            navigationBarColor = Color.BLACK
            statusBarEdgeToEdge = EdgeToEdge.Enabled
            navigationBarEdgeToEdge = EdgeToEdge.Gesture
            isAppearanceLightStatusBar = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        requestManager = Glide.with(this)
        viewPager = ViewPager2(requireContext()).apply {
            offscreenPageLimit = 1
            orientation = ORIENTATION_VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        adapter = bindingAdapter(
            uniqueId = VideoStreamItem::id,
            inflate = ItemVideoStreamBinding::inflate
        ) {
            onCreateView {
                tvName.insets().paddings(navigationBars())
            }
            onBindView {
                requestManager.load(it.coverUrl)
                    .fitCenter().into(ivCover)
                tvName.text = it.name
            }
            doOnLongItemClick { _, _ -> smoothScrollToFirst() }
        }

        viewPager.adapter = adapter
        return viewPager.withSwipeRefresh(adapter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setTransformTransitionV1()
        // setTransformTransitionV2()
        viewLifecycleScope.launchSafely {
            // 首次刷新完成后，再选中位置和注册页面回调，这个处理对Fragment重建流程同样适用
            adapter.pagingCollector.loadStatesFlow().first { it.refresh.isSuccess }
            viewPager.setCurrentItem(viewModel.selectedPosition.value, false)
            viewPager.registerOnPageChangeCallback(onSelected = viewModel::selectVideo)
        }

        viewModel.videoPagingFlow
            .onEach(adapter.pagingCollector)
            .launchRepeatOnLifecycle(viewLifecycle)
    }

    /**
     * 设置Fragment的过渡动画，直到目标视频的封面加载完成，才开始过渡动画
     */
    private fun setTransformTransitionV1() {
        val receiver = this@VideoStreamFragment
        val transitionProvider = Transform.createTransitionProvider(
            receiver = receiver,
            receiverRoot = { getCurrentBinding()?.root },
            receiverImage = { getCurrentBinding()?.ivCover },
            imageDecoration = object : ImageTransform.Decoration {
                override fun onDrawOver(isEnter: Boolean, bounds: RectF, fraction: Float, canvas: Canvas) {
                    // 绘制ivCover之后，绘制覆盖在ivCover上面的内容
                    val coverOver = getCurrentBinding()?.coverOver ?: return
                    val alpha = if (isEnter) fraction else 1f - fraction
                    for (i in 0 until coverOver.childCount) coverOver.getChildAt(i).alpha = alpha
                    val x = bounds.centerX() - coverOver.width.toFloat() / 2
                    val y = bounds.centerY() - coverOver.height.toFloat() / 2
                    canvas.translate(x, y)
                    coverOver.draw(canvas)
                }
            }
        )
        Transform.postponeEnterTransition(
            receiver = receiver,
            requestManager = Glide.with(receiver),
            transitionProvider = transitionProvider,
            canStartEnterTransition = { getCurrentBinding()?.ivCover == it }
        )
        Transform.setReceiverEventEmitter(
            token = viewModel.sharedId,
            receiver = receiver,
            viewPager2 = viewPager,
            receiverId = { viewModel.getSelectedId() }
        )
    }

    /**
     * 设置Fragment的过渡动画，使用Sender设置的`root`跟Receiver的`view`完成过渡
     */
    private fun setTransformTransitionV2() {
        val receiver = this@VideoStreamFragment
        Transform.setTransformTransition(receiver)
        Transform.setReceiverEventEmitter(
            token = viewModel.sharedId,
            receiver = receiver,
            viewPager2 = viewPager,
            receiverId = { viewModel.getSelectedId() }
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentBinding() = viewPager.run {
        val rv = getChildAt(0) as RecyclerView
        val holder = rv.findViewHolderForLayoutPosition(currentItem)
        (holder as? BindingHolder<ItemVideoStreamBinding>)?.binding
    }

    private fun smoothScrollToFirst() = viewPager.run {
        if (currentItem == 0) return@run false
        currentItem = 0
        snackbar().setText("长按平滑滚动至首位").show()
        true
    }

    companion object {
        fun show(activity: FragmentActivity, args: Bundle?) {
            val fm = activity.supportFragmentManager
            fm.commit {
                addToBackStack(null)
                add(android.R.id.content, VideoStreamFragment::class.java, args)
            }
        }
    }
}