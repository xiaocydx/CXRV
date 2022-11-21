@file:Suppress("SpellCheckingInspection")

package com.xiaocydx.sample.viewpager2.animatable

import android.graphics.drawable.Animatable
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.xiaocydx.cxrv.list.Disposable

/**
 * 设置控制[Animatable]的中间者，负责[AnimatableProvider]和[AnimatableController]之间的调度
 *
 * ### 添加常用的[AnimatableProvider]
 * 1. [registerImageView]
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
 * 在视图树draw之前，实现类会判断[canStartAnimatable]，
 * 若[canStartAnimatable] = `false`，则调用[stopAllAnimatable]。
 */
interface AnimatableMediator : Disposable {

    /**
     * 关联的[RecyclerView]
     */
    val recyclerView: RecyclerView

    /**
     * 是否允许执行[startAllAnimatable]
     *
     * 该属性的结果，由全部的[AnimatableController.canStartAnimatable]决定。
     */
    val canStartAnimatable: Boolean

    /**
     * 通过遍历[recyclerView]的全部子View，调用[Animatable.start]
     *
     * 当[canStartAnimatable]为`true`时，该函数才会真正执行。
     */
    fun startAllAnimatable()

    /**
     * 通过遍历[recyclerView]的全部子View，调用[Animatable.stop]
     */
    fun stopAllAnimatable()

    /**
     * 添加[AnimatableProvider]
     *
     * [startAllAnimatable]、[stopAllAnimatable]被调用时，
     * 会调用[AnimatableProvider.getAnimatableOrNull]获取[Animatable]。
     */
    fun addAnimatableProvider(provider: AnimatableProvider)

    /**
     * 移除[AnimatableProvider]
     */
    fun removeAnimatableProvider(provider: AnimatableProvider)

    /**
     * 添加[AnimatableController]
     *
     * 添加的[controller]根据具体实现，调用[startAllAnimatable]、[stopAllAnimatable]。
     */
    fun addAnimatableController(controller: AnimatableController)

    /**
     * 移除[AnimatableController]
     */
    fun removeAnimatableController(controller: AnimatableController)

    /**
     * 查找父类或者自身等于[clazz]的[AnimatableController]
     */
    fun <T : AnimatableController> findAnimatableController(clazz: Class<T>): T?
}

/**
 * 查找父类或者自身等于`T::class.java`的[AnimatableController]
 */
inline fun <reified T : AnimatableController> AnimatableMediator.findAnimatableController(): T? {
    return findAnimatableController(T::class.java)
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
    fun getAnimatableOrNull(holder: ViewHolder): Animatable?

    /**
     * 是否可以执行[Animatable.start]
     *
     * @param animatable [getAnimatableOrNull]返回的非空结果
     */
    fun canStartAnimatable(holder: ViewHolder, animatable: Animatable): Boolean
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
     * 是否可以执行[Animatable.start]
     *
     * 1. [AnimatableMediator.startAllAnimatable]被调用时会判断该属性，
     * 若[canStartAnimatable] = `true`，则函数才会被真正执行。
     *
     * 2. [AnimatableMediator]会在视图树draw之前判断该属性，
     * 若[canStartAnimatable] = `false`，则调用[AnimatableMediator.stopAllAnimatable]。
     * 这个逻辑可以简化很多场景的实现，例如RecyclerView滚动时、动图加载完成时，
     * 会申请下一帧重绘，因此在下一帧视图树draw之前判断该属性。
     */
    val canStartAnimatable: Boolean
}