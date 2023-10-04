package com.xiaocydx.sample.itemselect

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActivityItemSelectBinding
import com.xiaocydx.sample.onClick

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
        if (savedInstanceState == null) replace<SingleSelectionFragment>()
    }

    private fun contentView() = ActivityItemSelectBinding
        .inflate(layoutInflater).apply {
            btnSingleSelection.onClick { replace<SingleSelectionFragment>() }
            btnMultiSelection.onClick { replace<MultiSelectionFragment>() }
        }.root

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(R.id.container, T::class.java, null) }
    }
}