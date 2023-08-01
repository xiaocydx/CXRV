@file:JvmName("TransitionInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.transition

import android.annotation.SuppressLint
import android.view.ViewGroup

@SuppressLint("RestrictedApi")
fun Transition.dependsOn(other: Transition, sceneRoot: ViewGroup) {
    val source = this
    if (source === other) return
    other.addListener(object : Transition.TransitionListener {
        override fun onTransitionStart(transition: Transition) = source.start()
        override fun onTransitionPause(transition: Transition) = source.pause(sceneRoot)
        override fun onTransitionResume(transition: Transition) = source.resume(sceneRoot)
        override fun onTransitionCancel(transition: Transition) {
            other.removeListener(this)
            source.cancel()
        }

        override fun onTransitionEnd(transition: Transition) {
            other.removeListener(this)
            source.end()
        }
    })
}