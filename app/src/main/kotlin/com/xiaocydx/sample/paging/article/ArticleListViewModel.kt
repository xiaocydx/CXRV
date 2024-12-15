package com.xiaocydx.sample.paging.article

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingPrefetch
import com.xiaocydx.cxrv.paging.flowMap
import com.xiaocydx.cxrv.paging.itemMap
import com.xiaocydx.cxrv.paging.storeIn

/**
 * @author xcc
 * @date 2022/3/17
 */
class ArticleListViewModel(repository: ArticleRepository = ArticleRepository) : ViewModel() {
    private val list = MutableStateList<Article>()
    private val pager = repository.getArticlePager(
        initKey = 0,
        config = PagingConfig(
            pageSize = 15,
            appendPrefetch = PagingPrefetch.ItemCount(5)
        )
    )

    val rvId = ViewCompat.generateViewId()
    val pagingFlow = pager.flow
        .flowMap { it.itemMap { _, item -> item.toUi() } }
        .storeIn(list, viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun removeArticle(position: Int) {
        list.removeAt(position)
    }

    fun moveArticle(from: Int, to: Int): Boolean {
        return list.moveAt(from, to)
    }
}

data class Article(
    val id: Int,
    val title: String,
    val desc: String,
    val type: String
)

private fun ArticleData.toUi() = Article(
    id = id,
    title = title ?: "",
    desc = when {
        !author.isNullOrEmpty() -> "作者：${author}"
        !shareUser.isNullOrEmpty() -> "分享人：${shareUser}"
        else -> ""
    },
    type = "分类：${superChapterName ?: ""} / ${chapterName ?: ""}"
)