package com.yesh.helaev

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var calculatedRange: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Receive the range from the previous screen
        calculatedRange = intent.getIntExtra("RANGE_RESULT", 0)

        // Set up the map fragment
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Example: Add a marker in Colombo, Sri Lanka
        val colombo = LatLng(6.9271, 79.8612)
        mMap.addMarker(MarkerOptions().position(colombo).title("Marker in Colombo"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombo, 12f))
    }
}