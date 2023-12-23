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

@file:JvmName("TransformRootInternalKt")
@file:Suppress("PackageDirectoryMismatch")

package androidx.fragment.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.transition.Transition
import com.google.android.material.transition.MaterialContainerTransform
import com.xiaocydx.accompanist.R
import com.xiaocydx.accompanist.transition.transform.TransformTransition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.mapNotNull
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
    private val stateHolder: StateHolder,
    private val fragmentManager: FragmentManager
) {
    private val containerId = android.R.id.content
    private var transactionWho: String? = null
    private val senderViewRefs = mutableMapOf<String, WeakReference<View>>()
    private val fragmentMaxLifecycleEnforcer = FragmentMaxLifecycleEnforcer()
    private val receiverReturn = MutableSharedFlow<String>(extraBufferCapacity = Int.MAX_VALUE)

    constructor(activity: FragmentActivity) : this(
        lifecycle = activity.lifecycle,
        stateHolder = ViewModelProvider(activity)[StateHolder::class.java],
        fragmentManager = activity.supportFragmentManager
    )

    fun setSenderView(sender: Fragment, view: View?) {
        clearInvalidSenderViewRefs()
        val senderView = senderViewRefs[sender.mWho]?.get()
        if (senderView === view) return
        senderViewRefs[sender.mWho] = WeakReference(view)
    }

    fun receiverReturn(sender: Fragment): Flow<Unit> {
        val senderWho = sender.mWho
        return receiverReturn.mapNotNull { if (it == senderWho) Unit else null }
    }

    fun forwardReceiver(
        sender: Fragment,
        receiverClass: KClass<out Fragment>,
        args: Bundle? = null
    ): Boolean {
        if (lifecycle.currentState == DESTROYED) return false
        if (transactionWho != null) return false
        val receiver = createFragment(receiverClass, args)
        val receiverTag = stateHolder.createTagBySenderWho(sender)
        transactionWho = receiver.mWho
        val fragments = stateHolder.findResumedFragments(fragmentManager)
        stateHolder.recordResumedFragments(receiver, fragments)
        fragmentMaxLifecycleEnforcer.updateFragmentToStarted(receiver, fragments)
        fragmentManager
            .beginTransaction()
            .setReorderingAllowed(true)
            .addToBackStack(null)
            .add(containerId, receiver, receiverTag)
            .commit()
        return true
    }

    fun createReceiverTransition(receiver: Fragment, transform: MaterialContainerTransform): Transition {
        val receiverRef = WeakReference(receiver)
        return TransformTransition(containerId, transform) { start ->
            val receiverF = receiverRef.get()
            val senderWho = stateHolder.parseSenderWhoFromTag(receiverF?.tag ?: "")
            clearInvalidSenderViewRefs()
            val targetView = when {
                receiverF == null -> null
                receiverF.isAdded -> if (start) senderViewRefs[senderWho]?.get() else receiverF.view
                else -> if (start) receiverF.view else senderViewRefs[senderWho]?.get()
            }
            if (start && targetView === receiverF?.view) {
                senderWho.takeIf { it.isNotEmpty() }?.let(receiverReturn::tryEmit)
            }
            targetView
        }
    }

    @SuppressLint("RestrictedApi")
    private fun createFragment(clazz: KClass<out Fragment>, args: Bundle?): Fragment {
        val fragmentFactory = fragmentManager.fragmentFactory
        val classLoader = fragmentManager.host.context.classLoader
        val fragment = fragmentFactory.instantiate(classLoader, clazz.java.name)
        if (args != null) fragment.arguments = args
        return fragment
    }

    private fun clearInvalidSenderViewRefs() {
        var toRemove: ArrayList<String>? = null
        senderViewRefs.entries.forEach action@{
            if (it.value.get() != null) return@action
            if (toRemove == null) toRemove = arrayListOf()
            toRemove!!.add(it.key)
        }
        val keys = toRemove ?: return
        for (i in keys.indices) senderViewRefs.remove(keys[i])
    }

    private inner class FragmentMaxLifecycleEnforcer {
        private val pendingUpdateOps = ArrayList<UpdateOp>()

        init {
            lifecycle.addObserver(LifecycleEventObserver { _, _ ->
                // 当动画结束时，可能错过了saveState，不允许提交事务，
                // 因此观察Lifecycle的状态更改，尝试提交事务修正状态。
                val ops = pendingUpdateOps
                if (ops.isNotEmpty() && !fragmentManager.isStateSaved) {
                    for (i in ops.indices) updateFragmentMaxLifecycle(ops[i])
                    pendingUpdateOps.clear()
                }
            })
            fragmentManager.registerFragmentLifecycleCallbacks(
                object : FragmentManager.FragmentLifecycleCallbacks() {
                    override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                        if (f.mWho == transactionWho) transactionWho = null
                    }

                    override fun onFragmentDestroyed(fm: FragmentManager, f: Fragment) {
                        if (!stateHolder.hasRecordedFragments(f)) return
                        updateFragmentToResumed(f)
                    }
                },
                false
            )
        }

        fun updateFragmentToStarted(receiver: Fragment, fragments: List<Fragment>) {
            updateFragmentMaxLifecycle(UpdateOp(receiver.mWho, STARTED), fragments)
        }

        fun updateFragmentToResumed(receiver: Fragment) {
            updateFragmentMaxLifecycle(UpdateOp(receiver.mWho, RESUMED))
        }

        private fun updateFragmentMaxLifecycle(op: UpdateOp, fragments: List<Fragment>? = null) {
            if (fragmentManager.isStateSaved) {
                pendingUpdateOps.takeIf { !it.contains(op) }?.add(op)
                return
            }
            val (who, state) = op
            val toUpdate = fragments ?: stateHolder.findRecordedFragments(who, fragmentManager)
            if (state == RESUMED) stateHolder.clearRecordedFragments(who)
            if (toUpdate.isEmpty()) return
            val transaction = fragmentManager.beginTransaction()
            toUpdate.forEach { transaction.setMaxLifecycle(it, state) }
            transaction.commit()
        }
    }

    private data class UpdateOp(val who: String, val state: Lifecycle.State)

    internal class StateHolder(private val savaStateHandle: SavedStateHandle) : ViewModel() {

        fun createTagBySenderWho(sender: Fragment): String {
            return "$TAG_RECEIVER_PREFIX${sender.mWho}"
        }

        fun parseSenderWhoFromTag(tag: String): String {
            if (!tag.startsWith(TAG_RECEIVER_PREFIX)) return ""
            return tag.substring(TAG_RECEIVER_PREFIX.length)
        }

        fun findResumedFragments(fm: FragmentManager): List<Fragment> {
            return fm.fragments.filter { it.lifecycle.currentState.isAtLeast(RESUMED) }
        }

        fun recordResumedFragments(receiver: Fragment, fragments: List<Fragment>) {
            savaStateHandle[receiver.mWho] = ArrayList(fragments.map { it.mWho })
        }

        fun hasRecordedFragments(receiver: Fragment): Boolean {
            return !savaStateHandle.get<ArrayList<String>>(receiver.mWho).isNullOrEmpty()
        }

        fun findRecordedFragments(receiver: Fragment, fm: FragmentManager): List<Fragment> {
            return findRecordedFragments(receiver.mWho, fm)
        }

        fun findRecordedFragments(who: String, fm: FragmentManager): List<Fragment> {
            val fragments = savaStateHandle.get<ArrayList<String>>(who)
            if (fragments.isNullOrEmpty()) return emptyList()
            return fm.fragments.filter { fragments.contains(it.mWho) }
        }

        fun clearRecordedFragments(who: String) {
            savaStateHandle.remove<ArrayList<String>>(who)
        }
    }

    private companion object {
        const val TAG_RECEIVER_PREFIX = "From-Sender#"
    }
}

internal fun Fragment.requireTransformRoot() = requireNotNull(findTransformRoot())

internal fun Fragment.findTransformRoot(): TransformRoot? {
    val activity = activity ?: return null
    val tag = R.id.tag_decor_transform_root
    var root = activity.window.decorView.getTag(tag) as? TransformRoot?
    if (root == null) {
        root = TransformRoot(activity)
        activity.window.decorView.setTag(tag, root)
    }
    return root
}