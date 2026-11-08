package com.example.model

data class VehicleProfile(
    val id: String,
    val brand: String,
    val model: String,
    val version: String,
    val year: String,
    val engine: String,
    val displacement: String,
    val fuelType: String,
    val horsepower: String,
    val torque: String,
    val transmission: String,
    val drivetrain: String,
    val tankCapacity: String,
    val isHybrid: Boolean = false,
    val notes: String = ""
) {
  val fullName: String
    get() = "$brand $model $version ($year)"
}

object PreloadedVehicles {
  val corollaAltis2025 = VehicleProfile(
      id = "corolla_altis_2025",
      brand = "Toyota",
      model = "Corolla",
      version = "Altis Premium",
      year = "2025",
      engine = "2.0L Dynamic Force Dual VVT-iE",
      displacement = "1.987 cm³",
      fuelType = "Flex (Etanol / Gasolina)",
      horsepower = "175 cv (Etanol) / 169 cv (Gasolina)",
      torque = "21,3 kgfm a 4.800 rpm",
      transmission = "Direct Shift CVT (10 marchas simuladas)",
      drivetrain = "Tração Dianteira (FWD)",
      tankCapacity = "50 litros",
      isHybrid = false,
      notes = "Motor de alta eficiência com injeção direta e port-injection (D-4S)."
  )

  val corollaHybrid2025 = VehicleProfile(
      id = "corolla_hybrid_2025",
      brand = "Toyota",
      model = "Corolla",
      version = "Altis Hybrid Premium",
      year = "2025",
      engine = "1.8L VVT-i 16V Flex + Motor Elétrico",
      displacement = "1.798 cm³",
      fuelType = "Flex + Sistema Elétrico Híbrido",
      horsepower = "122 cv (Potência Combinada)",
      torque = "14,5 kgfm (Combustível) + Elétrico",
      transmission = "Hybrid Transaxle CVT",
      drivetrain = "Tração Dianteira (FWD)",
      tankCapacity = "43 litros",
      isHybrid = true,
      notes = "Sistema híbrido auto-recarregável com monitoramento de bateria HV e inversor."
  )

  val yaris2025 = VehicleProfile(
      id = "yaris_xl_plus_2025",
      brand = "Toyota",
      model = "Yaris",
      version = "XL Plus",
      year = "2025",
      engine = "1.5L Dual VVT-i Flex (2NR-FBE)",
      displacement = "1.496 cm³",
      fuelType = "Flex (Etanol / Gasolina)",
      horsepower = "110 cv (Etanol) / 105 cv (Gasolina)",
      torque = "14,9 kgfm (Etanol) / 14,3 kgfm (Gasolina)",
      transmission = "Multidrive CVT (7 marchas simuladas)",
      drivetrain = "Tração Dianteira (FWD)",
      tankCapacity = "45 litros",
      isHybrid = false,
      notes = "Hatch compacto ágil e econômico com comando variável inteligente."
  )

  val all = listOf(corollaAltis2025, corollaHybrid2025, yaris2025)
}

data class TelemetryState(
    val rpm: Int = 0,
    val speed: Float = 0f,
    val coolantTemp: Int = 0,
    val intakeTemp: Int = 0,
    val batteryVoltage: Float = 0f,
    val instantConsumption: Float = 0f, // km/L
    val averageConsumption: Float = 0f, // km/L
    val engineLoad: Int = 0, // %
    val manifoldPressure: Float = 0f, // kPa
    val throttlePosition: Int = 0, // %
    val fuelPressure: Float = 0f, // kPa
    val ethanolPercentage: Int = 0, // %
    val fuelTypeLabel: String = "—",
    val hybridBatterySoc: Int = 0, // % (for hybrid)
    val hybridMotorPowerKw: Float = 0f, // kW
    val isConnected: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val isDemoMode: Boolean = false
)

enum class ConnectionState {
  DISCONNECTED,
  SEARCHING,
  CONNECTING,
  CONNECTED,
  ERROR
}

data class DiagnosticCode(
    val code: String,
    val title: String,
    val description: String,
    val system: String,
    val severity: DTCLevel
)

enum class DTCLevel {
  WARNING, CRITICAL, INFO
}

data class TripRecord(
    val id: String,
    val date: String,
    val vehicleName: String,
    val distanceKm: Float,
    val durationMinutes: Int,
    val avgSpeed: Float,
    val maxSpeed: Float,
    val fuelConsumedLiters: Float,
    val avgConsumption: Float
)
