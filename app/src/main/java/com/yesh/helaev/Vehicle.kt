package com.yesh.helaev

// 1. The Blueprint: This defines what a "Vehicle" is in our app
data class Vehicle(
    val name: String,
    val maxRangeKm: Int, // The range when battery is 100%
    val batteryCapacityKwh: Double
)

// 2. The Database: This holds our list of cars
object VehicleRepository {
    val vehicleList = listOf(
        Vehicle("Tesla Model 3", 491, 60.0),
        Vehicle("BYD Atto 3", 420, 60.5),
        Vehicle("BYD Seal", 570, 82.5),
        Vehicle("Nissan Leaf", 270, 40.0),
        Vehicle("MG ZS EV", 320, 50.3),
        Vehicle("Hyundai Kona", 484, 64.0),
        Vehicle("Kia Niro EV", 460, 64.8),
        Vehicle("BMW iX3", 460, 80.0),
        Vehicle("Audi e-tron GT", 487, 93.4),
        Vehicle("Jaguar I-PACE", 470, 90.0)
    )

    // A helper function to find a vehicle by its name (we will use this later)
    fun getVehicleByName(name: String): Vehicle? {
        return vehicleList.find { it.name == name }
    }
}