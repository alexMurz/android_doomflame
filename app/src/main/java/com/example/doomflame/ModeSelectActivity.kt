package com.example.doomflame

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.doomflame.databinding.ActivityModeSelectBinding
import com.example.doomflame.icon_select.iconSelectIntent
import com.example.doomflame.theme_change.ThemeChangeIntent

class ModeSelectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityModeSelectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityModeSelectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.iconSelect.setOnClickListener {
            startActivity(iconSelectIntent())
        }

        binding.themeChange.setOnClickListener {
            startActivity(ThemeChangeIntent(this))
        }
    }
}