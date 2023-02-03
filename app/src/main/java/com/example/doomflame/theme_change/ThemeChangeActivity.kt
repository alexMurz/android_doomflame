package com.example.doomflame.theme_change

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import com.example.doomflame.R

private const val IS_DARK_THEME = "is_dark"

class ThemeChangeIntent(context: Context) : Intent(context, ThemeChangeActivity::class.java)

class ThemeChangeActivity : AppCompatActivity() {
    private val store by lazy(LazyThreadSafetyMode.NONE) {
        getSharedPreferences("app", Context.MODE_PRIVATE)
    }

    //    private lateinit var changerView: ThemeChangerView
//    private lateinit var changerState: SnapshotStateHolder
    private lateinit var swapLayout: SwappingFrameLayout

    private val lightView by lazy {
        createContentView(baseContext, true)
    }
    private val darkView by lazy {
        createContentView(baseContext, true)
    }

    private fun setView(isLight: Boolean) {
        val view = if (isLight) lightView else darkView
        swapLayout.updateView(view)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        swapLayout = SwappingFrameLayout(this)
        setView(isCurrentThemeLight())
        setContentView(swapLayout)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setView(isCurrentThemeLight())
    }

    private fun changeTheme() {
        // Change theme
        val theme = store.getBoolean(IS_DARK_THEME, false)
        store.edit().putBoolean(IS_DARK_THEME, !theme).apply()

        AppCompatDelegate.setDefaultNightMode(
            if (theme) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun createContentView(rootContext: Context, isLight: Boolean): View {
        val context = ContextThemeWrapper(
            rootContext,
            if (isLight)
                R.style.Theme_Activity_ThemeChange_Light
            else
                R.style.Theme_Activity_ThemeChange_Night
        )
        return LayoutInflater.from(context).inflate(R.layout.activity_theme_change, null)
            .apply {
                findViewById<View>(R.id.app_bar_layout).apply {
                    val topPadding = statusBarHeight()
                    setPadding(0, topPadding, 0, 0)
                }

                setSupportActionBar(findViewById<Toolbar>(R.id.toolbar).apply {
                    title = "Theme change"
                })

                println("Start activity ${System.identityHashCode(this)}")

                findViewById<Button>(R.id.button1).setOnClickListener {
                    changeTheme()
                }
            }
    }

    private fun isCurrentThemeLight(): Boolean {
        val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == Configuration.UI_MODE_NIGHT_NO
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
}