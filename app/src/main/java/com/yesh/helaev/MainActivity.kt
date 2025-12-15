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

        setupVehicleSelector()
        setupSeekBar()

        btnDirectMe.setOnClickListener {
            val rangeText = tvRangeResult.text.toString().filter { it.isDigit() }

            if (rangeText.isNotEmpty() && rangeText.toInt() > 0) {
                val rangeValue = rangeText.toInt()
                Toast.makeText(this, "Calculating route for $rangeValue km...", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LoadingActivity::class.java)
                intent.putExtra("RANGE_RESULT", rangeValue)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a vehicle first!", Toast.LENGTH_SHORT).show()
            }
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