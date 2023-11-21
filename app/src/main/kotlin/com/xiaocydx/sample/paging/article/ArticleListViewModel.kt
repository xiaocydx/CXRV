package com.xiaocydx.sample.paging.article

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xiaocydx.cxrv.list.MutableStateList
import com.xiaocydx.cxrv.paging.PagingConfig
import com.xiaocydx.cxrv.paging.PagingPrefetch
import com.xiaocydx.cxrv.paging.storeIn
import com.xiaocydx.sample.retrofit.ArticleInfo

/**
 * @author xcc
 * @date 2022/3/17
 */
class ArticleListViewModel(repository: ArticleListRepository = ArticleListRepository) : ViewModel() {
    private val list = MutableStateList<ArticleInfo>()
    private val pager = repository.getArticlePager(
        initKey = 0,
        config = PagingConfig(
            pageSize = 15,
            appendPrefetch = PagingPrefetch.ItemCount(5)
        )
    )
    val rvId = ViewCompat.generateViewId()
    val flow = pager.flow.storeIn(list, viewModelScope)

    fun refresh() {
        pager.refresh()
    }

    fun deleteArticle(position: Int) {
        list.removeAt(position)
    }
}