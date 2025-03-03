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

package com.xiaocydx.accompanist.transition.render

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.animation.TimeInterpolator
import android.annotation.SuppressLint
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.view.View
import androidx.annotation.RequiresApi

/**
 * 运行在RenderThread的属性动画
 *
 * **注意**：
 * [AnimatorRT.create]传入的`view`作为RenderNodeAnimator的RenderNode，
 * 不支持`view`内部或者其child内部运行[AnimatedVectorDrawable]，比如:
 * ```
 * val view = FrameLayout(context)
 * val progressBar = ProgressBar(context)
 * view.add(progressBar)
 *
 * // 在animatorRT运行后，progressBar的AnimatedVectorDrawable动画（loading转圈）会暂停，
 * // AnimatedVectorDrawable内部使用了VectorDrawableAnimatorRT，也是运行在RenderThread。
 * // 目前不清楚出现冲突的原因。
 * val animatorRT = AnimatorRT.create(view, Property.TRANSLATION_X, 100f)
 * animatorRT.start()
 * ```
 *
 * @author xcc
 * @date 2024/12/29
 */
class AnimatorRT private constructor(private val renderNodeAnimator: Animator) {

    private fun setTarget(view: View) = apply {
        setTarget.invoke(renderNodeAnimator, view)
    }

    fun set(block: Animator.() -> Unit) {
        renderNodeAnimator.block()
    }

    fun start() {
        renderNodeAnimator.start()
    }

    /**
     * 将RenderNodeAnimator转换为安全的[Animator]
     *
     * RenderNodeAnimator实现的`pause()`和`resume()`会抛出[UnsupportedOperationException]，
     * 转换的[Animator]去除抛出[UnsupportedOperationException]，调用这两个函数不做任何处理。
     */
    fun toSafeAnimator(): Animator {
        return SafeAnimator(renderNodeAnimator)
    }

    private class SafeAnimator(private val renderNodeAnimator: Animator) : Animator() {
        private var listeners: MutableMap<AnimatorListener, SafeAnimatorListener>? = null

        private fun ensureListeners() = run {
            if (listeners == null) listeners = mutableMapOf()
            listeners!!
        }

        override fun start() {
            renderNodeAnimator.start()
        }

        override fun cancel() {
            renderNodeAnimator.cancel()
        }

        override fun end() {
            renderNodeAnimator.end()
        }

        override fun pause() {
            // renderNodeAnimator.pause() // throw UnsupportedOperationException
        }

        override fun resume() {
            // renderNodeAnimator.resume() // throw UnsupportedOperationException
        }

        override fun getStartDelay(): Long {
            return renderNodeAnimator.startDelay
        }

        override fun setStartDelay(startDelay: Long) {
            renderNodeAnimator.startDelay = startDelay
        }

        override fun setDuration(duration: Long): Animator {
            renderNodeAnimator.duration = duration
            return this
        }

        override fun getDuration(): Long {
            return renderNodeAnimator.duration
        }

        @RequiresApi(Build.VERSION_CODES.N)
        override fun getTotalDuration(): Long {
            return renderNodeAnimator.totalDuration
        }

        override fun setInterpolator(value: TimeInterpolator?) {
            renderNodeAnimator.interpolator = value
        }

        override fun getInterpolator(): TimeInterpolator {
            return renderNodeAnimator.interpolator
        }

        override fun isRunning(): Boolean {
            return renderNodeAnimator.isRunning
        }

        override fun isStarted(): Boolean {
            return renderNodeAnimator.isStarted
        }

        override fun addListener(listener: AnimatorListener) {
            if (ensureListeners().containsKey(listener)) return
            val safeListener = SafeAnimatorListener(this, listener)
            ensureListeners()[listener] = safeListener
            renderNodeAnimator.addListener(safeListener)
        }

        override fun removeListener(listener: AnimatorListener) {
            val safeListener = ensureListeners().remove(listener) ?: return
            renderNodeAnimator.removeListener(safeListener)
        }

        override fun removeAllListeners() {
            listeners?.clear()
            listeners = null
            renderNodeAnimator.removeAllListeners()
        }

        override fun getListeners(): ArrayList<AnimatorListener> {
            // 无法确定返回可变集合的意图
            throw UnsupportedOperationException()
        }

        override fun clone(): Animator {
            return renderNodeAnimator.clone() // throw IllegalStateException("Cannot clone this animator")
        }
    }

    /**
     * 包装[AnimatorListener]，避免实现类获取[onAnimationStart]等函数传入的RenderNodeAnimator，
     * 调用RenderNodeAnimator实现的`pause()`和`resume()`抛出[UnsupportedOperationException]。
     */
    private class SafeAnimatorListener(
        private val animator: SafeAnimator,
        private val listener: AnimatorListener
    ) : AnimatorListener {

        override fun onAnimationStart(renderNodeAnimator: Animator) {
            listener.onAnimationStart(animator)
        }

        override fun onAnimationEnd(renderNodeAnimator: Animator) {
            listener.onAnimationEnd(animator)
        }

        override fun onAnimationCancel(renderNodeAnimator: Animator) {
            listener.onAnimationCancel(animator)
        }

        override fun onAnimationRepeat(renderNodeAnimator: Animator) {
            listener.onAnimationRepeat(animator)
        }
    }

    /**
     * `android.graphics.animation.RenderNodeAnimator`的同名常量
     */
    enum class Property(internal val value: Int) {
        TRANSLATION_X(value = 0),
        TRANSLATION_Y(value = 1),
        ALPHA(value = 11),
        // TRANSLATION_Z(value = 2),
        // SCALE_X(value = 3),
        // SCALE_Y(value = 4),
        // ROTATION(value = 5),
        // ROTATION_X(value = 6),
        // ROTATION_Y(value = 7),
        // X(value = 8),
        // Y(value = 9),
        // Z(value = 10),
    }

    companion object : Reflection {
        @SuppressLint("PrivateApi")
        private val clazz = Class.forName("android.view.RenderNodeAnimator")

        /**
         * ```
         * package android.view;
         *
         * public class RenderNodeAnimator extends android.graphics.animation.RenderNodeAnimator {
         *     public RenderNodeAnimator(int property, float finalValue) {...}
         * }
         * ```
         */
        private val property_finalValue = clazz.declaredConstructor(
            Int::class.javaPrimitiveType!!, Float::class.javaPrimitiveType!!
        ).toCache()

        /**
         * ```
         * package android.view;
         *
         * public class RenderNodeAnimator extends android.graphics.animation.RenderNodeAnimator {
         *     public void setTarget(View view) {...}
         * }
         * ```
         */
        private val setTarget = clazz.declaredMethod(name = "setTarget", View::class.java).toCache()

        fun create(view: View, property: Property, finalValue: Float): AnimatorRT {
            val animator = property_finalValue.newInstance(property.value, finalValue)
            return AnimatorRT(animator as Animator).setTarget(view)
        }
    }
}