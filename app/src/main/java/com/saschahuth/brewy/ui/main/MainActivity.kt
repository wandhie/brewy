package com.saschahuth.brewy.ui.main

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.saschahuth.brewy.BaseActivity
import com.saschahuth.brewy.R
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adapter = MainContentFragmentAdapter(supportFragmentManager)
        adapter.addFragment(NearbyBreweriesFragment.newInstance(), "Nearby")
        adapter.addFragment(BeersFragment.newInstance(), "Beers")
        viewPager.adapter = adapter

        tabs.setupWithViewPager(viewPager)

        MainComponent.Initializer.init(this).inject(this)
    }

    class MainContentFragmentAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val fragments = ArrayList<Fragment>()
        private val fragmentTitles = ArrayList<String>()

        fun addFragment(fragment: Fragment, title: String) {
            fragments.add(fragment)
            fragmentTitles.add(title)
        }

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return fragments.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return fragmentTitles[position]
        }
    }
}