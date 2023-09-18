package com.xiaocydx.sample.transition.compat

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransitionCompat21Enhance
import androidx.fragment.app.FragmentTransitionSupportEnhance
import android.transition.Transition as AndroidTransition
import androidx.transition.Transition as AndroidXTransition

/**
 * Fragment过渡动画的调度过程，会将`Fragment.enterTransition`合并到`TransitionSet`，
 * `TransitionSet`不会同步`Fragment.enterTransition`在初始化阶段主动添加的`target`，
 * 因此`TransitionSet`仍然是遍历Fragment容器视图树，捕获每一个视图的状态。
 *
 * 随着Fragment容器中的视图越来越多，例如不断添加Fragment，但不移除之前的Fragment，
 * `TransitionSet`遍历Fragment容器视图树的流程也会越来越长，并且会创建大量临时集合。
 *
 * [FragmentTransitionCompat21Enhance]和[FragmentTransitionSupportEnhance]优化了捕获流程，
 * 对`TransitionSet`同步[transition]的`targets`和`targetIds`，避免遍历Fragment容器视图树。
 *
 * [FragmentTransitionSupportEnhance]修复了[AndroidXTransition]和Fragment生命周期状态的关联，
 * 确保在Fragment过渡动画结束时，才转换Fragment的生命周期状态。
 */
fun Fragment.setEnterTransitionCompat(transition: Any?) {
    enterTransition = when (transition) {
        is AndroidTransition -> FragmentTransitionCompat21Enhance.merge(transition)
        is AndroidXTransition -> FragmentTransitionSupportEnhance.merge(transition)
        else -> transition
    }
}