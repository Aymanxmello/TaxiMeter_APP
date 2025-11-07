package com.cmc.taximeter

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class OnboardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)


        val btnStart = findViewById<Button>(R.id.btnStart)
        btnStart.setOnClickListener {
            completeOnboarding()
        }
    }

    private fun completeOnboarding() {
        val sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("hasSeenOnboarding", true)
        editor.apply()

        val intent = Intent(this, AuthenticationActivity::class.java)
        startActivity(intent)
        finish()
    }
}