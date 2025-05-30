package com.xiaocydx.sample.itemclick

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.accompanist.view.snackbar
import com.xiaocydx.cxrv.itemclick.reduce.doOnItemClick
import com.xiaocydx.cxrv.list.linear
import com.xiaocydx.cxrv.list.submitList
import com.xiaocydx.sample.common.actionList
import com.xiaocydx.sample.databinding.ActionContentBinding
import com.xiaocydx.sample.itemclick.scenes.AbsoluteScenes
import com.xiaocydx.sample.itemclick.scenes.ItemClickScenes
import com.xiaocydx.sample.itemclick.scenes.ListAdapterScenes
import com.xiaocydx.sample.itemclick.scenes.RelativeScenes
import com.xiaocydx.sample.itemclick.scenes.ViewTypeDelegateScenes

/**
 * ItemClick示例代码
 *
 * @author xcc
 * @date 2022/2/18
 */
class ItemClickActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
    }

    private fun contentView() = ActionContentBinding
        .inflate(layoutInflater).apply {
            rvContent.linear()
            rvAction.actionList<ItemClickScenes> {
                submitList(ItemClickScenesList())
                var disposable = currentList.first().apply(rvContent)
                doOnItemClick { item ->
                    disposable.dispose()
                    disposable = item.apply(rvContent)
                    snackbar().setText("切换为${item.text}").show()
                }
            }
        }.root

    /**
     * [ItemClickScenes]演示了从基础函数开始，逐步增强和简化的过程
     */
    @Suppress("FunctionName")
    private fun ItemClickScenesList() = listOf(
        AbsoluteScenes(), RelativeScenes(),
        ListAdapterScenes(), ViewTypeDelegateScenes()
    )
}