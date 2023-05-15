package com.xiaocydx.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.*
import com.xiaocydx.sample.concat.HeaderFooterActivity
import com.xiaocydx.sample.databinding.ItemStartBinding
import com.xiaocydx.sample.itemclick.ItemClickActivity
import com.xiaocydx.sample.itemselect.MultiSelectionActivity
import com.xiaocydx.sample.itemselect.SingleSelectionActivity
import com.xiaocydx.sample.itemtouch.ItemTouchActivity
import com.xiaocydx.sample.multitype.MultiTypeActivity
import com.xiaocydx.sample.nested.NestedListActivity
import com.xiaocydx.sample.paging.PagingActivity
import com.xiaocydx.sample.paging.article.ArticleListActivity
import com.xiaocydx.sample.payload.PayloadActivity
import com.xiaocydx.sample.viewpager2.ViewPager2Activity
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity

/**
 * @author xcc
 * @date 2022/2/17
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(RecyclerView(this).apply {
            linear()
            fixedSize()
            adapter(bindingAdapter(
                uniqueId = StartItem::text,
                inflate = ItemStartBinding::inflate
            ) {
                submitStartList()
                doOnItemClick(
                    target = { binding.btnStart }
                ) { _, item -> item.start(context) }
                onBindView { btnStart.text = it.text }
            })
            overScrollNever()
            withLayoutParams(matchParent, matchParent)
        })
    }

    private fun ListAdapter<StartItem, *>.submitStartList() {
        submitList(listOf(
            StartItem<NestedListActivity>("嵌套列表示例"),
            StartItem<ItemClickActivity>("item点击、长按示例"),
            StartItem<ItemTouchActivity>("item拖动、侧滑示例"),
            StartItem<SingleSelectionActivity>("item单项选择示例"),
            StartItem<MultiSelectionActivity>("item多项选择示例"),
            StartItem<MultiTypeActivity>("item多类型示例"),
            StartItem<PayloadActivity>("Payload更新示例"),
            StartItem<HeaderFooterActivity>("HeaderFooter示例"),
            StartItem<PagingActivity>("分页加载示例（本地测试）"),
            StartItem<ArticleListActivity>("分页加载示例（网络请求）"),
            StartItem<ViewPager2Activity>("ViewPager2共享池示例"),
            StartItem<LoopPagerActivity>("ViewPager2循环页面示例")
        ))
    }

    private inline fun <reified T : Activity> StartItem(text: String): StartItem {
        return StartItem(text, T::class.java)
    }

    private data class StartItem(val text: String, val clazz: Class<out Activity>) {
        fun start(context: Context) {
            context.startActivity(Intent(context, clazz))
        }
    }
}