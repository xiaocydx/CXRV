package com.xiaocydx.sample

import android.content.res.Resources
import android.util.TypedValue

val Float.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    ).let { if (it >= 0) it + 0.5f else it - 0.5f }.toInt()

val Double.dp: Int
    get() = toFloat().dp

val Int.dp: Int
    get() = toFloat().dp