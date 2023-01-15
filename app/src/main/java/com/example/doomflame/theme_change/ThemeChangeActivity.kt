package com.example.doomflame.theme_change

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doomflame.R

private const val IS_DARK_THEME = "is_dark"
private const val SAVED_SNAPSHOT = "snapshot"

class ThemeChangeIntent(context: Context) : Intent(context, ThemeChangeActivity::class.java)

class ThemeChangeActivity : AppCompatActivity(R.layout.activity_theme_change) {
    private val store by lazy(LazyThreadSafetyMode.NONE) {
        getSharedPreferences("app", Context.MODE_PRIVATE)
    }

    private lateinit var changerView: ThemeChangerView
    private lateinit var changerState: SnapshotStateHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        findViewById<View>(R.id.app_bar_layout).apply {
            val topPadding = statusBarHeight()
            setPadding(0, topPadding, 0, 0)
        }

        changerState = savedInstanceState
            ?.getParcelable(SAVED_SNAPSHOT) as? SnapshotStateHolder
            ?: SnapshotStateHolder()
        println("Restore state $changerState")

        changerView = findViewById<ThemeChangerView>(R.id.container).apply {
            stateHolder = changerState
            animateDrop()
        }

        println("Start activity ${System.identityHashCode(this)}")

        findViewById<Button>(R.id.button1).setOnClickListener {
            changeTheme(ANIM_CROSS_FADE)
        }
        findViewById<Button>(R.id.button2).setOnClickListener {
            changeTheme(ANIM_FLIP_OVER)
        }
        findViewById<Button>(R.id.button3).setOnClickListener {
            changeTheme(ANIM_CIRCLE)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        println("Save instance")
        outState.putParcelable(SAVED_SNAPSHOT, changerState)
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun statusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            val heightDp = if (VERSION.SDK_INT >= VERSION_CODES.M) 24 else 25
            (resources.displayMetrics.density * heightDp).toInt()
        }
    }

    private fun changeTheme(style: Int) {
        // Change theme
        val theme = store.getBoolean(IS_DARK_THEME, false)
        store.edit().putBoolean(IS_DARK_THEME, !theme).apply()

        changerState.animation = style
        changerView.makeSnapshot()

        AppCompatDelegate.setDefaultNightMode(
            if (theme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        println("End activity ${System.identityHashCode(this)}")
    }
}