@file:Suppress("FunctionName", "SpellCheckingInspection")

package com.xiaocydx.sample

import android.app.Activity
import android.content.Intent
import androidx.annotation.CheckResult
import androidx.fragment.app.FragmentActivity
import com.xiaocydx.sample.SampleItem.Category
import com.xiaocydx.sample.SampleItem.Element
import com.xiaocydx.sample.animatable.AnimatableMediatorActivity
import com.xiaocydx.sample.concat.HeaderFooterActivity
import com.xiaocydx.sample.divider.DividerActivity
import com.xiaocydx.sample.itemclick.ItemClickActivity
import com.xiaocydx.sample.itemselect.ItemSelectActivity
import com.xiaocydx.sample.itemtouch.ItemTouchActivity
import com.xiaocydx.sample.list.MutableStateListActivity
import com.xiaocydx.sample.multitype.MultiTypeActivity
import com.xiaocydx.sample.paging.article.ArticleListActivity
import com.xiaocydx.sample.paging.complex.ComplexListActivity
import com.xiaocydx.sample.paging.local.PagingActivity
import com.xiaocydx.sample.payload.PayloadActivity
import com.xiaocydx.sample.transition.EnterTransitionActivity
import com.xiaocydx.sample.viewpager2.loop.LoopPagerActivity
import com.xiaocydx.sample.viewpager2.nested.NestedScrollableActivity
import com.xiaocydx.sample.viewpager2.shared.SharedPoolActivity
import kotlin.reflect.KClass

/**
 * @author xcc
 * @date 2024/12/20
 */
class SampleList {
    private val selectedId = R.drawable.ic_sample_selected
    private val unselectedId = R.drawable.ic_sample_unselected
    private val source = listOf(
        ListAdapter(), ViewTypeDelegate(),
        MutableStateList(), Extensions(),
        `cxrv-paging`(), `cxrv-animatable`(),
        `cxrv-viewpager2`(), Scene()
    ).flatten().toMutableList()

    @CheckResult
    fun toggle(category: Category): List<SampleItem> {
        val position = source.indexOf(category)
        val isSelected = !category.isSelected()
        val selectedResId = if (isSelected) selectedId else unselectedId
        source[position] = category.copy(selectedResId = selectedResId)
        return filter()
    }

    @CheckResult
    fun filter(): List<SampleItem> {
        val outcome = mutableListOf<SampleItem>()
        var isSelected = false
        source.forEach {
            when {
                it is Category -> {
                    isSelected = it.isSelected()
                    outcome.add(it)
                }
                it is Element && isSelected -> outcome.add(it)
            }
        }
        return outcome
    }

    @CheckResult
    fun categoryPayload(oldItem: Category, newItem: Category): Any? {
        return if (oldItem.isSelected() != newItem.isSelected()) "change" else null
    }

    private fun Category.isSelected(): Boolean {
        return selectedResId == selectedId
    }

    private fun ListAdapter() = listOf(
        Category(title = "ListAdapter", selectedResId = unselectedId),
        StartActivity(
            title = "ListAdapter",
            desc = "MutableStateList示例代码演示了如何使用ListAdapter",
            clazz = MutableStateListActivity::class
        )
    )

    private fun ViewTypeDelegate() = listOf(
        Category(title = "ViewTypeDelegate", selectedResId = unselectedId),
        StartActivity(
            title = "ViewTypeDelegate",
            desc = "基于ListAdapter实现的多类型方案",
            clazz = MultiTypeActivity::class
        )
    )

    private fun MutableStateList() = listOf(
        Category(title = "MutableStateList", selectedResId = unselectedId),
        StartActivity(
            title = "MutableStateList",
            desc = "Lifecycle处于非活跃状态时，仅修改列表数据",
            clazz = MutableStateListActivity::class
        )
    )

    private fun Extensions() = listOf(
        Category(title = "Extensions", selectedResId = unselectedId),
        StartActivity(
            title = "HeaderFooter",
            desc = "基于ConcatAdapter实现的HeaderFooter",
            clazz = HeaderFooterActivity::class
        ),
        StartActivity(
            title = "ItemClick",
            desc = "确保共享RecycledViewPool场景不会出现内存泄漏问题",
            clazz = ItemClickActivity::class
        ),
        StartActivity(
            title = "ItemTouch",
            desc = "基于ItemTouchHelper实现，提取出业务场景常用的函数",
            clazz = ItemTouchActivity::class
        ),
        StartActivity(
            title = "ItemSelect",
            desc = "单项选择和多项选择",
            clazz = ItemSelectActivity::class
        ),
        StartActivity(
            title = "Divider",
            desc = "基于ItemDecoration实现的通用分割线",
            clazz = DividerActivity::class
        )
    )

    private fun `cxrv-paging`() = listOf(
        Category(title = "cxrv-paging", selectedResId = unselectedId),
        StartActivity(
            title = "本地数据",
            desc = "不同LayoutManager的分页加载效果",
            clazz = PagingActivity::class
        ),
        StartActivity(
            title = "网络数据",
            desc = "WanAndroid文章列表的分页加载效果（使用预取策略）",
            clazz = ArticleListActivity::class
        )
    )

    private fun `cxrv-animatable`() = listOf(
        Category(title = "cxrv-animatable", selectedResId = unselectedId),
        StartActivity(
            title = "AnimatableMediator",
            desc = "控制全部Animatable的开始和停止",
            clazz = AnimatableMediatorActivity::class
        )
    )

    private fun `cxrv-viewpager2`() = listOf(
        Category(title = "cxrv-viewpager2", selectedResId = unselectedId),
        StartActivity(
            title = "Vp2NestedScrollable",
            desc = "ViewPager2滚动冲突处理方案",
            clazz = NestedScrollableActivity::class
        ),
        StartActivity(
            title = "LoopPagerController",
            desc = "ViewPager2的循环页面控制器",
            clazz = LoopPagerActivity::class
        )
    )

    private fun Scene() = listOf(
        Category(title = "Scene", selectedResId = unselectedId),
        StartActivity(
            title = "Payload",
            desc = "Payload对象的二进制方案",
            clazz = PayloadActivity::class
        ),
        StartActivity(
            title = "ViewPager2共享池",
            desc = "使用共享池优化Fragment重建",
            clazz = SharedPoolActivity::class
        ),
        StartActivity(
            title = "Fragment过渡动画卡顿优化",
            desc = "Fragment过渡动画卡顿的原因及优化方案",
            clazz = EnterTransitionActivity::class
        ),
        StartActivity(
            title = "视频流的过渡动画和分页加载",
            desc = "使用简易的过渡动画框架，共享分页数据流",
            clazz = ComplexListActivity::class
        )
    )
}

sealed class SampleItem {
    data class Category(val title: String, val selectedResId: Int) : SampleItem()
    sealed class Element(open val title: String, open val desc: String) : SampleItem() {
        abstract fun perform(activity: FragmentActivity)
    }
}

private data class StartActivity(
    override val title: String,
    override val desc: String,
    val clazz: KClass<out Activity>
) : Element(title, desc) {
    override fun perform(activity: FragmentActivity) {
        activity.startActivity(Intent(activity, clazz.java))
    }
}