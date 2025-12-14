package com.yesh.helaev

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // 1. CHANGED: We use AutoCompleteTextView instead of Spinner
    private lateinit var autoCompleteVehicle: AutoCompleteTextView
    private lateinit var tvBatteryPercentage: TextView
    private lateinit var seekBarBattery: SeekBar
    private lateinit var tvRangeResult: TextView
    private lateinit var btnDirectMe: Button

    private var selectedVehicle: Vehicle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 2. Connect the new ID
        autoCompleteVehicle = findViewById(R.id.autoCompleteVehicle)
        tvBatteryPercentage = findViewById(R.id.tvBatteryPercentage)
        seekBarBattery = findViewById(R.id.seekBarBattery)
        tvRangeResult = findViewById(R.id.tvRangeResult)
        btnDirectMe = findViewById(R.id.btnDirectMe)

        setupVehicleSelector() // Updated function name
        setupSeekBar()

        btnDirectMe.setOnClickListener {
            // Navigation logic will go here later
        }
    }

    private fun setupVehicleSelector() {
        val vehicleNames = VehicleRepository.vehicleList.map { it.name }

        // Create the adapter for filtering
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicleNames)
        autoCompleteVehicle.setAdapter(adapter)

        // LOGIC: When the user clicks a suggestion from the list
        autoCompleteVehicle.setOnItemClickListener { _, _, position, _ ->
            // The position here refers to the FILTERED list, not the original list.
            // So we must get the text they clicked, then find the car by name.
            val selectedName = adapter.getItem(position).toString()
            selectedVehicle = VehicleRepository.getVehicleByName(selectedName)

            updateRangeCalculation()
        }

        // OPTIONAL: Open the list as soon as they click the box (even before typing)
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