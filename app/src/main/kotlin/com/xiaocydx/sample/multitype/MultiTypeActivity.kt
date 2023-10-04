package com.xiaocydx.sample.multitype

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.xiaocydx.sample.R
import com.xiaocydx.sample.databinding.ActivityMultitypeBinding
import com.xiaocydx.sample.multitype.onetomany.OneToManyFragment
import com.xiaocydx.sample.multitype.onetoone.OneToOneFragment
import com.xiaocydx.sample.onClick

/**
 * item多类型示例代码
 *
 * @author xcc
 * @date 2022/2/17
 */
class MultiTypeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(contentView())
        if (savedInstanceState == null) replace<OneToOneFragment>()
    }

    private fun contentView() = ActivityMultitypeBinding
        .inflate(layoutInflater).apply {
            btnOneToOne.onClick { replace<OneToOneFragment>() }
            btnOneToMany.onClick { replace<OneToManyFragment>() }
        }.root

    private inline fun <reified T : Fragment> replace() {
        supportFragmentManager.commit { replace(R.id.container, T::class.java, null) }
    }
}