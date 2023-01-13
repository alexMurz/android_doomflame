package com.example.doomflame.theme_change

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.doomflame.R
import java.util.*

private const val IS_DARK_THEME = "is_dark"

private val stateHolder = SnapshotStateHolder()

class ThemeChangeIntent(context: Context) : Intent(context, ThemeChangeActivity::class.java)

class ThemeChangeActivity : AppCompatActivity(R.layout.activity_theme_change) {
    private val store by lazy(LazyThreadSafetyMode.NONE) {
        getSharedPreferences("app", Context.MODE_PRIVATE)
    }

    private lateinit var changerView: ThemeChangerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        changerView = findViewById<ThemeChangerView>(R.id.container).apply {
            setStateHolder(stateHolder)
        }
        changerView.animateDrop()

        println("Start activity ${System.identityHashCode(this)}")

        findViewById<Button>(R.id.theme_change_button).setOnClickListener {
            // Change theme
            val theme = store.getBoolean(IS_DARK_THEME, false)
            store.edit().putBoolean(IS_DARK_THEME, !theme).apply()

            changerView.makeSnapshot()

            AppCompatDelegate.setDefaultNightMode(
                if (theme) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("End activity ${System.identityHashCode(this)}")
    }
}