package com.xiaocydx.sample

import android.content.Context
import android.content.res.Resources
import android.util.TypedValue
import android.widget.Toast
import androidx.fragment.app.Fragment

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

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Fragment.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(text, duration)
}