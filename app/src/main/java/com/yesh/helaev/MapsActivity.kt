package com.yesh.helaev

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var calculatedRange: Int = 0
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        calculatedRange = intent.getIntExtra("RANGE_RESULT", 0)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()

        // 1. Define Stations
        val stations = listOf(
            LatLng(6.9271, 79.8612) to "Colombo City Centre ChargeNet",
            LatLng(6.8404, 79.8712) to "Mount Lavinia Hotel Station",
            LatLng(6.7106, 79.9074) to "Panadura Public Charger", // This is our target
            LatLng(6.9030, 79.8530) to "One Galle Face Mall",
            LatLng(6.7969, 79.8885) to "Moratuwa University Point"
        )

        var recommendedMarker: Marker? = null

        // 2. Loop through and Add Markers with Logic
        for ((location, title) in stations) {

            // Check if this is the "Best" station (Panadura)
            val isMostConvenient = (title == "Panadura Public Charger")

            val markerOptions = MarkerOptions()
                .position(location)
                .title(title)

            if (isMostConvenient) {
                // HIGHLIGHT LOGIC: Make it GREEN and add a Star
                markerOptions
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .snippet("★ Best Choice (In Range)")
                    .zIndex(1.0f) // Put this marker on top of others
            } else {
                // NORMAL LOGIC: Make others RED
                markerOptions
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .snippet("Status: Unknown")
            }

            val marker = mMap.addMarker(markerOptions)

            if (isMostConvenient) {
                recommendedMarker = marker
            }
        }

        // 3. Zoom specifically to the Green Marker
        val targetLocation = LatLng(6.7106, 79.9074)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 13f))

        // 4. Auto-open the Green Marker's popup
        recommendedMarker?.showInfoWindow()

        mMap.setOnMarkerClickListener { marker ->
            showStationDetails(marker)
            true
        }
    }

    private fun enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            }
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

        tvStationName.text = marker.title
        tvStatus.text = marker.snippet

        btnNavigate.setOnClickListener {
            val intentUri = Uri.parse("google.navigation:q=${marker.position.latitude},${marker.position.longitude}")
            val mapIntent = Intent(Intent.ACTION_VIEW, intentUri)
            mapIntent.setPackage("com.google.android.apps.maps")
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?daddr=${marker.position.latitude},${marker.position.longitude}")))
            }
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val selectedId = rgBusyness.checkedRadioButtonId
            if (selectedId != -1) {
                val selectedRb = dialog.findViewById<RadioButton>(selectedId)
                val status = selectedRb.text.toString()
                marker.snippet = "Status: $status"
                tvStatus.text = "Status: $status"
                marker.showInfoWindow()
                Toast.makeText(this, "Crowd report submitted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please select a level", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
}