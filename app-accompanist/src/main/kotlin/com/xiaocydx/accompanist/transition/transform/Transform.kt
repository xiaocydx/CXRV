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

import android.graphics.Matrix
import android.graphics.RectF
import android.widget.ImageView
import java.lang.Float.min
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.round
import kotlin.math.sign

/**
 * 过渡动画的中间者，负责协调Sender页面和Receiver页面
 *
 * **注意**：当前实现逻辑很简单，只支持两个页面的过渡，不支持多个页面进行嵌套。
 *
 * @author xcc
 * @date 2024/11/19
 */
object Transform

/**
 * 图片变换
 *
 * @author xcc
 * @date 2024/11/19
 */
interface ImageTransformer {

    /**
     * 更新图片矩阵
     */
    fun updateMatrix(matrix: Matrix, imageView: ImageView)

    /**
     * 更新图片平移
     *
     * @param translate 已有的平移值
     * @param imageRect 基于[updateMatrix]的矩阵得到的图片坐标
     */
    fun updateTranslate(translate: Translate, imageRect: RectF, imageView: ImageView) = Unit

    companion object
}

/**
 * x和y的平移数值
 */
data class Translate(var x: Float = 0f, var y: Float = 0f) {

    fun copyFrom(other: Translate) {
        x = other.x
        y = other.y
    }

    fun reset() {
        x = 0f
        y = 0f
    }
}

/**
 * 居中填充显示图片，最终的平移值不会使Image脱离ImageView边界
 */
fun ImageTransformer.Companion.fitCenter(): ImageTransformer = FitCenterTransformer

/**
 * 居中裁剪显示图片，最终的平移值不会使Image脱离ImageView边界
 */
fun ImageTransformer.Companion.centerCrop(): ImageTransformer = CenterCropTransformer

private object CenterCropTransformer : ImageTransformer {

    override fun updateMatrix(matrix: Matrix, imageView: ImageView) {
        imageView.centerCropMatrix(matrix)
    }

    override fun updateTranslate(translate: Translate, imageRect: RectF, imageView: ImageView) {
        val dx = (imageRect.width() - imageView.width) / 2
        val dy = (imageRect.height() - imageView.height) / 2
        var max = dx.coerceAtLeast(0f)
        translate.x = translate.x.absoluteValue.coerceAtMost(max) * translate.x.sign
        max = dy.coerceAtLeast(0f)
        translate.y = translate.y.absoluteValue.coerceAtMost(max) * translate.y.sign
    }
}

private object FitCenterTransformer : ImageTransformer {

    override fun updateMatrix(matrix: Matrix, imageView: ImageView) {
        imageView.fitCenterMatrix(matrix)
    }

    override fun updateTranslate(translate: Translate, imageRect: RectF, imageView: ImageView) {
        val dx = (imageRect.width() - imageView.width) / 2
        val dy = (imageRect.height() - imageView.height) / 2
        var max = dx.coerceAtLeast(0f)
        translate.x = translate.x.absoluteValue.coerceAtMost(max) * translate.x.sign
        max = dy.coerceAtLeast(0f)
        translate.y = translate.y.absoluteValue.coerceAtMost(max) * translate.y.sign
    }
}

/**
 * 基于当前`ImageView.drawable`、[width]、[height]，对[matrix]设置居中填充的结果
 */
fun ImageView.fitCenterMatrix(matrix: Matrix, width: Int = this.width, height: Int = this.height) {
    matrix.reset()
    val imageWidth = drawable?.intrinsicWidth ?: 0
    val imageHeight = drawable?.intrinsicHeight ?: 0
    if (imageWidth > 0 && imageHeight > 0) {
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val minScale = min(scaleX, scaleY)
        val w = imageWidth * minScale
        val h = imageHeight * minScale
        val tx = round((width - w) / 2f)
        val ty = round((height - h) / 2f)
        matrix.postScale(minScale, minScale)
        matrix.postTranslate(tx, ty)
    }
}

/**
 * 基于当前`ImageView.drawable`、[width]、[height]，对[matrix]设置居中裁剪的结果
 */
fun ImageView.centerCropMatrix(matrix: Matrix, width: Int = this.width, height: Int = this.height) {
    matrix.reset()
    val imageWidth = drawable?.intrinsicWidth ?: 0
    val imageHeight = drawable?.intrinsicHeight ?: 0
    if (imageWidth > 0 && imageHeight > 0) {
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val maxScale = max(scaleX, scaleY)
        val w = imageWidth * maxScale
        val h = imageHeight * maxScale
        val tx = round((width - w) / 2f)
        val ty = round((height - h) / 2f)
        matrix.postScale(maxScale, maxScale)
        matrix.postTranslate(tx, ty)
    }
}