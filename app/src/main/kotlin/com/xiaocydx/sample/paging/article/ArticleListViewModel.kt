package com.xiaocydx.sample.paging.article

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingPrefetch
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.retrofit.ArticleInfo

/**
 * @author xcc
 * @date 2022/3/17
 */
class ArticleListViewModel(repository: ArticleListRepository = ArticleListRepository) : ViewModel() {
    private val state = ListState<ArticleInfo>()
    private val pager = repository.getArticlePager(
        initKey = 0,
        config = PagingConfig(
            pageSize = 15,
            appendPrefetch = PagingPrefetch.ItemCount(5)
        )
    )
    val rvId = ViewCompat.generateViewId()
    val flow = pager.flow.storeIn(state, viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun deleteArticle(articleId: Int) {
        state.currentList
            .indexOfFirst { it.id == articleId }
            .takeIf { it != -1 }
            ?.let(state::removeItemAt)
    }
}