package com.ttv.spleeterdemo

import android.content.Intent
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.ttv.spleeter.SpleeterSDK

class HomeActivity : AppCompatActivity() {
    private lateinit var frameLayout: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val bottomAppBar = findViewById<BottomAppBar>(R.id.bottomAppBar)
        val btnHome = findViewById<ImageButton>(R.id.btnHome)
        val btnProfile = findViewById<ImageButton>(R.id.btnProfile)
        val btnImport = findViewById<FloatingActionButton>(R.id.fabImport)
        frameLayout = findViewById(R.id.frameLayout)

        val radius = 60f
        val bottomBarBackground = bottomAppBar.background as MaterialShapeDrawable
        bottomBarBackground.shapeAppearanceModel = bottomBarBackground.shapeAppearanceModel
            .toBuilder()
            .setAllCornerSizes(radius)
            .build()

        Thread{
            SpleeterSDK.createInstance(this).create()
        }.start()


        val homeFragment = HomeFragment()
        val profileFragment = ProfileFragment()

        changeFragment(homeFragment)

        btnHome.setOnClickListener {
            changeFragment(homeFragment)
        }

        btnProfile.setOnClickListener {
            changeFragment(profileFragment)
        }

        btnImport.setOnClickListener{
            val intent = Intent(this, ImportActivity::class.java)
            startActivity(intent)
        }

        SpleeterSDK.createInstance(this).create()
    }

    private fun changeFragment(frag: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.frameLayout, frag)
        transaction.commit()
    }

}