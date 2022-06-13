package com.xiaocydx.sample.paging.article

import android.os.Bundle
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.enableViewBoundCheckCompat
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.sample.databinding.ItemArticleBinding
import com.xiaocydx.sample.doOnApplyWindowInsetsCompat
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.getNavigationBarHeight
import com.xiaocydx.sample.navigationBarEdgeToEdge
import com.xiaocydx.sample.paging.config.paging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.retrofit.ArticleInfo
import kotlinx.coroutines.flow.launchIn

/**
 * 分页加载示例代码（网络请求）
 *
 * @author xcc
 * @date 2022/3/17
 */
class ArticleListActivity : AppCompatActivity() {
    private lateinit var rvArticle: RecyclerView
    private lateinit var listAdapter: ListAdapter<ArticleInfo, *>
    private val viewModel: ArticleListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObserve()
        initEdgeToEdge()
    }

    private fun initView() {
        rvArticle = RecyclerView(this).apply {
            id = viewModel.rvId
            linear().fixedSize().divider {
                width = 10.dp
                height = 10.dp
                horizontalEdge = true
                verticalEdge = true
            } paging bindingAdapter(
                uniqueId = ArticleInfo::id,
                inflate = ItemArticleBinding::inflate
            ) {
                listAdapter = this
                onBindView {
                    tvTitle.text = it.title ?: ""
                    tvAuthor.text = ("作者：${it.author ?: ""}")
                }
            }
            overScrollMode = OVER_SCROLL_NEVER
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        setContentView(rvArticle.withSwipeRefresh(listAdapter))
    }

    private fun initObserve() {
        viewModel.flow
            .onEach(listAdapter)
            .flowWithLifecycle(lifecycle)
            .launchIn(lifecycleScope)
    }

    private fun initEdgeToEdge() {
        window.navigationBarEdgeToEdge()
        rvArticle.clipToPadding = false
        rvArticle.layoutManager?.enableViewBoundCheckCompat()
        rvArticle.doOnApplyWindowInsetsCompat { view, insets, initialState ->
            val paddingBottom = initialState.paddings.bottom
            view.updatePadding(bottom = insets.getNavigationBarHeight() + paddingBottom)
        }
    }
}