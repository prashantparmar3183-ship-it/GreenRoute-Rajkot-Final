package com.example.data

object SampleRajkotData {
    // Depot: Government Polytechnic Rajkot / RMC East Zone Depot
    const val DEPOT_NAME = "RMC Depot (Gov. Polytechnic Rajkot)"
    const val DEPOT_LAT = 22.2736
    const val DEPOT_LON = 70.8222

    fun getInitialGarbagePoints(): List<GarbagePointEntity> {
        // Deliberately ordered sub-optimally across East/West/South/North Rajkot so Before vs After optimization shows real distance & time savings
        return listOf(
            GarbagePointEntity(
                name = "Gov. Polytechnic Campus Gate",
                address = "Bhavnagar Road, Aji Dam Chowkdi, Rajkot",
                latitude = 22.2748,
                longitude = 70.8235,
                wasteType = "Mixed Municipal",
                estimatedWasteKg = 420,
                binCapacityKg = 500,
                fillPercentage = 84,
                priorityScore = 82,
                riskLevel = "High",
                status = "Pending",
                notes = "Main institutional smart bin cluster near main gate",
                routeOrder = 1
            ),
            GarbagePointEntity(
                name = "University Road Hostel Zone",
                address = "Saurashtra University Rd, West Rajkot",
                latitude = 22.2918,
                longitude = 70.7532,
                wasteType = "Organic / Wet",
                estimatedWasteKg = 480,
                binCapacityKg = 500,
                fillPercentage = 96,
                priorityScore = 95,
                riskLevel = "Critical",
                status = "Pending",
                notes = "High canteen & hostel food waste overflow alert",
                routeOrder = 2
            ),
            GarbagePointEntity(
                name = "Kothariya Road Market Bin",
                address = "Kothariya Main Rd, Near Hudko Police Chowki",
                latitude = 22.2562,
                longitude = 70.8145,
                wasteType = "Commercial / Bulk",
                estimatedWasteKg = 580,
                binCapacityKg = 600,
                fillPercentage = 97,
                priorityScore = 96,
                riskLevel = "Critical",
                status = "Pending",
                notes = "Dense market zone, immediate morning pickup required",
                routeOrder = 3
            ),
            GarbagePointEntity(
                name = "Raiya Road Circle Point",
                address = "Raiya Chowkdi, 150 Feet Ring Road, Rajkot",
                latitude = 22.3085,
                longitude = 70.7684,
                wasteType = "Recyclable Dry",
                estimatedWasteKg = 310,
                binCapacityKg = 450,
                fillPercentage = 69,
                priorityScore = 65,
                riskLevel = "Medium",
                status = "Scheduled",
                notes = "Segregated commercial packaging & dry waste",
                routeOrder = 4
            ),
            GarbagePointEntity(
                name = "Marketing Yard APMC Gate 2",
                address = "Old Marketing Yard, Bedipara, Rajkot",
                latitude = 22.3124,
                longitude = 70.8258,
                wasteType = "Organic / Wet",
                estimatedWasteKg = 680,
                binCapacityKg = 700,
                fillPercentage = 97,
                priorityScore = 98,
                riskLevel = "Critical",
                status = "Pending",
                notes = "Heavy vegetable market biodegradable load",
                routeOrder = 5
            ),
            GarbagePointEntity(
                name = "Mavdi Chowkdi Collection Hub",
                address = "Mavdi Main Road, 150 Ft Ring Rd South, Rajkot",
                latitude = 22.2621,
                longitude = 70.7785,
                wasteType = "Mixed Municipal",
                estimatedWasteKg = 390,
                binCapacityKg = 500,
                fillPercentage = 78,
                priorityScore = 76,
                riskLevel = "High",
                status = "Pending",
                notes = "Residential apartments secondary transfer bin",
                routeOrder = 6
            ),
            GarbagePointEntity(
                name = "Aji Riverfront Promenade",
                address = "Near Aji River Bridge, Kaiser-e-Hind, Rajkot",
                latitude = 22.2965,
                longitude = 70.8112,
                wasteType = "Recyclable Dry",
                estimatedWasteKg = 240,
                binCapacityKg = 400,
                fillPercentage = 60,
                priorityScore = 58,
                riskLevel = "Medium",
                status = "Scheduled",
                notes = "Public park & riverfront plastic/dry bins",
                routeOrder = 7
            ),
            GarbagePointEntity(
                name = "Kalawad Road Crystal Point",
                address = "Kalawad Road, Near Kotecha Chowk, Rajkot",
                latitude = 22.2842,
                longitude = 70.7654,
                wasteType = "Mixed Municipal",
                estimatedWasteKg = 410,
                binCapacityKg = 500,
                fillPercentage = 82,
                priorityScore = 80,
                riskLevel = "High",
                status = "Pending",
                notes = "High-traffic commercial corridor pickup",
                routeOrder = 8
            ),
            GarbagePointEntity(
                name = "Bhaktinagar Circle Station",
                address = "Bhaktinagar Station Plot, Gondal Rd Link, Rajkot",
                latitude = 22.2788,
                longitude = 70.7995,
                wasteType = "Mixed Municipal",
                estimatedWasteKg = 340,
                binCapacityKg = 500,
                fillPercentage = 68,
                priorityScore = 45,
                riskLevel = "Low",
                status = "Collected",
                notes = "Cleared during early morning shift",
                routeOrder = 0
            ),
            GarbagePointEntity(
                name = "Gondal Road GIDC Phase 1",
                address = "Gondal Road, Near Malaviya Chowk, Rajkot",
                latitude = 22.2598,
                longitude = 70.7982,
                wasteType = "Commercial / Bulk",
                estimatedWasteKg = 290,
                binCapacityKg = 500,
                fillPercentage = 58,
                priorityScore = 42,
                riskLevel = "Low",
                status = "Collected",
                notes = "Industrial estate dry non-hazardous collection completed",
                routeOrder = 0
            )
        )
    }

    fun getInitialVehicles(): List<VehicleEntity> {
        return listOf(
            VehicleEntity(
                name = "RMC EcoTipper-01",
                registrationNumber = "GJ-03-GA-4821",
                fuelType = "CNG",
                payloadCapacityKg = 4500,
                currentLoadKg = 630,
                driverName = "Rameshbhai Jadeja",
                driverContact = "+91 98250 74120",
                isActive = true,
                isAssignedToRoute = true
            ),
            VehicleEntity(
                name = "RMC VoltLoader-02",
                registrationNumber = "GJ-03-EV-9104",
                fuelType = "Electric EV",
                payloadCapacityKg = 2800,
                currentLoadKg = 0,
                driverName = "Kishorbhai Parmar",
                driverContact = "+91 94262 31890",
                isActive = true,
                isAssignedToRoute = false
            ),
            VehicleEntity(
                name = "RMC Compactor-03",
                registrationNumber = "GJ-03-MW-6512",
                fuelType = "Diesel",
                payloadCapacityKg = 6500,
                currentLoadKg = 0,
                driverName = "Hareshbhai Gohel",
                driverContact = "+91 99784 55210",
                isActive = true,
                isAssignedToRoute = false
            )
        )
    }

    fun getInitialLogs(now: Long = System.currentTimeMillis()): List<CollectionLogEntity> {
        return listOf(
            CollectionLogEntity(
                locationId = 9,
                locationName = "Bhaktinagar Circle Station",
                status = "Collected",
                wasteAmountKg = 340,
                timestamp = now - 75 * 60 * 1000L, // 75 mins ago
                assignedVehicle = "RMC EcoTipper-01 (GJ-03-GA-4821)"
            ),
            CollectionLogEntity(
                locationId = 10,
                locationName = "Gondal Road GIDC Phase 1",
                status = "Collected",
                wasteAmountKg = 290,
                timestamp = now - 110 * 60 * 1000L, // 110 mins ago
                assignedVehicle = "RMC EcoTipper-01 (GJ-03-GA-4821)"
            )
        )
    }
}
