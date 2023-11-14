package com.xiaocydx.sample.itemselect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.doOnAttach
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActionContainerBinding
import com.xiaocydx.sample.extensions.Action
import com.xiaocydx.sample.extensions.initActionList
import com.xiaocydx.sample.itemselect.ItemSelectActivity.ItemSelectAction.SINGLE
import com.xiaocydx.sample.layoutParams
import com.xiaocydx.sample.matchParent
import com.xiaocydx.sample.snackbar
import com.xiaocydx.sample.wrapContent
import kotlin.reflect.KClass

/**
 * ItemSelect示例代码
 *
 * @author xcc
 * @date 2023/8/23
 */
class ItemSelectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        if (savedInstanceState == null) {
            performItemSelectAction(SINGLE, show = false)
        }
    }

    private fun contentView() = ActionContainerBinding
        .inflate(layoutInflater).initActionList {
            submitList(ItemSelectAction.values().toList())
            doOnSimpleItemClick(::performItemSelectAction)
            doOnAttach { rv -> rv.grid(spanCount = 2) }
            onCreateView { root.layoutParams(matchParent, wrapContent) }
        }.root

    private fun performItemSelectAction(action: ItemSelectAction, show: Boolean = true) {
        supportFragmentManager.commit { replace(R.id.container, action.clazz.java, null) }
        if (!show) return
        window.decorView.snackbar().setText("替换为${action.clazz.java.simpleName}").show()
    }

    private enum class ItemSelectAction(
        override val text: String,
        val clazz: KClass<out Fragment>
    ) : Action {
        SINGLE("单项选择", SingleSelectionFragment::class),
        MULTI("多项选择", MultiSelectionFragment::class)
    }
}