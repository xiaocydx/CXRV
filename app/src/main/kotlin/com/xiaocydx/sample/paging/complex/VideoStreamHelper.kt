package com.xiaocydx.sample.paging.complex

import android.content.Intent
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.annotation.CheckResult
import androidx.annotation.MainThread
import androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation
import androidx.lifecycle.Lifecycle.State.DESTROYED
import androidx.lifecycle.ViewModelStoreOwner
import com.xiaocydx.sample.doOnTargetState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * @author xcc
 * @date 2023/7/30
 */
object VideoStreamHelper {
    private const val NO_TOKEN = -1
    private const val KEY_OWNER_ID = "KEY_OWNER_ID"
    private val events = mutableMapOf<Int, MutableSharedFlow<VideoStreamEvent>>()
    private val params = mutableMapOf<Int, VideoStreamParams>()
    private val ViewModelStoreOwner.token: Int
        get() = viewModelStore.hashCode()

    @MainThread
    @CheckResult
    fun token(activity: ComponentActivity): Int {
        assertMainThread()
        val intent = activity.intent ?: return NO_TOKEN
        return intent.getIntExtra(KEY_OWNER_ID, NO_TOKEN)
    }

    @MainThread
    fun send(token: Int, event: VideoStreamEvent) {
        assertMainThread()
        events[token]?.tryEmit(event)
    }

    @MainThread
    fun event(activity: ComponentActivity): Flow<VideoStreamEvent> {
        assertMainThread()
        var event: MutableSharedFlow<VideoStreamEvent>? = events[activity.token]
        if (event == null) {
            event = MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
            events[activity.token] = event
            activity.lifecycle.doOnTargetState(DESTROYED) { events.remove(activity.token) }
        }
        return event
    }

    @MainThread
    fun start(
        activity: ComponentActivity,
        sharedElement: View,
        targetItem: ComplexItem,
        currentList: List<ComplexItem>,
        nextKey: Int?
    ) {
        assertMainThread()
        val data = currentList.filter { it.type == ComplexItem.TYPE_VIDEO }
        val position = data.indexOfFirst { it.id == targetItem.id }
        val params = VideoStreamParams(data, nextKey, position)
        this.params[activity.token] = params
        val intent = Intent(activity, VideoStreamActivity::class.java)
        intent.putExtra(KEY_OWNER_ID, activity.token)
        val options = makeSceneTransitionAnimation(activity, sharedElement, params.sharedName)
        activity.startActivity(intent, options.toBundle())
    }

    @MainThread
    @CheckResult
    fun consume(token: Int): VideoStreamParams {
        assertMainThread()
        return requireNotNull(params.remove(token))
    }

    private fun assertMainThread() {
        assert(Thread.currentThread() === Looper.getMainLooper().thread)
    }
}

data class VideoStreamParams(
    val data: List<ComplexItem>,
    val nextKey: Int?,
    val position: Int,
    val sharedName: String = data.getOrNull(position)?.id ?: ""
)

sealed class VideoStreamEvent {
    class Select(val id: String) : VideoStreamEvent()
    class Refresh(val data: List<ComplexItem>, val nextKey: Int?) : VideoStreamEvent()
    class Append(val data: List<ComplexItem>, val nextKey: Int?) : VideoStreamEvent()
}