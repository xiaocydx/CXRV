package com.xiaocydx.sample.multitype

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xiaocydx.accompanist.view.layoutParams
import com.xiaocydx.accompanist.view.matchParent
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.accompanist.view.wrapContent
import com.xiaocydx.cxrv.itemclick.doOnSimpleItemClick
import com.xiaocydx.cxrv.list.doOnAttach
import com.xiaocydx.cxrv.list.grid
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.R
import com.xiaocydx.sample.common.Action
import com.xiaocydx.sample.common.initActionList
import com.xiaocydx.sample.databinding.ActionContainerBinding
import com.xiaocydx.sample.multitype.MultiTypeActivity.MultiTypeAction.ONE_TO_ONE
import com.xiaocydx.sample.multitype.onetomany.OneToManyFragment
import com.xiaocydx.sample.multitype.onetoone.OneToOneFragment
import kotlin.reflect.KClass

/**
 * MultiType示例代码
 *
 * @author xcc
 * @date 2022/2/17
 */
class MultiTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        if (savedInstanceState == null) {
            performMultiTypeAction(ONE_TO_ONE, show = false)
        }
    }

    private fun contentView() = ActionContainerBinding
        .inflate(layoutInflater).initActionList {
            submitList(MultiTypeAction.values().toList())
            doOnSimpleItemClick(::performMultiTypeAction)
            doOnAttach { rv -> rv.grid(spanCount = 2) }
            onCreateView { root.layoutParams(matchParent, wrapContent) }
        }.root

    private fun performMultiTypeAction(action: MultiTypeAction, show: Boolean = true) {
        supportFragmentManager.commit { replace(R.id.container, action.clazz.java, null) }
        if (!show) return
        window.decorView.snackbar().setText("替换为${action.clazz.java.simpleName}").show()
    }

    private enum class MultiTypeAction(
        override val text: String,
        val clazz: KClass<out Fragment>
    ) : Action {
        ONE_TO_ONE("OneToOne多类型", OneToOneFragment::class),
        ONE_TO_MANY("OneToMany多类型", OneToManyFragment::class)
    }
}