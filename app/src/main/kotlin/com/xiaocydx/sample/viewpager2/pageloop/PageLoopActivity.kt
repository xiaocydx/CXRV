package com.xiaocydx.sample.viewpager2.pageloop

import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.listCollector
import com.xiaocydx.cxrv.list.onEach
import com.xiaocydx.cxrv.viewpager2.pageloop.PageLoopHelper
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.repeatOnLifecycle
import com.xiaocydx.sample.showToast
import com.xiaocydx.sample.withLayoutParams
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlin.math.abs

/**
 * @author xcc
 * @date 2023/5/11
 */
class PageLoopActivity : AppCompatActivity() {
    private val viewModel: ContentViewModel by viewModels()
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: ContentListAdapter
    private lateinit var helper: PageLoopHelper

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

        helper = PageLoopHelper(viewPager2, adapter, extraPageLimit = 2).attach()

        val tlWidth = 30.dp
        val brWidth = 30.dp
        val pageMargin = 10.dp
        viewPager2.offscreenPageLimit = 1
        viewPager2.setPageTransformer(CompositePageTransformer().apply {
            // addTransformer(MarginPageTransformer(pageMargin))
            addTransformer(OverlapSliderTransformer(
                viewPager2.orientation,
                minScale = 0.25f,
                unSelectedItemRotation = 0f,
                unSelectedItemAlpha = 1f,
                itemGap = 0f
            ))
        })

        val rv = viewPager2.getChildAt(0) as RecyclerView
        if (viewPager2.orientation == ViewPager2.ORIENTATION_VERTICAL) {
            rv.setPadding(viewPager2.paddingLeft, tlWidth + abs(pageMargin), viewPager2.paddingRight, brWidth + abs(pageMargin))
        } else {
            rv.setPadding(tlWidth + abs(pageMargin), viewPager2.paddingTop, brWidth + abs(pageMargin), viewPager2.paddingBottom)
        }
        rv.clipToPadding = false
        setContentView(viewPager2)
    }

    private fun initCollect() {
        adapter.doOnItemClick { holder, item ->
            viewModel.append()
            showToast("item.text = ${item.text}\n" +
                    "layoutPosition = ${holder.layoutPosition}\n" +
                    "bindingAdapterPosition = ${holder.bindingAdapterPosition}")
        }

        viewModel.refreshEvent
            .onStart { emit(Unit) }
            .onEach { helper.setCurrentItem(0) }
            .launchIn(lifecycleScope)

        viewModel.flow
            .onEach(adapter.listCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }
}