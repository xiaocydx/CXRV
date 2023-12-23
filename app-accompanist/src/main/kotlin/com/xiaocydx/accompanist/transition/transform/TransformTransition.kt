/*
 * Copyright 2022 xiaocydx
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaocydx.accompanist.transition.transform

import android.view.View
import androidx.transition.Transition
import androidx.transition.TransitionSet
import androidx.transition.TransitionValues
import com.google.android.material.transition.MaterialContainerTransform

/**
 * [MaterialContainerTransform]不能被继承，利用[TransitionSet]包装一层，
 * 重写[TransitionSet]相关函数，以实现对捕获时机的监听，当真正开始捕获时，
 * 才获取`target`进行捕获，[TransitionSet]会触发`childTransition`的回调，
 * 这也让[MaterialContainerTransform]在动画开始和结束时，能正常完成工作。
 *
 * @author xcc
 * @date 2023/8/5
 */
internal class TransformTransition(
    private val sceneRootId: Int,
    private val transform: MaterialContainerTransform,
    private val targetView: (start: Boolean) -> View?
) : TransitionSet() {

    init {
        addTarget(sceneRootId)
        addTransition(transform)
        transform.drawingViewId = sceneRootId
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues, start = true)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues, start = false)
    }

    private fun captureValues(transitionValues: TransitionValues, start: Boolean) {
        // 捕获流程调用自Transition.captureValues()，或者Transition.captureHierarchy()，
        // 对于这两种情况，将sceneRoot的起始和结束捕获委托给transform，确保能创建属性动画。
        val view = transitionValues.view
        if (view == null || view.id != sceneRootId) return

        // 当transform.createAnimator()创建属性动画时，会向上递归查找drawingView，
        // 若查找不到，则抛出异常，因此在创建属性动画之前，先判断target能否进行查找，
        // return表示不捕获，startValues或endValues会缺一个，也就不会创建属性动画。
        val target = targetView(start)
        if (target == null || !canFindDrawingViewById(target)) return

        // 当前Transition和transform可能被添加了target，先移除再添加，确保元素不重复
        this.addTargetSafely(target)
        transform.addTargetSafely(target)

        // 将transitionValues.view替换为target有两个目的：
        // 1. 确保调用捕获函数能通过当前Transition和transform的Transition.isValidTarget()检查。
        // 2. 确保transform.captureStartValues()和transform.captureEndValues()能捕获target。
        transitionValues.view = target
        if (start) {
            super.captureStartValues(transitionValues)
        } else {
            super.captureEndValues(transitionValues)
        }
        this.removeTarget(target)
        transform.removeTarget(target)
    }

    private fun Transition.addTargetSafely(target: View) {
        removeTarget(target)
        addTarget(target)
    }

    private fun canFindDrawingViewById(target: View): Boolean {
        val drawingViewId = transform.drawingViewId
        var view: View? = target
        while (view != null) {
            if (view.id == drawingViewId) return true
            val parent = view.parent
            view = if (parent is View) parent else break
        }
        return false
    }
}