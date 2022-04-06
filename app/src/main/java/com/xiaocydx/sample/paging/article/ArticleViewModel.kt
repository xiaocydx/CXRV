package com.xiaocydx.sample.paging.article

import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.xiaocydx.recycler.list.ListState
import com.xiaocydx.recycler.paging.storeIn
import com.xiaocydx.sample.retrofit.RetrofitInstance
import com.xiaocydx.sample.retrofit.WanAndroidApi

/**
 * @author xcc
 * @date 2022/3/17
 */
class ArticleViewModel(repository: ArticleRepository) : ViewModel() {
    val rvId = ViewCompat.generateViewId()
    val flow = repository
        .getArticleFlow(initKey = 0, pageSize = 15)
        .storeIn(ListState(), viewModelScope)

    companion object Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass == ArticleViewModel::class.java) {
                val api: WanAndroidApi = RetrofitInstance.create()
                return ArticleViewModel(ArticleRepository(api)) as T
            }
            throw IllegalArgumentException()
        }
    }
}