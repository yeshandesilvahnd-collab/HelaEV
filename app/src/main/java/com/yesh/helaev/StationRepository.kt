package com.yesh.helaev

import android.content.Context
import com.google.android.gms.maps.model.LatLng

data class ChargingStation(
    val id: String,
    val title: String,
    val location: LatLng
)

object StationRepository {

    val stations = listOf(
        ChargingStation("1", "Colombo City Centre ChargeNet", LatLng(6.9271, 79.8612)),
        ChargingStation("2", "Mount Lavinia Hotel Station", LatLng(6.8404, 79.8712)),
        ChargingStation("3", "Panadura Public Charger", LatLng(6.7106, 79.9074)),
        ChargingStation("4", "One Galle Face Mall", LatLng(6.9030, 79.8530)),
        ChargingStation("5", "Moratuwa University Point", LatLng(6.7969, 79.8885))
    )

    // --- NEW LOGIC: APPEND TO LIST ---
    fun saveStationUpdate(context: Context, stationId: String, status: String, username: String) {
        val prefs = context.getSharedPreferences("HelaEV_Data", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // 1. Save the new Status (Busy/Free)
        editor.putString(stationId + "_STATUS", status)

        // 2. Get the OLD list of reporters (e.g., "Kamal")
        val currentReporters = prefs.getString(stationId + "_REPORTERS", "") ?: ""

        // 3. Create NEW list
        // Logic: If the name is not already in the list, add it.
        val newReportersList = if (currentReporters.isEmpty()) {
            username
        } else if (!currentReporters.contains(username)) {
            "$currentReporters, $username" // Append: "Kamal, Yeshan"
        } else {
            currentReporters // He already voted, don't duplicate name
        }

        // 4. Save the List
        editor.putString(stationId + "_REPORTERS", newReportersList)
        editor.apply()
    }

    fun getStatus(context: Context, stationId: String): String {
        val prefs = context.getSharedPreferences("HelaEV_Data", Context.MODE_PRIVATE)
        return prefs.getString(stationId + "_STATUS", "Free") ?: "Free"
    }

    // NEW: Get the List of Names
    fun getReporterNames(context: Context, stationId: String): String {
        val prefs = context.getSharedPreferences("HelaEV_Data", Context.MODE_PRIVATE)
        return prefs.getString(stationId + "_REPORTERS", "None") ?: "None"
    }
}