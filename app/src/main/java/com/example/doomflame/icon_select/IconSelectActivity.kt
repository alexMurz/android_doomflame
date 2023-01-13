package com.example.doomflame.icon_select

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.doomflame.R

fun Context.iconSelectIntent(): Intent = Intent(this, IconSelectActivity::class.java)

class IconSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_icon_select)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, IconSelectFragment.newInstance())
                .commitNow()
        }
    }
}

