@file:Suppress("FunctionName", "SpellCheckingInspection")

package com.xiaocydx.sample

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.CheckResult
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
        source[position] = category.copy(resId = selectedResId)
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

    private fun Category.isSelected() = resId == selectedId

    private fun ListAdapter() = listOf(
        "ListAdapter" and unselectedId,
        "ListAdapter" and "MutableStateList示例代码演示了如何使用" to MutableStateListActivity::class
    )

    private fun ViewTypeDelegate() = listOf(
        "ViewTypeDelegate" and unselectedId,
        "ViewTypeDelegate" and "基于ListAdapter实现的多类型方案" to MultiTypeActivity::class
    )

    private fun MutableStateList() = listOf(
        "MutableStateList" and unselectedId,
        "MutableStateList" and "Lifecycle处于非活跃状态时，仅修改列表数据" to MutableStateListActivity::class
    )

    private fun Extensions() = listOf(
        "Extensions" and unselectedId,
        "HeaderFooter" and "基于ConcatAdapter实现的HeaderFooter" to HeaderFooterActivity::class,
        "ItemClick" and "确保共享RecycledViewPool场景不会出现内存泄漏问题" to ItemClickActivity::class,
        "ItemTouch" and "基于ItemTouchHelper实现，提取出业务场景常用的函数" to ItemTouchActivity::class,
        "ItemSelect" and "单项选择和多项选择" to ItemSelectActivity::class,
        "Divider" and "基于ItemDecoration实现的通用分割线" to DividerActivity::class
    )

    private fun `cxrv-paging`() = listOf(
        "cxrv-paging" and unselectedId,
        "本地数据" and "不同LayoutManager的分页加载效果" to PagingActivity::class,
        "网络数据" and "WanAndroid文章列表的分页加载效果（使用预取策略）" to ArticleListActivity::class
    )

    private fun `cxrv-animatable`() = listOf(
        "cxrv-animatable" and unselectedId,
        "AnimatableMediator" and "控制全部Animatable的开始和停止" to AnimatableMediatorActivity::class
    )

    private fun `cxrv-viewpager2`() = listOf(
        "cxrv-viewpager2" and unselectedId,
        "Vp2NestedScrollable" and "ViewPager2滚动冲突处理方案" to NestedScrollableActivity::class,
        "LoopPagerController" and "ViewPager2的循环页面控制器" to LoopPagerActivity::class
    )

    private fun Scene() = listOf(
        "Scene" and unselectedId,
        "Payload" and "Payload对象的二进制方案" to PayloadActivity::class,
        "ViewPager2共享池" and "使用共享池优化Fragment重建" to SharedPoolActivity::class,
        "Fragment过渡动画卡顿优化" and "Fragment过渡动画卡顿的原因及优化方案" to EnterTransitionActivity::class,
        "视频流的过渡动画和分页加载" and "使用简易的过渡动画框架，共享分页数据流" to ComplexListActivity::class,
    )
}

sealed class SampleItem {
    data class Category(val title: String, val resId: Int) : SampleItem()

    data class Element(
        val title: String,
        val desc: String,
        private val clazz: KClass<out Activity>? = null
    ) : SampleItem() {
        fun perform(context: Context) {
            clazz ?: return
            context.startActivity(Intent(context, clazz.java))
        }
    }
}

private infix fun String.and(resId: Int) = Category(title = this, resId = resId)

private infix fun String.and(desc: String) = Element(title = this, desc = desc)

private infix fun Element.to(clazz: KClass<out Activity>) = copy(clazz = clazz)