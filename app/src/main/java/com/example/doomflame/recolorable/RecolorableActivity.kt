package com.example.doomflame.recolorable

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import com.example.doomflame.R

class RecolorableActivityIntent(context: Context) : Intent(
    context, RecolorableActivity::class.java
)

private fun Resources.themeOf(res: Int) = newTheme().apply {
    applyStyle(res, true)
}

class RecolorableActivity : ThemableActivity() {
    private val lightTheme by lazy {
        resources.themeOf(R.style.Theme_Activity_Recolorable_Light)
    }
    private val darkTheme by lazy {
        resources.themeOf(R.style.Theme_Activity_Recolorable_Dark)
    }

    @SuppressLint("InflateParams")
    override fun onCreate(savedInstanceState: Bundle?) {
        theme = lightTheme
        super.onCreate(savedInstanceState)
        setContentView(
            layoutInflater.inflate(R.layout.activity_recolorable, null)
        )
        findViewById<View>(R.id.light).setOnClickListener { theme = lightTheme }
        findViewById<View>(R.id.dark).setOnClickListener { theme = darkTheme }
    }
}