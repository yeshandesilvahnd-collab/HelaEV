package com.yesh.helaev

import android.Manifest
import android.app.Dialog
import android.content.Context
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
    private var currentUserName: String = "Guest"
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        calculatedRange = intent.getIntExtra("RANGE_RESULT", 0)
        userMode = intent.getStringExtra("USER_MODE") ?: "DRIVER"

        // GET LOGGED IN USER NAME
        val prefs = getSharedPreferences("HelaEV_User", Context.MODE_PRIVATE)
        currentUserName = prefs.getString("CURRENT_USER", "Guest") ?: "Guest"

        val btnBack = findViewById<Button>(R.id.btnBackHome)
        btnBack.setOnClickListener { finish() }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()

        val stations = StationRepository.stations
        var recommendedMarker: Marker? = null
        val homeLocation = LatLng(6.7106, 79.9074)

        for (station in stations) {
            val currentStatus = StationRepository.getStatus(this, station.id)

            // FIXED 1: Use the new "getReporterNames" (List version)
            val reporterList = StationRepository.getReporterNames(this, station.id)

            val isPanadura = (station.title == "Panadura Public Charger")

            // FIXED 2: Updated text format to show the List
            val snippetText = "Status: $currentStatus (Reports: $reporterList)"

            val markerOptions = MarkerOptions()
                .position(station.location)
                .title(station.title)
                .snippet(snippetText)

            // Color Logic
            if (currentStatus == "Busy") {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            } else if (currentStatus == "Medium") {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            } else if (isPanadura && userMode == "DRIVER") {
                markerOptions
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet("★ Recommended ($currentStatus)")
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

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLocation, 10f))

        if (userMode == "DRIVER" && calculatedRange > 0) {
            val radiusInMeters = calculatedRange * 1000.0
            mMap.addCircle(
                CircleOptions()
                    .center(homeLocation)
                    .radius(radiusInMeters)
                    .strokeWidth(3f)
                    .strokeColor(Color.BLUE)
                    .fillColor(0x220000FF)
            )
            Toast.makeText(this, "Logged in as: $currentUserName", Toast.LENGTH_SHORT).show()
        }

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
        val btnViewReporters = dialog.findViewById<Button>(R.id.btnViewReporters) // NEW BUTTON
        val rgBusyness = dialog.findViewById<RadioGroup>(R.id.rgBusyness)

        val stationId = marker.tag as? String ?: return

        tvStationName.text = marker.title

        // Show simplified status on the main dialog
        val count = StationRepository.getReporterNames(this, stationId).split(",").filter { it.isNotBlank() }.size
        tvStatus.text = "${marker.snippet} \n($count total reports)"

        // --- NEW: OPEN LEADERBOARD ---
        btnViewReporters.setOnClickListener {
            showLeaderboardDialog(stationId)
        }

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

                StationRepository.saveStationUpdate(this, stationId, exactStatus, currentUserName)

                val updatedList = StationRepository.getReporterNames(this, stationId)
                marker.snippet = "Status: $exactStatus" // Keep map pin clean

                when (exactStatus) {
                    "Busy" -> marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    "Medium" -> marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    "Free" -> marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                }
                marker.showInfoWindow()
                Toast.makeText(this, "Report added!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // --- NEW FUNCTION: Show the Vertical List ---
    private fun showLeaderboardDialog(stationId: String) {
        val listDialog = Dialog(this)
        listDialog.setContentView(R.layout.dialog_reporters_list)

        val tvList = listDialog.findViewById<TextView>(R.id.tvReportersList)
        val btnClose = listDialog.findViewById<Button>(R.id.btnCloseList)

        // 1. Get raw string "Nimal, Kamal"
        val rawNames = StationRepository.getReporterNames(this, stationId)

        // 2. Format it nicely
        if (rawNames == "None" || rawNames.isEmpty()) {
            tvList.text = "No reports yet."
        } else {
            // Split by comma and make a vertical list
            val namesArray = rawNames.split(", ")
            val formattedString = StringBuilder()

            for ((index, name) in namesArray.withIndex()) {
                formattedString.append("${index + 1}. $name\n") // "1. Nimal"
            }
            tvList.text = formattedString.toString()
        }

        btnClose.setOnClickListener {
            listDialog.dismiss()
        }

        listDialog.show()
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