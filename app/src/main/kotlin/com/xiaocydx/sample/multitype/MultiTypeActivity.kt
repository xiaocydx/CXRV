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
        if (savedInstanceState == null) initOneToOneFragment()
    }

    private fun contentView() = ActivityMultitypeBinding
        .inflate(layoutInflater).apply {
            btnOneToOne.onClick(::initOneToOneFragment)
            btnOneToMany.onClick(::initOneToManyFragment)
        }.root

    private fun initOneToOneFragment() {
        replaceFragment(OneToOneFragment())
    }

    private fun initOneToManyFragment() {
        replaceFragment(OneToManyFragment())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.commit { replace(R.id.container, fragment) }
    }
}