package com.xiaocydx.sample.viewpager2.loop

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView.HORIZONTAL
import androidx.viewpager2.widget.MarginPageTransformer
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.*
import com.xiaocydx.cxrv.viewpager2.loop.LookupDirection
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.sample.databinding.ActivityLoopPagerBinding
import com.xiaocydx.sample.databinding.ItemButtonBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.showToast
import kotlinx.coroutines.Job

/**
 * [LoopPagerController]的示例代码
 *
 * @author xcc
 * @date 2023/5/11
 */
class LoopPagerActivity : AppCompatActivity() {
    private val viewModel: ContentViewModel by viewModels()
    private lateinit var binding: ActivityLoopPagerBinding
    private lateinit var adapter: ContentListAdapter
    private lateinit var controller: LoopPagerController
    private var bannerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoopPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initCollect()
    }

    private fun initView() = with(binding) {
        adapter = ContentListAdapter()
        adapter.doOnItemClick { holder, item ->
            showToast("item.text = ${item.text}\n" +
                    "layoutPosition = ${holder.layoutPosition}\n" +
                    "bindingAdapterPosition = ${holder.bindingAdapterPosition}")
        }
        adapter.doOnListChanged {
            // 列表已更改，在下一帧布局完成后，触发requestTransform()修正间距
            viewPager2.doOnPreDraw { viewPager2.requestTransform() }
        }

        controller = LoopPagerController(viewPager2).apply {
            setAdapter(adapter)
            setPadding(left = 50.dp, right = 50.dp)
            setPageTransformer(ScaleInTransformer(), MarginPageTransformer(10.dp))
        }

        rvAction
            .linear(orientation = HORIZONTAL)
            .divider(5.dp, 5.dp) { edge(Edge.horizontal()) }
            .adapter(bindingAdapter(
                uniqueId = LoopPagerAction::ordinal,
                inflate = ItemButtonBinding::inflate
            ) {
                initLoopPagerAction()
                onBindView { root.text = it.text }
            })
    }

    private fun initCollect() {
        adapter.doOnListChanged changed@{
            if (!viewModel.consumeScrollToFirst()) return@changed
            controller.scrollToPosition(0)
        }
        viewModel.flow
            .onEach(adapter.listCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }

    private fun ListAdapter<LoopPagerAction, *>.initLoopPagerAction() {
        val position = 0
        val timeMillis = 1000L
        doOnItemClick { _, item ->
            when (item) {
                LoopPagerAction.REFRESH -> {
                    viewModel.refresh(timeMillis)
                    showToast("${timeMillis / 1000}s后刷新")
                }
                LoopPagerAction.APPEND -> {
                    viewModel.append(timeMillis)
                    showToast("${timeMillis / 1000}s后添加")
                }
                LoopPagerAction.SCROLL -> {
                    controller.scrollToPosition(position)
                    showToast("非平滑滚动至bindingAdapterPosition = $position")
                }
                LoopPagerAction.SMOOTH_SCROLL -> {
                    controller.smoothScrollToPosition(position, LookupDirection.START)
                    showToast("平滑滚动至bindingAdapterPosition = $position")
                }
                LoopPagerAction.LAUNCH_BANNER -> {
                    bannerJob = controller.launchBanner(adapter, lifecycle)
                    showToast("启动Banner轮播交互")
                }
                LoopPagerAction.CANCEL_BANNER -> {
                    bannerJob?.cancel()
                    bannerJob = null
                    showToast("取消Banner轮播交互")
                }
            }
        }
        submitList(LoopPagerAction.values().toList())
    }
}

enum class LoopPagerAction(val text: String) {
    REFRESH("Refresh"),
    APPEND("Append"),
    SCROLL("Scroll"),
    SMOOTH_SCROLL("SmoothScroll"),
    LAUNCH_BANNER("LaunchBanner"),
    CANCEL_BANNER("CancelBanner")
}