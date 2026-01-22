package com.yesh.helaev

import android.content.Context
import com.google.android.gms.maps.model.LatLng

data class ChargingStation(
    val id: String,
    val title: String,
    val location: LatLng
)

object StationRepository {

    // Our fixed list of stations
    val stations = listOf(
        ChargingStation("1", "Colombo City Centre ChargeNet", LatLng(6.9271, 79.8612)),
        ChargingStation("2", "Mount Lavinia Hotel Station", LatLng(6.8404, 79.8712)),
        ChargingStation("3", "Panadura Public Charger", LatLng(6.7106, 79.9074)),
        ChargingStation("4", "One Galle Face Mall", LatLng(6.9030, 79.8530)),
        ChargingStation("5", "Moratuwa University Point", LatLng(6.7969, 79.8885))
    )

    // Save Status (Reporter Mode)
    fun saveStationStatus(context: Context, stationId: String, status: String) {
        val prefs = context.getSharedPreferences("HelaEV_Data", Context.MODE_PRIVATE)
        prefs.edit().putString(stationId, status).apply()
    }

    // Load Status (Driver Mode)
    fun getStationStatus(context: Context, stationId: String): String {
        val prefs = context.getSharedPreferences("HelaEV_Data", Context.MODE_PRIVATE)
        return prefs.getString(stationId, "Free") ?: "Free"
    }
}