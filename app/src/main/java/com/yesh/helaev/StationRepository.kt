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

    // --- NEW: VERIFY A USER ---
    fun verifyUserReport(context: Context, stationId: String, userToVerify: String) {
        val prefs = context.getSharedPreferences("HelaEV_Data", Context.MODE_PRIVATE)

        // 1. Get the current list (e.g. "Yeshan, Kamal")
        var currentListString = prefs.getString(stationId + "_REPORTERS", "") ?: ""

        // 2. Find "Yeshan" and replace with "Yeshan_VERIFIED"
        // (We use a special tag "_VERIFIED" to track it invisibly)
        if (currentListString.contains(userToVerify) && !currentListString.contains(userToVerify + "_VERIFIED")) {
            currentListString = currentListString.replace(userToVerify, userToVerify + "_VERIFIED")

            // 3. Save it back
            prefs.edit().putString(stationId + "_REPORTERS", currentListString).apply()
        }
    }

    // 1. INCREMENT SCORE (Run this when someone clicks "Verify")
    fun incrementUserScore(context: Context, username: String) {
        val prefs = context.getSharedPreferences("HelaEV_Scores", Context.MODE_PRIVATE)
        val currentScore = prefs.getInt(username, 0)
        prefs.edit().putInt(username, currentScore + 1).apply()
    }

    // 2. GET BADGE (Run this when showing the list)
    fun getUserBadge(context: Context, username: String): String {
        val prefs = context.getSharedPreferences("HelaEV_Scores", Context.MODE_PRIVATE)
        val score = prefs.getInt(username, 0) // Default is 0

        return when {
            score >= 100 -> "👑 Tesla Master"
            score >= 50 -> "⚡ Amp Expert"
            score >= 1 -> "🔋 Volt Scout"
            else -> "" // No badge yet
        }
    }
}