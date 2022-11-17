@file:Suppress("SpellCheckingInspection")

package com.xiaocydx.sample.viewpager2.animatable

import android.graphics.drawable.Animatable
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.Disposable

/**
 * 设置控制[Animatable]的中间者，负责[AnimatableProvider]和[AnimatableController]之间的调度
 *
 * ### 添加常用的[AnimatableProvider]
 * 1. [registerImageView]
 * 2. [registerProvider]
 *
 * ### 添加常用的[AnimatableController]
 * 1. [controlledByScroll]
 * 2. [controlledByParentViewPager2]
 *
 * ```
 * recyclerView.setAnimatableMediator {
 *     controlledByScroll()
 *     controlledByParentViewPager2(viewPager2)
 *     registerImageView(adapter) { imageView }
 * }
 * ```
 */
inline fun RecyclerView.setAnimatableMediator(initialize: AnimatableMediator.() -> Unit): Disposable {
    return AnimatableMediatorImpl(this).apply(initialize).attach()
}

/**
 * 控制[Animatable]的中间者
 *
 * 在视图树draw之前，实现类会判断[isAllowStart]，若[isAllowStart] = `false`，则调用[stopAll]。
 */
interface AnimatableMediator : Disposable {

    /**
     * 关联的[RecyclerView]
     */
    val recyclerView: RecyclerView

    /**
     * 是否允许执行[start]和[startAll]
     *
     * 该属性的结果，由全部的[AnimatableController.isAllowStart]决定
     */
    val isAllowStart: Boolean

    /**
     * 通过[child]调用[Animatable.start]
     *
     * 当[isAllowStart]为`true`时，该函数才会真正执行。
     */
    fun start(child: View)

    /**
     * 通过[child]调用[Animatable.stop]
     */
    fun stop(child: View)

    /**
     * 通过遍历[recyclerView]的全部子View，调用[Animatable.start]
     *
     * 当[isAllowStart]为`true`时，该函数才会真正执行。
     */
    fun startAll()

    /**
     * 通过遍历[recyclerView]的全部子View，调用[Animatable.stop]
     */
    fun stopAll()

    /**
     * 添加[AnimatableProvider]
     *
     * [start]、[stop]、[startAll]、[stopAll]被调用时，
     * 会调用[AnimatableProvider.getAnimatable]获取[Animatable]。
     */
    fun addProvider(provider: AnimatableProvider)

    /**
     * 移除[AnimatableProvider]
     */
    fun removeProvider(provider: AnimatableProvider)

    /**
     * 添加[AnimatableController]
     *
     * 添加的[controller]根据具体实现，调用[start]、[stop]、[startAll]、[stopAll]。
     */
    fun addController(controller: AnimatableController)

    /**
     * 移除[AnimatableController]
     */
    fun removeController(controller: AnimatableController)

    /**
     * 查找父类或者自身等于[clazz]的[AnimatableController]
     */
    fun <T : AnimatableController> findController(clazz: Class<T>): T?
}

/**
 * [Animatable]的提供者
 *
 * 常见的动图加载场景，其实现类提供的[Animatable]就是[ImageView.getDrawable]，
 * 可以让[ViewHolder]实现[Animatable]，统一控制[ViewHolder]内部的多个[Animatable]。
 */
interface AnimatableProvider : Disposable {

    /**
     * 获取[Animatable]
     *
     * [AnimatableMediator]执行[Animatable.start]和[Animatable.stop]。
     */
    fun getAnimatable(holder: ViewHolder): Animatable?
}

/**
 * [Animatable]的控制器
 *
 * 常见的控制场景有：
 * 1. 滚动容器滚动时控制[Animatable]，例如RecyclerView。
 * 2. 父级滚动容器滚动时控制[Animatable]，例如ViewPager2嵌套RecyclerView。
 * 3. Lifecycle状态更改时控制[Animatable]，例如Fragment的`viewLifecycle`。
 */
interface AnimatableController : Disposable {

    /**
     * 是否允许执行[Animatable.start]
     *
     * 1. [AnimatableMediator.start]和[AnimatableMediator.startAll]被调用时，
     * 会判断该属性，若[isAllowStart] = `true`，则上述两个函数才会真正执行。
     * 2. [AnimatableMediator]会在视图树draw之前，判断该属性，
     * 若[isAllowStart] = `false`，则调用[AnimatableMediator.stopAll]。
     * 这个逻辑可以简化很多场景的实现，例如RecyclerView滚动时、动图加载完成时，
     * 会申请下一帧重绘，因此在下一帧视图树draw之前，判断是否调用[AnimatableMediator.stopAll]。
     */
    val isAllowStart: Boolean
}