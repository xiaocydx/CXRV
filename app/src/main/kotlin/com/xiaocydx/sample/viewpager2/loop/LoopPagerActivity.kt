package com.xiaocydx.sample.viewpager2.loop

import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.itemclick.doOnLongItemClick
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.cxrv.viewpager2.loop.LookupDirection
import com.xiaocydx.cxrv.viewpager2.loop.LoopPagerController
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.withLayoutParams
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

/**
 * @author xcc
 * @date 2023/5/11
 */
class LoopPagerActivity : AppCompatActivity() {
    private val viewModel: ContentViewModel by viewModels()
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: ContentListAdapter
    private lateinit var controller: LoopPagerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initCollect()
    }

    private fun initView() {
        adapter = ContentListAdapter()
        viewPager2 = ViewPager2(this).also {
            it.id = viewModel.vp2Id
            it.adapter = adapter
            it.withLayoutParams(MATCH_PARENT, MATCH_PARENT)
        }

        val width = 30.dp
        val margin = 10.dp
        controller = LoopPagerController(viewPager2)
        controller.setAdapter(adapter)
        controller.setPadding(left = width + margin, right = width + margin)

        viewPager2.offscreenPageLimit = 1
        viewPager2.setPageTransformer(CompositePageTransformer().apply {
            addTransformer(MarginPageTransformer(margin))
            addTransformer(OverlapSliderTransformer(
                viewPager2.orientation,
                minScale = 0.25f,
                unSelectedItemRotation = 0f,
                unSelectedItemAlpha = 1f,
                itemGap = 0f
            ))
        })

        setContentView(viewPager2)
    }

    private fun initCollect() {
        adapter.doOnItemClick { holder, item ->
            controller.smoothScrollToPosition(5, LookupDirection.START)
            // viewModel.append()
            // showToast("item.text = ${item.text}\n" +
            //         "layoutPosition = ${holder.layoutPosition}\n" +
            //         "bindingAdapterPosition = ${holder.bindingAdapterPosition}")
        }
        adapter.doOnLongItemClick { holder, item ->
            viewModel.refresh()
            true
        }

        viewModel.refreshEvent
            .onEach { controller.scrollToPosition(0) }
            .launchIn(lifecycleScope)

        viewModel.flow
            .onEach(adapter.listCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }
}