package com.yesh.helaev

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location // Imported for distance math
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
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
        val homeLocation = LatLng(6.7106, 79.9074) // Our "Simulation" Location (Panadura)

        // --- 1. SMART ALGORITHM: FIND THE BEST STATION ---
        var bestStationId: String? = null

        if (userMode == "DRIVER") {
            // Step A: Filter stations within Range
            val reachableStations = stations.filter { station ->
                val distKm = calculateDistanceKm(homeLocation, station.location)
                distKm <= calculatedRange
            }

            // Step B: Sort by Busyness (Free > Medium > Busy)
            // We give "Score": Free=1, Medium=2, Busy=3. Lowest score wins.
            val bestStation = reachableStations.minByOrNull { station ->
                val status = StationRepository.getStatus(this, station.id)
                when (status) {
                    "Free" -> 1
                    "Medium" -> 2
                    else -> 3
                }
            }

            bestStationId = bestStation?.id
        }
        // ------------------------------------------------

        for (station in stations) {
            val currentStatus = StationRepository.getStatus(this, station.id)
            val reporterList = StationRepository.getReporterNames(this, station.id)

            // Standard snippet
            var snippetText = "Status: $currentStatus (Reports: $reporterList)"
            var markerTitle = station.title
            var markerColor = BitmapDescriptorFactory.HUE_AZURE // Default Blue

            // --- 2. COLOR LOGIC ---
            if (station.id == bestStationId) {
                // WINNER! This is the smart recommendation
                markerColor = BitmapDescriptorFactory.HUE_VIOLET // Purple stands out!
                markerTitle = "★ BEST OPTION: ${station.title}"
                snippetText = "Recommended: $currentStatus & In Range"
            }
            else {
                // Standard Traffic Coloring
                if (currentStatus == "Busy") {
                    markerColor = BitmapDescriptorFactory.HUE_RED
                } else if (currentStatus == "Medium") {
                    markerColor = BitmapDescriptorFactory.HUE_ORANGE
                } else {
                    markerColor = BitmapDescriptorFactory.HUE_AZURE // Free
                }
            }

            val markerOptions = MarkerOptions()
                .position(station.location)
                .title(markerTitle)
                .snippet(snippetText)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

            // Ensure the "Best" marker sits on top of others
            if (station.id == bestStationId) {
                markerOptions.zIndex(1.0f)
            }

            val marker = mMap.addMarker(markerOptions)
            marker?.tag = station.id

            // Auto-open the winner so the user sees it immediately
            if (station.id == bestStationId) {
                marker?.showInfoWindow()
                Toast.makeText(this, "We found the best station for you: ${station.title}", Toast.LENGTH_LONG).show()
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
        }

        mMap.setOnMarkerClickListener { marker ->
            showStationDetails(marker)
            true
        }
    }

    // --- HELPER: MATH TO CALCULATE DISTANCE ---
    private fun calculateDistanceKm(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0] / 1000 // Convert meters to KM
    }
    // ------------------------------------------

    private fun showStationDetails(marker: Marker) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_rate_station)

        val tvStationName = dialog.findViewById<TextView>(R.id.tvStationName)
        val tvStatus = dialog.findViewById<TextView>(R.id.tvStationStatus)
        val btnNavigate = dialog.findViewById<Button>(R.id.btnNavigate)
        val btnSubmit = dialog.findViewById<Button>(R.id.btnSubmitRating)
        val btnViewReporters = dialog.findViewById<Button>(R.id.btnViewReporters)
        val rgBusyness = dialog.findViewById<RadioGroup>(R.id.rgBusyness)

        val stationId = marker.tag as? String ?: return

        tvStationName.text = marker.title
        val count = StationRepository.getReporterNames(this, stationId).split(",").filter { it.isNotBlank() }.size
        tvStatus.text = "${marker.snippet} \n($count total reports)"

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

                // Update marker snippet logic so it doesn't lose the "Best" title if it was best
                // (For simplicity, we just update status here)
                marker.snippet = "Status: $exactStatus"

                // Update Icon Color based on new status
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

    private fun showLeaderboardDialog(stationId: String) {
        val listDialog = Dialog(this)
        listDialog.setContentView(R.layout.dialog_reporters_list)

        val container = listDialog.findViewById<LinearLayout>(R.id.llReportersContainer)
        val btnClose = listDialog.findViewById<Button>(R.id.btnCloseList)

        val rawNames = StationRepository.getReporterNames(this, stationId)

        if (rawNames == "None" || rawNames.isEmpty()) {
            val tvEmpty = TextView(this)
            tvEmpty.text = "No reports yet."
            container.addView(tvEmpty)
        } else {
            val namesArray = rawNames.split(", ")

            for (rawName in namesArray) {
                if (rawName.isBlank()) continue

                val isVerified = rawName.contains("_VERIFIED")
                val cleanName = rawName.replace("_VERIFIED", "")

                // --- CREATE ROW ---
                val row = LinearLayout(this)
                row.orientation = LinearLayout.HORIZONTAL
                row.setPadding(0, 16, 0, 16)
                row.gravity = Gravity.CENTER_VERTICAL

                // 1. NAME + BADGE
                val tvName = TextView(this)
                val badge = StationRepository.getUserBadge(this, cleanName)
                if (badge.isNotEmpty()) {
                    tvName.text = "$cleanName  ($badge)"
                } else {
                    tvName.text = cleanName
                }

                tvName.textSize = 16f
                tvName.setTextColor(Color.BLACK)
                tvName.layoutParams = LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                )

                row.addView(tvName)

                // 2. BADGE or BUTTON
                if (isVerified) {
                    val tvBadge = TextView(this)
                    tvBadge.text = "✅ Approved"
                    tvBadge.setTextColor(Color.parseColor("#4CAF50"))
                    tvBadge.textSize = 12f
                    tvBadge.typeface = android.graphics.Typeface.DEFAULT_BOLD
                    row.addView(tvBadge)
                } else {
                    val btnVerify = Button(this)
                    btnVerify.text = "Verify"
                    btnVerify.textSize = 10f
                    val params = LinearLayout.LayoutParams(180, 100)
                    btnVerify.layoutParams = params

                    btnVerify.setOnClickListener {
                        StationRepository.verifyUserReport(this, stationId, cleanName)
                        StationRepository.incrementUserScore(this, cleanName) // Score +1
                        Toast.makeText(this, "Verified! $cleanName gained +1 Reputation", Toast.LENGTH_SHORT).show()
                        listDialog.dismiss()
                        showLeaderboardDialog(stationId)
                    }
                    row.addView(btnVerify)
                }

                container.addView(row)

                val divider = View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 2
                )
                divider.setBackgroundColor(Color.LTGRAY)
                container.addView(divider)
            }
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