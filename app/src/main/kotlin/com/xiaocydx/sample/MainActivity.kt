package com.xiaocydx.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

/**
 * @author xcc
 * @date 2022/2/17
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sample = Sample(source(), this)
        setContentView(sample.contentView())
    }

    private fun source() = listOf(
        "ListAdapter".elements(
            "Basic" desc "MutableStateList示例代码演示了如何使用" start MutableStateListActivity::class
        ),

        "ViewTypeDelegate".elements(
            "MultiType" desc "基于ListAdapter实现的多类型方案" start MultiTypeActivity::class
        ),

        "MutableStateList".elements(
            "Basic" desc "Lifecycle处于非活跃状态时，仅修改列表数据" start MutableStateListActivity::class
        ),

        "Extensions".elements(
            "HeaderFooter" desc "基于ConcatAdapter实现的HeaderFooter" start HeaderFooterActivity::class,
            "ItemClick" desc "确保共享RecycledViewPool场景不会出现内存泄漏问题" start ItemClickActivity::class,
            "ItemTouch" desc "基于ItemTouchHelper实现，提取出业务场景常用的函数" start ItemTouchActivity::class,
            "ItemSelect" desc "单项选择和多项选择" start ItemSelectActivity::class,
            "Divider" desc "基于ItemDecoration实现的通用分割线" start DividerActivity::class
        ),

        "cxrv-paging".elements(
            "本地数据" desc "不同LayoutManager的分页加载效果" start PagingActivity::class,
            "网络数据" desc "WanAndroid文章列表的分页加载效果（使用预取策略）" start ArticleListActivity::class
        ),

        "cxrv-animatable".elements(
            "AnimatableMediator" desc "控制全部Animatable的开始和停止" start AnimatableMediatorActivity::class
        ),

        "cxrv-viewpager2".elements(
            "Vp2NestedScrollable" desc "ViewPager2滚动冲突处理方案" start NestedScrollableActivity::class,
            "LoopPagerController" desc "ViewPager2的循环页面控制器" start LoopPagerActivity::class
        ),

        "Scene".elements(
            "Payload" desc "Payload对象的二进制方案" start PayloadActivity::class,
            "ViewPager2共享池" desc "使用共享池优化Fragment重建" start SharedPoolActivity::class,
            "Fragment过渡动画卡顿优化" desc "Fragment过渡动画卡顿的原因及优化方案" start EnterTransitionActivity::class,
            "视频流的过渡动画和分页加载" desc "使用简易的过渡动画框架，共享分页数据流" start ComplexListActivity::class,
        )
    )
}