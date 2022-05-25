package com.xiaocydx.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.extension.findFirstVisibleItemPosition
import com.xiaocydx.sample.click.ItemClickActivity
import com.xiaocydx.sample.selection.MultiSelectionActivity
import com.xiaocydx.sample.selection.SingleSelectionActivity
import com.xiaocydx.sample.multitype.MultiTypeActivity
import com.xiaocydx.sample.nested.NestedListActivity
import com.xiaocydx.sample.paging.PagingActivity
import com.xiaocydx.sample.paging.article.ArticleActivity
import com.xiaocydx.sample.viewpager.ViewPagerActivity

/**
 * @author xcc
 * @date 2022/2/17
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = MyTestLm()
        recyclerView.findFirstVisibleItemPosition()
    }

    class MyTestLm: RecyclerView.LayoutManager() {
        override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
            return RecyclerView.LayoutParams(-1,-1)
        }
    }

    fun startItemClickActivity(view: View) {
        startActivity<ItemClickActivity>()
    }

    fun startSingleSelectionActivity(view: View) {
        startActivity<SingleSelectionActivity>()
    }

    fun startMultiSelectionActivity(view: View) {
        startActivity<MultiSelectionActivity>()
    }

    fun startMultiTypeActivity(view: View) {
        startActivity<MultiTypeActivity>()
    }

    fun startPagingActivity(view: View) {
        startActivity<PagingActivity>()
    }

    fun startArticleActivity(view: View) {
        startActivity<ArticleActivity>()
    }

    fun startViewPagerActivity(view: View) {
        startActivity<ViewPagerActivity>()
    }

    fun startNestedListActivity(view: View) {
        startActivity<NestedListActivity>()
    }

    private inline fun <reified T : Activity> startActivity() {
        startActivity(Intent(this, T::class.java))
    }
}