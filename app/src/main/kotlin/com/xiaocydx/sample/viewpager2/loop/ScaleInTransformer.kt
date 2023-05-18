package com.xiaocydx.sample.viewpager2.loop

import android.view.View
import androidx.annotation.FloatRange
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * @author xcc
 * @date 2023/5/18
 */
class ScaleInTransformer(
    @FloatRange(from = 0.0, to = 1.0)
    minScale: Float = 0.85f
) : ViewPager2.PageTransformer {
    private val maxScale = 1f
    private val minScale = minScale.coerceAtLeast(0f)

    override fun transformPage(page: View, position: Float) {
        val fraction = position.absoluteValue
            .coerceAtLeast(0f)
            .coerceAtMost(1f)
        val width = page.width.toFloat()
        val height = page.height.toFloat()
        val sign = position.sign
        val pivotX = width * PIVOT_CENTER
        page.pivotX = pivotX - pivotX * sign * fraction
        page.pivotY = height * PIVOT_CENTER
        page.scaleY = maxScale + (minScale - maxScale) * fraction
    }

    private companion object {
        const val PIVOT_CENTER = 0.5f
    }
}