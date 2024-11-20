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
 * 以展示包含人脸的图片为例，`FaceInfo`是人脸的坐标和尺寸信息，
 * 调用[setImageTranslateAction]，尽量让人脸居中且不超出边界：
 * ```
 * data class FaceInfo(
 *     val percentX: Float, // 人脸左上角x坐标在原图的百分比
 *     val percentY: Float, // 人脸左上角y坐标在原图的百分比
 *     val percentWidth: Float, // 人脸宽度在原图的百分比
 *     val percentHeight: Float // 人脸高度在原图的百分比
 * ) {
 *
 *     // 获取人脸中心点x到当前图片中心点x的平移值
 *     fun getToCenterTranslateX(imageWidth: Float): Float {
 *         val imageCenterX = imageWidth / 2
 *         val faceWidth = percentWidth * imageWidth
 *         val faceCenterX = percentX * imageWidth + faceWidth / 2
 *         return imageCenterX - faceCenterX
 *     }
 *
 *     // 获取人脸中心点y到当前图片中心点y的平移值
 *     fun getToCenterTranslateY(imageHeight: Float): Float {
 *         val imageCenterY = imageHeight / 2
 *         val faceHeight = percentHeight * imageHeight
 *         val faceCenterY = percentY * imageHeight + faceHeight / 2
 *         return imageCenterY - faceCenterY
 *     }
 * }
 *
 * // imageWidth和imageHeight是图片经过矩阵变换后的尺寸
 * imageView.setImageTransformer(ImageTransformer.centerCrop())
 * imageView.setImageTranslateAction { imageWidth, imageHeight ->
 *     val x = faceInfo.getToCenterTranslateX(imageWidth)
 *     val y = faceInfo.getToCenterTranslateY(imageHeight)
 *     imageView.setImageExpectTranslateX(x)
 *     imageView.setImageExpectTranslateY(y)
 * }
 * ```
 *
 * 图片加载过程，根据人脸信息对Bitmap进行裁剪，也能实现效果，
 * 这种做法适用于不需要图片原始比例的Bitmap做过渡动画的场景。
 *
 * @author xcc
 * @date 2024/11/19
 */
class TransformImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs), ImageTransformer.Host {
    private val matrix = Matrix()
    private val imageRect = RectF()
    private val imageTranslate = Translate()
    private val expectTranslate = Translate()
    private var translateAction: ((Float, Float) -> Unit)? = null
    private var isUpdating = false

    override var transformer = ImageTransformer.fitCenter()
        set(value) {
            field = value
            requestUpdateMatrix()
        }

    init {
        scaleType = ScaleType.MATRIX
    }

    /**
     * 设置期望的x平移值，最终的x平移值不会使Image超出ImageView边界
     */
    fun setImageExpectTranslateX(value: Float) {
        if (expectTranslate.x == value) return
        expectTranslate.x = value
        requestUpdateMatrix()
    }

    /**
     * 设置期望的y平移值，最终的y平移值不会使Image超出ImageView边界
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

    override fun postImageTranslate(matrix: Matrix) {
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