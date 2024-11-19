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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

/**
 * 基于[ImageTransformer]做平移的ImageView，[ImageTransformer]默认为[fitCenter]
 *
 * @author xcc
 * @date 2024/11/19
 */
class TransformImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {
    private val matrix = Matrix()
    private val imageRect = RectF()
    private val imageTranslate = Translate()
    private val expectTranslate = Translate()
    private var translateAction: ((Float, Float) -> Unit)? = null
    private var transformer = ImageTransformer.fitCenter()
    private var isUpdating = false

    init {
        scaleType = ScaleType.MATRIX
    }

    /**
     * 设置期望的x平移值，最终的x平移值不会使Image脱离ImageView边界
     */
    fun setImageExpectTranslateX(value: Float) {
        if (expectTranslate.x == value) return
        expectTranslate.x = value
        requestUpdateMatrix()
    }

    /**
     * 设置期望的y平移值，最终的y平移值不会使Image脱离ImageView边界
     */
    fun setImageExpectTranslateY(value: Float) {
        if (expectTranslate.y == value) return
        expectTranslate.y = value
        requestUpdateMatrix()
    }

    /**
     * 当[updateMatrix]准备好`imageWidth`和`imageHeight`时，会调用[action]，
     * [action]中可调用[setImageExpectTranslateX]和[setImageExpectTranslateY]。
     */
    fun setImageTranslateAction(action: ((imageWidth: Float, imageHeight: Float) -> Unit)?) {
        translateAction = action
        requestUpdateMatrix()
    }

    /**
     * 对[matrix]左乘最终的x、y平移值
     */
    fun postImageTranslate(matrix: Matrix) {
        matrix.postTranslate(imageTranslate.x, imageTranslate.y)
    }

    override fun setScaleType(scaleType: ScaleType?) {
        super.setScaleType(ScaleType.MATRIX)
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        requestUpdateMatrix()
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        requestUpdateMatrix()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        requestUpdateMatrix()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        requestUpdateMatrix()
    }

    override fun setFrame(l: Int, t: Int, r: Int, b: Int): Boolean {
        val changed = super.setFrame(l, t, r, b)
        if (changed) requestUpdateMatrix()
        return changed
    }

    override fun onDraw(canvas: Canvas) {
        assert(scaleType == ScaleType.MATRIX)
        if (matrix.isIdentity) updateMatrix()
        super.onDraw(canvas)
    }

    private fun requestUpdateMatrix() {
        if (isUpdating) return
        matrix.reset()
        invalidate()
    }

    fun setImageTransformer(transformer: ImageTransformer) {
        this.transformer = transformer
        requestUpdateMatrix()
    }

    private fun updateMatrix() {
        isUpdating = true
        imageTranslate.reset()
        transformer.updateMatrix(matrix, imageView = this)
        val imageWidth = drawable?.intrinsicWidth ?: 0
        val imageHeight = drawable?.intrinsicHeight ?: 0
        if (imageWidth > 0 && imageHeight > 0) {
            imageRect.setEmpty()
            imageRect.right = imageWidth.toFloat()
            imageRect.bottom = imageHeight.toFloat()
            matrix.mapRect(imageRect)
            translateAction?.invoke(imageRect.width(), imageRect.height())

            imageTranslate.copyFrom(expectTranslate)
            transformer.updateTranslate(imageTranslate, imageRect, imageView = this)
            matrix.postTranslate(imageTranslate.x, imageTranslate.y)
        }
        imageMatrix = matrix
        isUpdating = false
    }
}