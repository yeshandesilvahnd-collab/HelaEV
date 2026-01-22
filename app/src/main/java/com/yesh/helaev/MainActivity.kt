package com.yesh.helaev

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var autoCompleteVehicle: AutoCompleteTextView
    private lateinit var tvBatteryPercentage: TextView
    private lateinit var seekBarBattery: SeekBar
    private lateinit var tvRangeResult: TextView
    private lateinit var btnDirectMe: Button

    // 1. Declare the new button
    private lateinit var btnReporterMode: Button

    private var selectedVehicle: Vehicle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        autoCompleteVehicle = findViewById(R.id.autoCompleteVehicle)
        tvBatteryPercentage = findViewById(R.id.tvBatteryPercentage)
        seekBarBattery = findViewById(R.id.seekBarBattery)
        tvRangeResult = findViewById(R.id.tvRangeResult)
        btnDirectMe = findViewById(R.id.btnDirectMe)

        // 2. Connect the new button ID
        btnReporterMode = findViewById(R.id.btnReporterMode)

        setupVehicleSelector()
        setupSeekBar()

        // --- OPTION A: DRIVER MODE ---
        btnDirectMe.setOnClickListener {
            val rangeText = tvRangeResult.text.toString().filter { it.isDigit() }

            if (rangeText.isNotEmpty() && rangeText.toInt() > 0) {
                val rangeValue = rangeText.toInt()

                // Go to Loading Screen first
                val intent = Intent(this, LoadingActivity::class.java)
                intent.putExtra("RANGE_RESULT", rangeValue)
                intent.putExtra("USER_MODE", "DRIVER") // Tell Map we are driving
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a vehicle first!", Toast.LENGTH_SHORT).show()
            }
        }

        // --- OPTION B: REPORTER MODE ---
        btnReporterMode.setOnClickListener {
            // Go Straight to Map (No Loading screen needed)
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("USER_MODE", "REPORTER") // Tell Map we are reporting
            startActivity(intent)
        }
    }

    private fun setupVehicleSelector() {
        val vehicleNames = VehicleRepository.vehicleList.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicleNames)
        autoCompleteVehicle.setAdapter(adapter)

        autoCompleteVehicle.setOnItemClickListener { _, _, position, _ ->
            val selectedName = adapter.getItem(position).toString()
            selectedVehicle = VehicleRepository.getVehicleByName(selectedName)
            updateRangeCalculation()
        }

        autoCompleteVehicle.setOnClickListener {
            autoCompleteVehicle.showDropDown()
        }
    }

    private fun setupSeekBar() {
        seekBarBattery.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvBatteryPercentage.text = "$progress%"
                updateRangeCalculation()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateRangeCalculation() {
        if (selectedVehicle != null) {
            val batteryPercent = seekBarBattery.progress
            val estimatedRange = (batteryPercent.toDouble() / 100.0) * selectedVehicle!!.maxRangeKm
            tvRangeResult.text = "${estimatedRange.toInt()} km"
        }
    }
}