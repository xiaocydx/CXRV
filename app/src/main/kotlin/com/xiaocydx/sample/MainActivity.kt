package com.xiaocydx.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.overScrollNever
import com.xiaocydx.cxrv.binding.bindingAdapter
import com.xiaocydx.cxrv.itemclick.doOnItemClick
import com.xiaocydx.cxrv.list.adapter
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.concat.HeaderFooterActivity
import com.xiaocydx.sample.databinding.ItemStartBinding
import com.xiaocydx.sample.divider.DividerActivity
import com.xiaocydx.sample.itemclick.ItemClickActivity
import com.xiaocydx.sample.itemselect.ItemSelectActivity
import com.xiaocydx.sample.itemtouch.ItemTouchActivity
import com.xiaocydx.sample.liststate.ListStateActivity
import com.xiaocydx.sample.multitype.MultiTypeActivity
import com.xiaocydx.sample.paging.article.ArticleListActivity
import com.xiaocydx.sample.paging.complex.ComplexContainerActivity
import com.xiaocydx.sample.paging.local.PagingActivity
import com.xiaocydx.sample.payload.PayloadActivity
import com.xiaocydx.sample.transition.EnterTransitionActivity
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity
import com.xiaocydx.sample.viewpager2.nested.NestedScrollableActivity
import com.xiaocydx.sample.viewpager2.shared.SharedPoolActivity
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2022/2/17
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = RecyclerView(this)
        .layoutParams(matchParent, matchParent)
        .linear().overScrollNever()
        .adapter(bindingAdapter(
            uniqueId = StartAction::text,
            inflate = ItemStartBinding::inflate
        ) {
            submitList(startActionList())
            doOnItemClick(
                target = { binding.btnStart },
                action = ::performStartAction
            )
            onBindView { btnStart.text = it.text }
        })

    private fun startActionList() = listOf(
        "Item点击、长按示例" to ItemClickActivity::class,
        "Item拖动、侧滑示例" to ItemTouchActivity::class,
        "Item单选、多选示例" to ItemSelectActivity::class,
        "MultiType示例" to MultiTypeActivity::class,
        "Payload更新示例" to PayloadActivity::class,
        "HeaderFooter示例" to HeaderFooterActivity::class,
        "Divider示例" to DividerActivity::class,
        "ListState示例" to ListStateActivity::class,
        "Paging示例（本地测试）" to PagingActivity::class,
        "Paging示例（网络请求）" to ArticleListActivity::class,
        "ViewPager2共享池示例" to SharedPoolActivity::class,
        "ViewPager2循环页面示例" to LoopPagerActivity::class,
        "ViewPager2滚动冲突处理示例" to NestedScrollableActivity::class,
        "Fragment过渡动画卡顿优化示例" to EnterTransitionActivity::class,
        "视频流的过渡动画和分页加载示例" to ComplexContainerActivity::class,
    )

    private fun performStartAction(holder: ViewHolder, action: StartAction) {
        val context = holder.itemView.context
        context.startActivity(Intent(context, action.clazz.java))
    }

    private infix fun String.to(clazz: KClass<out Activity>) = StartAction(this, clazz)

    private data class StartAction(val text: String, val clazz: KClass<out Activity>)
}