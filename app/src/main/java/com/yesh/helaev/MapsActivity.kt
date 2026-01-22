package com.yesh.helaev

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var calculatedRange: Int = 0
    private var userMode: String = "DRIVER"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        calculatedRange = intent.getIntExtra("RANGE_RESULT", 0)
        userMode = intent.getStringExtra("USER_MODE") ?: "DRIVER"

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Find the button
        val btnBack = findViewById<Button>(R.id.btnBackHome)

// Handle Click
        btnBack.setOnClickListener {
            // Option A: Just close this screen (Goes back to previous)
            finish()

            // Option B (Safer): Force restart Main Activity
            // val intent = Intent(this, MainActivity::class.java)
            // intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            // startActivity(intent)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()

        // 1. Load Stations
        val stations = StationRepository.stations
        var recommendedMarker: Marker? = null

        // Define our "Home Base" for the demo (Panadura)
        val homeLocation = LatLng(6.7106, 79.9074)

        for (station in stations) {
            val currentStatus = StationRepository.getStationStatus(this, station.id)
            val isPanadura = (station.title == "Panadura Public Charger")

            val markerOptions = MarkerOptions()
                .position(station.location)
                .title(station.title)
                .snippet("Status: $currentStatus")

            // LOGIC: Set Pin Colors
            if (currentStatus == "Busy") {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            } else if (currentStatus == "Medium") {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            } else if (isPanadura && userMode == "DRIVER") {
                markerOptions
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet("★ Recommended (Free & In Range)")
                    .zIndex(1.0f)
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            }

            val marker = mMap.addMarker(markerOptions)
            marker?.tag = station.id

            if (isPanadura && currentStatus == "Free" && userMode == "DRIVER") {
                recommendedMarker = marker
            }
        }

        // 2. Auto-Zoom to Home
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLocation, 10f)) // Zoomed out a bit to see the circle

        // 3. DRAW RANGE CIRCLE (Visual Geofencing)
        if (userMode == "DRIVER" && calculatedRange > 0) {
            // Convert km to meters
            val radiusInMeters = calculatedRange * 1000.0

            mMap.addCircle(
                CircleOptions()
                    .center(homeLocation) // Center on Panadura (Our Simulation Start)
                    .radius(radiusInMeters) // The Range from Screen 1
                    .strokeWidth(3f)
                    .strokeColor(Color.BLUE) // The outline color
                    .fillColor(0x220000FF) // Semi-transparent Blue (The "0x22" makes it see-through)
            )

            Toast.makeText(this, "Blue Circle shows your $calculatedRange km range", Toast.LENGTH_LONG).show()
        }

        // Show Recommendation
        if (recommendedMarker != null) {
            recommendedMarker.showInfoWindow()
        }

        mMap.setOnMarkerClickListener { marker ->
            showStationDetails(marker)
            true
        }
    }

    private fun showStationDetails(marker: Marker) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_rate_station)

        val tvStationName = dialog.findViewById<TextView>(R.id.tvStationName)
        val tvStatus = dialog.findViewById<TextView>(R.id.tvStationStatus)
        val btnNavigate = dialog.findViewById<Button>(R.id.btnNavigate)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitRating)
        val rgBusyness = dialog.findViewById<RadioGroup>(R.id.rgBusyness)

        val stationId = marker.tag as? String ?: return

        tvStationName.text = marker.title
        tvStatus.text = marker.snippet

        btnNavigate.setOnClickListener {
            val uri = Uri.parse("google.navigation:q=${marker.position.latitude},${marker.position.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")
            try { startActivity(intent) } catch (e: Exception) { }
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val selectedId = rgBusyness.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRb = dialog.findViewById<RadioButton>(selectedId)
                val statusText = selectedRb.text.toString()

                val exactStatus = when {
                    statusText.contains("High") -> "Busy"
                    statusText.contains("Medium") -> "Medium"
                    else -> "Free"
                }

                StationRepository.saveStationStatus(this, stationId, exactStatus)

                marker.snippet = "Status: $exactStatus"

                when (exactStatus) {
                    "Busy" -> marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    "Medium" -> marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    "Free" -> marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                }

                marker.showInfoWindow()
                Toast.makeText(this, "Status saved: $exactStatus", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation()
        }
    }
}