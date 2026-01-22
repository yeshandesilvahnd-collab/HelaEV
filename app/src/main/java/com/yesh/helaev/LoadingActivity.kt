package com.yesh.helaev

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val range = intent.getIntExtra("RANGE_RESULT", 0)
        // Get the mode passed from MainActivity
        val userMode = intent.getStringExtra("USER_MODE")

        Handler(Looper.getMainLooper()).postDelayed({
            val mapIntent = Intent(this, MapsActivity::class.java)
            mapIntent.putExtra("RANGE_RESULT", range)
            mapIntent.putExtra("USER_MODE", userMode) // Pass it forward!
            startActivity(mapIntent)
            finish()
        }, 3000)
    }
}