package com.xiaocydx.sample.paging.article

import android.os.Bundle
import android.view.View.OVER_SCROLL_NEVER
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.recycler.binding.bindingAdapter
import com.xiaocydx.recycler.extension.divider
import com.xiaocydx.recycler.extension.linear
import com.xiaocydx.recycler.list.ListAdapter
import com.xiaocydx.recycler.paging.emitAll
import com.xiaocydx.sample.databinding.ItemArticleBinding
import com.xiaocydx.sample.dp
import com.xiaocydx.sample.launchRepeatOnLifecycle
import com.xiaocydx.sample.paging.config.paging
import com.xiaocydx.sample.paging.config.withSwipeRefresh
import com.xiaocydx.sample.retrofit.ArticleInfo

/**
 * 分页加载示例代码（网络请求）
 *
 * @author xcc
 * @date 2022/3/17
 */
class ArticleActivity : AppCompatActivity() {
    private lateinit var adapter: ListAdapter<ArticleInfo, *>
    private val viewModel: ArticleViewModel by viewModels(
        factoryProducer = { ArticleViewModel.Factory }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
        initObserve()
    }

    private fun initView() {
        val rv = RecyclerView(this).apply {
            id = viewModel.rvId
            linear()
            divider {
                width = 10.dp
                height = 10.dp
                horizontalEdge = true
                verticalEdge = true
            }
            paging(bindingAdapter(
                uniqueId = ArticleInfo::id,
                inflate = ItemArticleBinding::inflate
            ) {
                this@ArticleActivity.adapter = this
                onBindView {
                    tvTitle.text = it.title ?: ""
                    tvAuthor.text = ("作者：${it.author ?: ""}")
                }
            })
            overScrollMode = OVER_SCROLL_NEVER
            layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        }
        setContentView(rv.withSwipeRefresh(adapter))
    }

    private fun initObserve() {
        launchRepeatOnLifecycle {
            adapter.emitAll(viewModel.flow)
        }
    }
}