package com.xiaocydx.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.xiaocydx.sample.itemselect.SingleSelectionActivity
import com.xiaocydx.sample.itemclick.ItemClickActivity
import com.xiaocydx.sample.itemselect.MultiSelectionActivity
import com.xiaocydx.sample.multitype.MultiTypeActivity
import com.xiaocydx.sample.paging.PagingActivity

/**
 * @author xcc
 * @date 2022/2/17
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun startItemClickActivity(view: View) {
        startActivity<ItemClickActivity>()
    }

    fun startSingleSelectionActivity(view: View) {
        startActivity<SingleSelectionActivity>()
    }

    fun startMultiSelectionActivity(view: View) {
        startActivity<MultiSelectionActivity>()
    }

    fun startMultiTypeActivity(view: View) {
        startActivity<MultiTypeActivity>()
    }

    fun startPagingActivity(view: View) {
        startActivity<PagingActivity>()
    }

    private inline fun <reified T : Activity> startActivity() {
        startActivity(Intent(this, T::class.java))
    }
}