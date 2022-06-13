package com.xiaocydx.sample.paging.article

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.ListState
import com.xiaocydx.cxrv.list.removeItemAt
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.retrofit.ArticleInfo

/**
 * @author xcc
 * @date 2022/3/17
 */
class ArticleListViewModel(
    repository: ArticleListRepository = ArticleListRepository
) : ViewModel() {
    private val listState = ListState<ArticleInfo>()
    private val pager = repository
        .getArticlePager(initKey = 0, pageSize = 15)
    val rvId = ViewCompat.generateViewId()
    val flow = pager.flow.storeIn(listState, viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun deleteArticle(articleId: Int) {
        listState.currentList
            .indexOfFirst { it.id == articleId }
            .takeIf { it != -1 }
            ?.let(listState::removeItemAt)
    }
}