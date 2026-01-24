package com.yesh.helaev

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsername = findViewById<EditText>(R.id.etUsername)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()
            val mobile = findViewById<EditText>(R.id.etMobile).text.toString() // Get Mobile

            if (username.isNotEmpty() && mobile.isNotEmpty()) {
                // Save Username Locally
                val prefs = getSharedPreferences("HelaEV_User", Context.MODE_PRIVATE)
                prefs.edit().putString("CURRENT_USER", username).apply()
                // We can save mobile too if needed, but username is enough for the display

                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Please enter Name and Mobile", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString()

            if (username.isNotEmpty()) {
                // Save Username Locally
                val prefs = getSharedPreferences("HelaEV_User", Context.MODE_PRIVATE)
                prefs.edit().putString("CURRENT_USER", username).apply()

                // Go to Main Screen
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish() // Prevents going back to login
            } else {
                Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            }


        }
    }
}

