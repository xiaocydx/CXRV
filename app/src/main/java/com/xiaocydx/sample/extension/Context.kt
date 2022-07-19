@file:Suppress("PackageDirectoryMismatch")

package com.xiaocydx.sample

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment

fun Context.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, text, duration).show()
}

fun Fragment.showToast(text: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    requireContext().showToast(text, duration)
}