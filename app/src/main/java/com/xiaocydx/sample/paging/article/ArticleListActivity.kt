package com.xiaocydx.sample.paging.article

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.list.*
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.sample.*
import com.xiaocydx.sample.databinding.ItemArticleBinding
import com.xiaocydx.sample.paging.config.withPaging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.retrofit.ArticleInfo

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
        initCollect()
        initEdgeToEdge()
    }

    private fun initView() {
        listAdapter = bindingAdapter(
            uniqueId = ArticleInfo::id,
            inflate = ItemArticleBinding::inflate
        ) {
            onBindView {
                tvTitle.text = it.title ?: ""
                tvAuthor.text = ("作者：${it.author ?: ""}")
            }
        }

        rvArticle = RecyclerView(this).apply {
            id = viewModel.rvId
            overScrollNever()
            withLayoutParams(matchParent, matchParent)
            linear().fixedSize().divider {
                width = 10.dp
                height = 10.dp
                horizontalEdge = true
                verticalEdge = true
            }.adapter(listAdapter.withPaging())
        }

        setContentView(rvArticle.withSwipeRefresh(listAdapter))
    }

    private fun initCollect() {
        viewModel.flow
            .onEach(listAdapter.pagingCollector)
            .repeatOnLifecycle(lifecycle)
            .launchInLifecycleScope()
    }

    private fun initEdgeToEdge() {
        window.navigationBarEdgeToEdge()
        rvArticle.clipToPadding = false
        rvArticle.layoutManager?.enableBoundCheckCompat()
        rvArticle.doOnApplyWindowInsetsCompat { view, insets, initialState ->
            val paddingBottom = initialState.paddings.bottom
            view.updatePadding(bottom = insets.getNavigationBarHeight() + paddingBottom)
        }
    }
}