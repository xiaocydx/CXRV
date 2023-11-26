@file:JvmName("TransformSceneRootInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.fragment.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.Transition
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.transition.platform.MaterialContainerTransform
import com.xiaocydx.sample.R
import com.xiaocydx.sample.transition.transform.TransformReceiver
import com.xiaocydx.sample.transition.transform.TransformTransition
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * 变换过渡动画的Root，负责完成整体调度
 *
 * @author xcc
 * @date 2023/8/1
 */
internal class TransformRoot private constructor(
    private val lifecycle: Lifecycle,
    private val saveState: SaveState,
    private val fragmentManager: FragmentManager
) {
    private val containerId = android.R.id.content
    private var incompleteReceiver: String? = null
    private var senderViewRef: WeakReference<View>? = null
    private val fragmentMaxLifecycleEnforcer = FragmentMaxLifecycleEnforcer()
    private val _receiverReturn = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // TODO: 实现Sender-Receiver的对应关系
    val receiverReturn = _receiverReturn.asSharedFlow()

    constructor(activity: FragmentActivity) : this(
        lifecycle = activity.lifecycle,
        saveState = ViewModelProvider(activity)[SaveState::class.java],
        fragmentManager = activity.supportFragmentManager
    )

    fun <R> forwardReceiver(
        receiverClass: KClass<out R>,
        args: Bundle? = null
    ): Boolean where R : Fragment, R : TransformReceiver {
        if (incompleteReceiver != null) return false
        val receiverFragment = createReceiverFragment(receiverClass, args)
        val resumedFragments = findCurrentResumedFragments()
        recordResumedFragments(receiverFragment.mWho, resumedFragments)
        // transformFragment的生命周期状态，在过渡动画结束后才转换为RESUMED，
        // 此时将contentFragment的生命周期状态回退至STARTED，是为了确保过渡动画流畅。
        fragmentMaxLifecycleEnforcer.updateFragmentMaxLifecycle(receiverFragment.mWho, STARTED)
        fragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .add(containerId, receiverFragment)
            .commit()
        return true
    }

    fun setSenderView(view: View?) {
        // TODO: 实现Sender-Receiver的对应关系
        if (senderViewRef?.get() === view) return
        senderViewRef = WeakReference(view)
    }

    fun createReceiverTransition(
        receiverFragment: Fragment,
        transform: MaterialContainerTransform
    ): Transition {
        val receiverFragmentRef = WeakReference(receiverFragment)
        return TransformTransition(containerId, transform) { start ->
            val f = receiverFragmentRef.get()
            val targetView = when {
                f == null -> null
                f.isAdded -> if (start) senderViewRef?.get() else f.view
                else -> if (start) f.view else senderViewRef?.get()
            }
            if (start && targetView === f?.view) {
                // TODO: 实现Sender-Receiver的对应关系
                _receiverReturn.tryEmit(Unit)
            }
            targetView
        }
    }

    @SuppressLint("RestrictedApi")
    private fun createReceiverFragment(
        receiverClass: KClass<out Fragment>,
        args: Bundle? = null
    ): Fragment {
        val fragmentFactory = fragmentManager.fragmentFactory
        val classLoader = fragmentManager.host.context.classLoader
        val fragment = fragmentFactory.instantiate(classLoader, receiverClass.java.name)
        if (args != null) fragment.arguments = args
        return fragment
    }

    private fun findCurrentResumedFragments(): List<Fragment> {
        return fragmentManager.fragments.filter { it.lifecycle.currentState == RESUMED }
    }

    private fun findResumedFragments(receiver: String): List<Fragment> {
        val fragments = saveState.getFragments(receiver)
        if (fragments.isEmpty()) return emptyList()
        return fragmentManager.fragments.filter { fragments.contains(it.mWho) }
    }

    private fun recordResumedFragments(receiver: String, resumedFragments: List<Fragment>) {
        saveState.putFragments(receiver, resumedFragments.map { it.mWho })
    }

    private fun clearResumedFragments(receiver: String) {
        saveState.removeFragments(receiver)
    }

    private inner class FragmentMaxLifecycleEnforcer {
        private var pendingReceiver: String? = null
        private var pendingState: Lifecycle.State? = null

        init {
            lifecycle.addObserver(LifecycleEventObserver { _, _ ->
                // 当动画结束时，可能错过了saveState，不允许提交事务，
                // 因此观察Lifecycle的状态更改，尝试提交事务修正状态。
                if (pendingReceiver != null && pendingState != null) {
                    updateFragmentMaxLifecycle(pendingReceiver!!, pendingState!!)
                }
            })
            fragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        if (f.mWho == incompleteReceiver) incompleteReceiver = null
                    }

                    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                        val maybeReceiver = f.mWho
                        updateFragmentMaxLifecycle(maybeReceiver, RESUMED)
                    }
                },
                false
            )
        }

        fun updateFragmentMaxLifecycle(receiver: String, state: Lifecycle.State) {
            // TODO: 处理不同的receiver
            pendingReceiver = null
            pendingState = null
            if (fragmentManager.isStateSaved) {
                pendingReceiver = receiver
                pendingState = state
                return
            }
            val fragments = findResumedFragments(receiver)
            if (fragments.isEmpty()) return
            if (state == RESUMED) clearResumedFragments(receiver)
            val transaction = fragmentManager.beginTransaction()
            fragments.forEach { transaction.setMaxLifecycle(it, state) }
            transaction.commit()
        }
    }

    // TODO: 检查执行效率和上限异常
    internal class SaveState(private val savaStateHandle: SavedStateHandle) : ViewModel() {

        fun getFragments(key: String): List<String> {
            return savaStateHandle.get<ArrayList<String>>(key) ?: emptyList()
        }

        fun putFragments(key: String, fragments: List<String>) {
            savaStateHandle[key] = ArrayList(fragments)
        }

        fun removeFragments(key: String) {
            savaStateHandle.remove<ArrayList<String>>(key)
        }
    }
}

internal fun Fragment.requireTransformRoot() = requireNotNull(findTransformRoot())

internal fun Fragment.findTransformRoot(): TransformRoot? {
    val activity = activity ?: return null
    val tag = R.id.transform_scene_root
    var root = activity.window.decorView.getTag(tag) as? TransformRoot?
    if (root == null) {
        root = TransformRoot(activity)
        activity.window.decorView.setTag(tag, root)
    }
    return root
}