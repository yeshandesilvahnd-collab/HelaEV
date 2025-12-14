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

        // Get the range calculated from the previous screen
        val range = intent.getIntExtra("RANGE_RESULT", 0)

        // Wait for 3 seconds (3000ms), then go to Maps
        Handler(Looper.getMainLooper()).postDelayed({
            val mapIntent = Intent(this, MapsActivity::class.java)
            mapIntent.putExtra("RANGE_RESULT", range) // Pass the data forward
            startActivity(mapIntent)
            finish() // Close this loading screen so user can't go back to it
        }, 3000)
    }
}