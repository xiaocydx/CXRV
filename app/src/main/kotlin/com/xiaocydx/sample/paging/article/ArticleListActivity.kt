package com.xiaocydx.sample.paging.article

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.accompanist.lifecycle.launchRepeatOnLifecycle
import com.xiaocydx.accompanist.paging.withPaging
import com.xiaocydx.accompanist.paging.withSwipeRefresh
import com.xiaocydx.accompanist.view.dp
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.overScrollNever
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.divider.Edge
import com.xiaocydx.cxrv.divider.divider
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.itemtouch.itemTouch
import com.xiaocydx.cxrv.list.ListAdapter
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.fixedSize
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.paging.PagingPrefetch
import com.xiaocydx.cxrv.paging.onEach
import com.xiaocydx.cxrv.paging.pagingCollector
import com.xiaocydx.insets.insets
import com.xiaocydx.insets.systembar.EdgeToEdge
import com.xiaocydx.insets.systembar.SystemBar
import com.xiaocydx.insets.systembar.systemBarController
import com.xiaocydx.sample.databinding.ItemArticleBinding

/**
 * Paging示例代码（网络请求）
 *
 * [ArticleListViewModel]已使用预取策略[PagingPrefetch.ItemCount]。
 *
 * @author xcc
 * @date 2022/3/17
 */
class ArticleListActivity : AppCompatActivity(), SystemBar {
    private lateinit var rvArticle: RecyclerView
    private lateinit var articleAdapter: ListAdapter<Article, *>
    private val viewModel: ArticleListViewModel by viewModels()

    init {
        systemBarController { navigationBarEdgeToEdge = EdgeToEdge.Gesture }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initCollect()
    }

    private fun initView() {
        articleAdapter = bindingAdapter(
            uniqueId = Article::id,
            inflate = ItemArticleBinding::inflate
        ) {
            onBindView {
                tvTitle.text = it.title
                tvDesc.text = it.desc
                tvType.text = it.type
            }
            itemTouch {
                onSwipe { position, _ -> viewModel.removeArticle(position) }
                onDrag { from, to -> viewModel.moveArticle(from, to) }
            }
            doOnItemClick { snackbar().setText("未实现文章跳转").show() }
        }

        rvArticle = RecyclerView(this)
            .apply { id = viewModel.rvId }
            .layoutParams(matchParent, matchParent)
            .overScrollNever().linear().fixedSize()
            .divider { size(10.dp).edge(Edge.all()) }
            .adapter(articleAdapter.withPaging())

        rvArticle.insets().gestureNavBarEdgeToEdge()
        setContentView(rvArticle.withSwipeRefresh(articleAdapter))
    }

    private fun initCollect() {
        viewModel.pagingFlow
            .onEach(articleAdapter.pagingCollector)
            .launchRepeatOnLifecycle(lifecycle)
    }
}