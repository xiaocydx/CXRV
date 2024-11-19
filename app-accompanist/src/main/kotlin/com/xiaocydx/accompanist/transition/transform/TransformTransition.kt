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

import android.animation.Animator
import android.animation.FloatEvaluator
import android.animation.IntEvaluator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.graphics.withMatrix
import androidx.core.graphics.withSave
import androidx.fragment.app.Fragment
import androidx.transition.ChangeImageTransform
import androidx.transition.Fade
import androidx.transition.MatrixEvaluator
import androidx.transition.PathMotion
import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter
import androidx.transition.TransitionSet
import androidx.transition.TransitionValues
import com.google.android.material.transition.MaterialContainerTransform
import com.xiaocydx.accompanist.view.RoundRectOutlineProvider

class FadeTransform(receiver: Fragment) : Fade() {
    init {
        addTarget(receiver.requireView())
    }
}

class ImageTransform(
    private val isEnter: Boolean,
    private val senderRoot: () -> View?,
    private val senderImage: () -> ImageView?,
    private val receiverRoot: () -> View?,
    private val receiverImage: () -> ImageView?,
    private val decoration: Decoration? = null
) : ChangeImageTransform() {
    private val transitionProperties: Array<String>
    private val contentParentId = android.R.id.content

    init {
        val superProperties = super.getTransitionProperties()
        val properties = arrayOf(PROPNAME_BOUNDS, PROPNAME_CORNERS, PROPNAME_ROOT)
        transitionProperties = superProperties + properties
        addTarget(contentParentId)
    }

    override fun getTransitionProperties(): Array<String> {
        return transitionProperties
    }

    override fun captureStartValues(values: TransitionValues) {
        val startRoot = if (isEnter) senderRoot() else receiverRoot()
        val startImage = if (isEnter) senderImage() else receiverImage()
        values.view = startImage ?: values.view
        values.ensureVisible { super.captureStartValues(values) }
        if (startRoot != null && startImage != null) {
            captureValues(startRoot, values)
        }
    }

    override fun captureEndValues(values: TransitionValues) {
        val endRoot = if (isEnter) receiverRoot() else senderRoot()
        val endImage = if (isEnter) receiverImage() else senderImage()
        values.view = endImage ?: values.view
        values.ensureVisible { super.captureEndValues(values) }
        if (endRoot != null && endImage != null) {
            captureValues(endRoot, values)
        }
    }

    private inline fun TransitionValues.ensureVisible(action: () -> Unit) {
        if (view == null) return action()
        // 确保父类逻辑能捕获view的矩阵。例如视频播放场景，捕获已经隐藏的封面。
        val visibility = view.visibility
        val changed = visibility != View.VISIBLE
        if (changed) view.visibility = View.VISIBLE
        action()
        if (changed) view.visibility = visibility
    }

    private fun captureValues(root: View, values: TransitionValues) {
        val view = values.view ?: return
        val contentParent = findContentParent() ?: return
        if (!view.isAttachedToWindow) return
        val point = IntArray(2)
        view.getLocationInWindow(point)
        val bounds = Rect()
        bounds.left = point[0]
        bounds.top = point[1]
        bounds.right = point[0] + view.width
        bounds.bottom = point[1] + view.height

        contentParent.getLocationInWindow(point)
        bounds.offset(0, -point[1])
        values.values[PROPNAME_BOUNDS] = bounds

        var corners = view.getCorners()
        if (corners == null
                && root.width - view.width <= 1
                && root.height - view.height <= 1) {
            corners = root.getCorners()
        }
        values.values[PROPNAME_CORNERS] = corners ?: 0f
        values.values[PROPNAME_ROOT] = root
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) {
            return null
        }

        val startMatrix = startValues.values[PROPNAME_MATRIX] as? Matrix
        val startBounds = startValues.values[PROPNAME_BOUNDS] as? Rect
        val startCorners = startValues.values[PROPNAME_CORNERS] as? Float ?: 0f
        val startRoot = startValues.values[PROPNAME_ROOT] as? View
        val startImage = startValues.view as? ImageView

        val endMatrix = endValues.values[PROPNAME_MATRIX] as? Matrix
        val endBounds = endValues.values[PROPNAME_BOUNDS] as? Rect
        val endCorners = endValues.values[PROPNAME_CORNERS] as? Float ?: 0f
        val endRoot = endValues.values[PROPNAME_ROOT] as? View
        val endImage = endValues.view as? ImageView

        val contentParent = findContentParent()
        if (startMatrix == null || endMatrix == null
                || startBounds == null || endBounds == null
                || startImage == null || endImage == null
                || contentParent == null) {
            return null
        }

        val targetImage = if (isEnter) endImage else startImage
        val otherImage = if (isEnter) startImage else endImage
        val otherMatrix = if (isEnter) startMatrix else endMatrix
        targetImage.centerCropMatrix(otherMatrix, otherImage.width, otherImage.height)
        (otherImage as? TransformImageView)?.postImageTranslate(otherMatrix)

        val drawable = TransitionDrawable(
            pathMotion, isEnter, targetImage,
            startMatrix, startBounds, startCorners,
            endMatrix, endBounds, endCorners, decoration
        )
        drawable.setBounds(0, 0, contentParent.width, contentParent.height)

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.addUpdateListener { drawable.fraction = it.animatedFraction }
        addListener(object : TransitionListenerAdapter() {
            override fun onTransitionStart(transition: Transition) {
                contentParent.overlay.add(drawable)
                setVisible(startRoot, startImage, isVisible = false)
                setVisible(endRoot, endImage, isVisible = false)
            }

            override fun onTransitionEnd(transition: Transition) {
                removeListener(this)
                setVisible(startRoot, startImage, isVisible = true)
                setVisible(endRoot, endImage, isVisible = true)
                contentParent.overlay.remove(drawable)
            }
        })
        return animator
    }

    private fun findContentParent(): View? {
        return findContentParent(senderRoot()) ?: findContentParent(receiverRoot())
    }

    private fun findContentParent(view: View?): View? {
        var parent = view?.parent as? ViewGroup
        while (parent != null && parent.id != contentParentId) {
            parent = parent.parent as? ViewGroup
        }
        return parent?.takeIf { it.id == contentParentId }
    }

    private fun setVisible(root: View?, image: View?, isVisible: Boolean) {
        if (isVisible) {
            root?.alpha = 1f
            image?.alpha = 1f
            return
        }
        if (root != null && image != null
                && root.width - image.width <= 1
                && root.height - image.height <= 1) {
            root.alpha = 0f
        } else {
            image?.alpha = 0f
        }
    }

    private fun View.getCorners(): Float? {
        val provider = outlineProvider
        if (provider is RoundRectOutlineProvider) {
            return provider.corners
        }
        return null
    }

    interface Decoration {
        fun onDraw(isEnter: Boolean, bounds: RectF, fraction: Float, canvas: Canvas) = Unit
        fun onDrawOver(isEnter: Boolean, bounds: RectF, fraction: Float, canvas: Canvas) = Unit
    }

    private companion object {
        const val PROPNAME_MATRIX = "android:changeImageTransform:matrix"
        const val PROPNAME_BOUNDS = "ImageTransform:bounds"
        const val PROPNAME_CORNERS = "ImageTransform:corners"
        const val PROPNAME_ROOT = "ImageTransform:root"
    }
}

private class TransitionDrawable(
    pathMotion: PathMotion,
    private val isEnter: Boolean,
    private val targetView: ImageView,
    private val startMatrix: Matrix,
    private val startBounds: Rect,
    private val startCorners: Float,
    private val endMatrix: Matrix,
    private val endBounds: Rect,
    private val endCorners: Float,
    private val decoration: ImageTransform.Decoration?
) : Drawable() {
    private val clipPath = Path()
    private val decorBounds = RectF()
    private val sizeEvaluator = IntEvaluator()
    private val cornersEvaluator = FloatEvaluator()
    private val matrixEvaluator = MatrixEvaluator()
    private val motionPathMeasure: PathMeasure
    private val motionPathLength: Float
    private val motionPathPoint = FloatArray(2)

    init {
        val motionPath = pathMotion.getPath(
            startBounds.left.toFloat(), startBounds.top.toFloat(),
            endBounds.left.toFloat(), endBounds.top.toFloat()
        )
        motionPathMeasure = PathMeasure(motionPath, false)
        motionPathLength = motionPathMeasure.length
    }

    var fraction = 0f
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val corners = cornersEvaluator.evaluate(fraction, startCorners, endCorners)
        val width = sizeEvaluator.evaluate(fraction, startBounds.width(), endBounds.width())
        val height = sizeEvaluator.evaluate(fraction, startBounds.height(), endBounds.height())
        val matrix = matrixEvaluator.evaluate(fraction, startMatrix, endMatrix)

        clipPath.reset()
        clipPath.addRoundRect(
            0f, 0f,
            width.toFloat(), height.toFloat(),
            corners, corners, Path.Direction.CW
        )
        motionPathMeasure.getPosTan(motionPathLength * fraction, motionPathPoint, null)

        canvas.withSave {
            canvas.translate(motionPathPoint[0], motionPathPoint[1])
            canvas.clipPath(clipPath)
            decorBounds.set(0f, 0f, width.toFloat(), height.toFloat())
            decoration?.onDraw(isEnter, decorBounds, fraction, canvas)
            canvas.withMatrix(matrix) { targetView.drawable?.draw(canvas) }
            decoration?.onDrawOver(isEnter, decorBounds, fraction, canvas)
        }
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit
    override fun getOpacity() = PixelFormat.TRANSLUCENT
}

/**
 * [MaterialContainerTransform]不能被继承，利用[TransitionSet]包装一层，
 * 重写[TransitionSet]相关函数，以实现对捕获时机的监听，当真正开始捕获时，
 * 才获取`target`进行捕获，[TransitionSet]会触发`childTransition`的回调，
 * 这也让[MaterialContainerTransform]在动画开始和结束时，能正常完成工作。
 */
internal class TransformTransition(
    private val transform: MaterialContainerTransform,
    private val targetView: (start: Boolean) -> View?
) : TransitionSet() {

    init {
        addTarget(transform.drawingViewId)
        addTransition(transform)
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
        if (view == null || view.id != transform.drawingViewId) return

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