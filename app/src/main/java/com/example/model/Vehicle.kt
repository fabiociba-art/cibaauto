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
    get() = listOf(brand, model, version).filter { it.isNotBlank() }.joinToString(" ") + (if (year.isNotBlank()) " ($year)" else "")

  fun getDisplacementLiters(): Float {
    return try {
      val clean = displacement.replace("cm³", "").replace("cc", "").replace("L", "").replace("l", "").trim()
      val match = Regex("""\d+([.,]\d+)?""").find(clean)
      if (match != null) {
        val value = match.value.replace(',', '.').toFloatOrNull() ?: 1.987f
        if (value > 50f) value / 1000f else value
      } else 1.987f
    } catch (_: Exception) { 1.987f }
  }

  fun getTankCapacityLiters(): Float {
    return try {
      val match = Regex("""\d+([.,]\d+)?""").find(tankCapacity)
      if (match != null) match.value.replace(',', '.').toFloatOrNull() ?: 50f else 50f
    } catch (_: Exception) { 50f }
  }
}

fun VehicleProfile.toJsonObject(): org.json.JSONObject {
  val obj = org.json.JSONObject()
  obj.put("id", id)
  obj.put("brand", brand)
  obj.put("model", model)
  obj.put("version", version)
  obj.put("year", year)
  obj.put("engine", engine)
  obj.put("displacement", displacement)
  obj.put("fuelType", fuelType)
  obj.put("horsepower", horsepower)
  obj.put("torque", torque)
  obj.put("transmission", transmission)
  obj.put("drivetrain", drivetrain)
  obj.put("tankCapacity", tankCapacity)
  obj.put("isHybrid", isHybrid)
  obj.put("notes", notes)
  return obj
}

fun vehicleProfileFromJson(json: org.json.JSONObject): VehicleProfile {
  return VehicleProfile(
      id = json.optString("id", java.util.UUID.randomUUID().toString()),
      brand = json.optString("brand", ""),
      model = json.optString("model", ""),
      version = json.optString("version", ""),
      year = json.optString("year", ""),
      engine = json.optString("engine", ""),
      displacement = json.optString("displacement", "1.987 cm³"),
      fuelType = json.optString("fuelType", "Flex (Etanol / Gasolina)"),
      horsepower = json.optString("horsepower", ""),
      torque = json.optString("torque", ""),
      transmission = json.optString("transmission", ""),
      drivetrain = json.optString("drivetrain", "Tração Dianteira (FWD)"),
      tankCapacity = json.optString("tankCapacity", "50 litros"),
      isHybrid = json.optBoolean("isHybrid", false),
      notes = json.optString("notes", "")
  )
}

object PreloadedVehicles {

  // ========== TOYOTA ==========
  val corollaAltis2025 = VehicleProfile(
      id = "corolla_altis_2025", brand = "Toyota", model = "Corolla", version = "Altis Premium",
      year = "2025", engine = "2.0L Dynamic Force Dual VVT-iE", displacement = "1.987 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "175 cv (Etanol) / 169 cv (Gasolina)",
      torque = "21,3 kgfm", transmission = "Direct Shift CVT", drivetrain = "Tração Dianteira (FWD)",
      tankCapacity = "50 litros", isHybrid = false, notes = "Motor D-4S.")
  val corollaHybrid2025 = VehicleProfile(
      id = "corolla_hybrid_2025", brand = "Toyota", model = "Corolla", version = "Altis Hybrid",
      year = "2025", engine = "1.8L VVT-i Flex + Elétrico", displacement = "1.798 cm³",
      fuelType = "Flex + Híbrido", horsepower = "122 cv (Combinada)", torque = "14,5 kgfm + Elétrico",
      transmission = "e-CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "43 litros",
      isHybrid = true, notes = "Híbrido pleno flex.")
  val corollaGliHybrid2026 = VehicleProfile(
      id = "corolla_gli_hybrid_2026", brand = "Toyota", model = "Corolla", version = "GLi Hybrid",
      year = "2026", engine = "1.8L VVT-i Flex + Elétrico", displacement = "1.798 cm³",
      fuelType = "Flex + Híbrido", horsepower = "122 cv (Combinada)", torque = "14,5 kgfm + Elétrico",
      transmission = "e-CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "43 litros",
      isHybrid = true, notes = "Entrada do híbrido flex.")
  val corollaCrossHybrid2025 = VehicleProfile(
      id = "corolla_cross_hybrid_2025", brand = "Toyota", model = "Corolla Cross", version = "XRX Hybrid",
      year = "2025", engine = "1.8L VVT-i Flex + Elétrico", displacement = "1.798 cm³",
      fuelType = "Flex + Híbrido", horsepower = "122 cv (Combinada)", torque = "14,5 kgfm + Elétrico",
      transmission = "e-CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "43 litros",
      isHybrid = true, notes = "SUV híbrido flex.")
  val yarisCrossHybrid2026 = VehicleProfile(
      id = "yaris_cross_hybrid_2026", brand = "Toyota", model = "Yaris Cross", version = "XRE Hybrid",
      year = "2026", engine = "1.5L Dual VVT-i Flex + Elétrico", displacement = "1.490 cm³",
      fuelType = "Flex + Híbrido", horsepower = "111 cv (Combinada)", torque = "12,2 kgfm + Elétrico",
      transmission = "e-CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "36 litros",
      isHybrid = true, notes = "SUV compacto híbrido econômico.")
  val hilux2025 = VehicleProfile(
      id = "toyota_hilux_srv_2025", brand = "Toyota", model = "Hilux", version = "SRX 2.8",
      year = "2025", engine = "2.8 Turbo Diesel", displacement = "2.755 cm³",
      fuelType = "Diesel", horsepower = "204 cv", torque = "50 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "4x4", tankCapacity = "80 litros",
      isHybrid = false, notes = "Picape média referência de mercado.")

  // ========== FIAT ==========
  val strada2025 = VehicleProfile(
      id = "fiat_strada_volcano_2025", brand = "Fiat", model = "Strada", version = "Volcano 1.3",
      year = "2025", engine = "1.3 Firefly Flex", displacement = "1.332 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "109 cv (Etanol)", torque = "14,2 kgfm",
      transmission = "Manual 5 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "55 litros",
      isHybrid = false, notes = "Picape mais vendida do Brasil.")
  val argo2025 = VehicleProfile(
      id = "fiat_argo_precision_2025", brand = "Fiat", model = "Argo", version = "Precision 1.3",
      year = "2025", engine = "1.3 Firefly Flex", displacement = "1.332 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "109 cv (Etanol)", torque = "14,2 kgfm",
      transmission = "Automático CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "48 litros",
      isHybrid = false, notes = "Hatch compacto best-seller Fiat.")
  val pulse2025 = VehicleProfile(
      id = "fiat_pulse_impetus_2025", brand = "Fiat", model = "Pulse", version = "Impetus 1.0 Turbo",
      year = "2025", engine = "1.0 Turbo Flex T200", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "130 cv (Etanol)", torque = "20,4 kgfm",
      transmission = "Automático CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "47 litros",
      isHybrid = false, notes = "SUV compacto da Fiat.")
  val fastback2025 = VehicleProfile(
      id = "fiat_fastback_limited_2025", brand = "Fiat", model = "Fastback", version = "Limited 1.0 Turbo",
      year = "2025", engine = "1.0 Turbo Flex T200", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "130 cv (Etanol)", torque = "20,4 kgfm",
      transmission = "Automático CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "47 litros",
      isHybrid = false, notes = "SUV cupê Fiat.")
  val toro2025 = VehicleProfile(
      id = "fiat_toro_volcano_2025", brand = "Fiat", model = "Toro", version = "Volcano 2.0",
      year = "2025", engine = "2.0 Turbo Diesel", displacement = "1.956 cm³",
      fuelType = "Diesel", horsepower = "170 cv", torque = "38 kgfm",
      transmission = "Automático 9 marchas", drivetrain = "4x4", tankCapacity = "60 litros",
      isHybrid = false, notes = "Picape média-compacta Fiat.")
  val mobi2025 = VehicleProfile(
      id = "fiat_mobi_like_2025", brand = "Fiat", model = "Mobi", version = "Like 1.0",
      year = "2025", engine = "1.0 Fire Flex", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "75 cv (Etanol)", torque = "10,2 kgfm",
      transmission = "Manual 5 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "47 litros",
      isHybrid = false, notes = "Entry-level urbano.")

  // ========== VOLKSWAGEN ==========
  val poloTsi2025 = VehicleProfile(
      id = "vw_polo_highline_2025", brand = "Volkswagen", model = "Polo", version = "Highline 200 TSI",
      year = "2025", engine = "1.0 TSI Turbo Flex", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "128 cv (Etanol)", torque = "20,4 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "52 litros",
      isHybrid = false, notes = "Hatch premium VW.")
  val tera2026 = VehicleProfile(
      id = "vw_tera_comfort_2026", brand = "Volkswagen", model = "Tera", version = "Comfort 200 TSI",
      year = "2026", engine = "1.0 TSI Turbo Flex", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "116–128 cv", torque = "20,4 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "50 litros",
      isHybrid = false, notes = "Novo SUV compacto VW (ex-T-Cross entry).")
  val nivus2025 = VehicleProfile(
      id = "vw_nivus_highline_2025", brand = "Volkswagen", model = "Nivus", version = "Highline 200 TSI",
      year = "2025", engine = "1.0 TSI Turbo Flex", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "128 cv (Etanol)", torque = "20,4 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "52 litros",
      isHybrid = false, notes = "SUV cupê compacto.")
  val tcross2025 = VehicleProfile(
      id = "vw_tcross_highline_2025", brand = "Volkswagen", model = "T-Cross", version = "Highline 250 TSI",
      year = "2025", engine = "1.4 TSI Turbo Flex", displacement = "1.398 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "150 cv", torque = "25,5 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "52 litros",
      isHybrid = false, notes = "SUV compacto líder VW.")
  val virtus2025 = VehicleProfile(
      id = "vw_virtus_exclusive_2025", brand = "Volkswagen", model = "Virtus", version = "Exclusive 250 TSI",
      year = "2025", engine = "1.4 TSI Turbo Flex", displacement = "1.398 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "150 cv", torque = "25,5 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "52 litros",
      isHybrid = false, notes = "Sedã compacto 521 L.")
  val saveiro2025 = VehicleProfile(
      id = "vw_saveiro_extreme_2025", brand = "Volkswagen", model = "Saveiro", version = "Extreme 1.6",
      year = "2025", engine = "1.6 MSI Flex", displacement = "1.598 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "120 cv (Etanol)", torque = "16,8 kgfm",
      transmission = "Manual 5 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "55 litros",
      isHybrid = false, notes = "Picape compacta VW.")

  // ========== CHEVROLET ==========
  val onix2025 = VehicleProfile(
      id = "chevy_onix_premier_2025", brand = "Chevrolet", model = "Onix", version = "Premier 1.0 Turbo",
      year = "2025", engine = "1.0 Turbo Flex DI", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "121 cv (Etanol)", torque = "18,9 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "44 litros",
      isHybrid = false, notes = "Hatch volume Chevrolet.")
  val tracker2025 = VehicleProfile(
      id = "chevy_tracker_premier_2025", brand = "Chevrolet", model = "Tracker", version = "Premier 1.2 Turbo",
      year = "2025", engine = "1.2 Turbo Flex DI", displacement = "1.199 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "141 cv (Etanol)", torque = "22,9 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "44 litros",
      isHybrid = false, notes = "SUV compacto Chevrolet.")
  val montana2025 = VehicleProfile(
      id = "chevy_montana_ltz_2025", brand = "Chevrolet", model = "Montana", version = "LTZ 1.2 Turbo",
      year = "2025", engine = "1.2 Turbo Flex DI", displacement = "1.199 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "141 cv (Etanol)", torque = "22,9 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "44 litros",
      isHybrid = false, notes = "Picape compacta sobre Tracker.")
  val s10_2025 = VehicleProfile(
      id = "chevy_s10_high_country_2025", brand = "Chevrolet", model = "S10", version = "High Country 2.8",
      year = "2025", engine = "2.8 Turbo Diesel", displacement = "2.776 cm³",
      fuelType = "Diesel", horsepower = "200 cv", torque = "51 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "4x4", tankCapacity = "80 litros",
      isHybrid = false, notes = "Picape média GM.")

  // ========== HYUNDAI ==========
  val hb20_2025 = VehicleProfile(
      id = "hyundai_hb20_platinum_2025", brand = "Hyundai", model = "HB20", version = "Platinum 1.0 Turbo",
      year = "2025", engine = "1.0 Turbo Flex Kappa", displacement = "998 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "120 cv", torque = "17,5 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "50 litros",
      isHybrid = false, notes = "Hatch com garantia 5 anos.")
  val creta2025 = VehicleProfile(
      id = "hyundai_creta_ultimate_2025", brand = "Hyundai", model = "Creta", version = "Ultimate 1.0 Turbo",
      year = "2025", engine = "1.0 Turbo Flex Kappa", displacement = "998 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "120 cv", torque = "17,5 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "50 litros",
      isHybrid = false, notes = "SUV compacto Hyundai.")
  val konaHybrid2026 = VehicleProfile(
      id = "hyundai_kona_hybrid_2026", brand = "Hyundai", model = "Kona", version = "Ultimate Hybrid",
      year = "2026", engine = "1.6 GDI + Elétrico", displacement = "1.580 cm³",
      fuelType = "Gasolina + Híbrido", horsepower = "141 cv (Combinada)", torque = "27 kgfm",
      transmission = "DCT 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "38 litros",
      isHybrid = true, notes = "SUV híbrido pleno.")

  // ========== BYD ==========
  val bydSongPro2026 = VehicleProfile(
      id = "byd_song_pro_2026", brand = "BYD", model = "Song Pro", version = "GL DM-i Flex",
      year = "2026", engine = "1.5 Flex + Elétrico DM-i", displacement = "1.498 cm³",
      fuelType = "Flex + PHEV", horsepower = "218 cv (Combinada)", torque = "32 kgfm",
      transmission = "E-CVT DM-i", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "52 litros",
      isHybrid = true, notes = "SUV PHEV flex best-seller BYD.")
  val bydAtto2_2026 = VehicleProfile(
      id = "byd_atto2_2026", brand = "BYD", model = "Atto 2", version = "DM-i GL Flex",
      year = "2026", engine = "1.5 Flex + Elétrico DM-i", displacement = "1.498 cm³",
      fuelType = "Flex + PHEV", horsepower = "177–197 cv (Combinada)", torque = "—",
      transmission = "E-CVT DM-i", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "45 litros",
      isHybrid = true, notes = "SUV compacto PHEV flex.")
  val bydDolphinMini2026 = VehicleProfile(
      id = "byd_dolphin_mini_2026", brand = "BYD", model = "Dolphin Mini", version = "GL",
      year = "2026", engine = "Motor Elétrico", displacement = "—",
      fuelType = "100% Elétrico", horsepower = "95 cv", torque = "18,4 kgfm",
      transmission = "Redutor 1 marcha", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "— (bateria)",
      isHybrid = false, notes = "Elétrico de entrada mais vendido.")
  val bydDolphin2025 = VehicleProfile(
      id = "byd_dolphin_2025", brand = "BYD", model = "Dolphin", version = "GS",
      year = "2025", engine = "Motor Elétrico", displacement = "—",
      fuelType = "100% Elétrico", horsepower = "177 cv", torque = "29,6 kgfm",
      transmission = "Redutor 1 marcha", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "— (bateria)",
      isHybrid = false, notes = "Hatch elétrico.")

  // ========== GWM ==========
  val havalH6Hev2025 = VehicleProfile(
      id = "gwm_haval_h6_hev2_2025", brand = "GWM", model = "Haval H6", version = "HEV2",
      year = "2025", engine = "1.5 Turbo + Elétrico", displacement = "1.499 cm³",
      fuelType = "Gasolina + Híbrido", horsepower = "243–248 cv (Combinada)", torque = "54,5 kgfm",
      transmission = "DHT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "61 litros",
      isHybrid = true, notes = "Híbrido pleno mais vendido GWM.")
  val havalH6Phev2026 = VehicleProfile(
      id = "gwm_haval_h6_phev19_2026", brand = "GWM", model = "Haval H6", version = "PHEV19 Flex",
      year = "2026", engine = "1.5 Turbo Flex + Elétricos", displacement = "1.499 cm³",
      fuelType = "Flex + PHEV", horsepower = "326 cv (Combinada)", torque = "54,5 kgfm",
      transmission = "DHT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "55 litros",
      isHybrid = true, notes = "Plug-in flex.")

  // ========== JEEP ==========
  val renegade2025 = VehicleProfile(
      id = "jeep_renegade_longitude_2025", brand = "Jeep", model = "Renegade", version = "Longitude T270",
      year = "2025", engine = "1.3 Turbo Flex T270", displacement = "1.332 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "176 cv", torque = "27,5 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "55 litros",
      isHybrid = false, notes = "SUV compacto Jeep.")
  val compass2025 = VehicleProfile(
      id = "jeep_compass_limited_2025", brand = "Jeep", model = "Compass", version = "Limited T270",
      year = "2025", engine = "1.3 Turbo Flex T270", displacement = "1.332 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "176 cv", torque = "27,5 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "60 litros",
      isHybrid = false, notes = "SUV médio Jeep.")
  val commander2025 = VehicleProfile(
      id = "jeep_commander_overland_2025", brand = "Jeep", model = "Commander", version = "Overland T270",
      year = "2025", engine = "1.3 Turbo Flex T270", displacement = "1.332 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "176 cv", torque = "27,5 kgfm",
      transmission = "Automático 6 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "60 litros",
      isHybrid = false, notes = "SUV 7 lugares.")

  // ========== HONDA ==========
  val hrV2025 = VehicleProfile(
      id = "honda_hrv_touring_2025", brand = "Honda", model = "HR-V", version = "Touring",
      year = "2025", engine = "1.5 Turbo Flex VTEC", displacement = "1.498 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "177 cv", torque = "24,5 kgfm",
      transmission = "CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "40 litros",
      isHybrid = false, notes = "SUV compacto Honda.")
  val city2025 = VehicleProfile(
      id = "honda_city_touring_2025", brand = "Honda", model = "City", version = "Touring",
      year = "2025", engine = "1.5 Aspirado Flex", displacement = "1.498 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "126 cv", torque = "15,5 kgfm",
      transmission = "CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "40 litros",
      isHybrid = false, notes = "Sedã compacto.")
  val wrv2025 = VehicleProfile(
      id = "honda_wrv_exl_2025", brand = "Honda", model = "WR-V", version = "EXL",
      year = "2025", engine = "1.5 Aspirado Flex", displacement = "1.498 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "126 cv", torque = "15,5 kgfm",
      transmission = "CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "40 litros",
      isHybrid = false, notes = "SUV de entrada Honda.")

  // ========== RENAULT ==========
  val kwid2025 = VehicleProfile(
      id = "renault_kwid_intense_2025", brand = "Renault", model = "Kwid", version = "Intense 1.0",
      year = "2025", engine = "1.0 SCe Flex", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "70 cv (Etanol)", torque = "9,4 kgfm",
      transmission = "Manual 5 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "38 litros",
      isHybrid = false, notes = "Entry-level Renault.")
  val kardian2025 = VehicleProfile(
      id = "renault_kardian_premiere_2025", brand = "Renault", model = "Kardian", version = "Premiere 1.0 Turbo",
      year = "2025", engine = "1.0 Turbo Flex", displacement = "999 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "125 cv (Etanol)", torque = "22,4 kgfm",
      transmission = "CVT X-Tronic", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "41 litros",
      isHybrid = false, notes = "SUV compacto Renault.")

  // ========== CAOA CHERY ==========
  val tiggo5x2025 = VehicleProfile(
      id = "chery_tiggo5x_pro_2025", brand = "Caoa Chery", model = "Tiggo 5X", version = "Pro 1.5 Turbo",
      year = "2025", engine = "1.5 Turbo Flex", displacement = "1.498 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "150 cv (Etanol)", torque = "21,4 kgfm",
      transmission = "CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "51 litros",
      isHybrid = false, notes = "SUV compacto Chery.")
  val tiggo7_2025 = VehicleProfile(
      id = "chery_tiggo7_pro_2025", brand = "Caoa Chery", model = "Tiggo 7", version = "Pro 1.5 Turbo",
      year = "2025", engine = "1.5 Turbo Flex", displacement = "1.498 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "150–160 cv", torque = "21,4 kgfm",
      transmission = "CVT / DCT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "51 litros",
      isHybrid = false, notes = "SUV médio Chery.")

  // ========== NISSAN ==========
  val kicks2025 = VehicleProfile(
      id = "nissan_kicks_exclusive_2025", brand = "Nissan", model = "Kicks", version = "Exclusive 1.6",
      year = "2025", engine = "1.6 Dual VVT Flex", displacement = "1.598 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "114 cv (Etanol)", torque = "15,5 kgfm",
      transmission = "CVT", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "41 litros",
      isHybrid = false, notes = "SUV compacto Nissan.")

  // ========== FORD ==========
  val ranger2025 = VehicleProfile(
      id = "ford_ranger_limited_2025", brand = "Ford", model = "Ranger", version = "Limited 3.0 V6",
      year = "2025", engine = "3.0 V6 Turbo Diesel", displacement = "2.993 cm³",
      fuelType = "Diesel", horsepower = "250 cv", torque = "61,2 kgfm",
      transmission = "Automático 10 marchas", drivetrain = "4x4", tankCapacity = "80 litros",
      isHybrid = false, notes = "Picape média Ford nova geração.")
  val territory2025 = VehicleProfile(
      id = "ford_territory_titanium_2025", brand = "Ford", model = "Territory", version = "Titanium",
      year = "2025", engine = "1.5 Turbo Flex", displacement = "1.497 cm³",
      fuelType = "Flex (Etanol / Gasolina)", horsepower = "169 cv (Etanol)", torque = "25,5 kgfm",
      transmission = "DCT 7 marchas", drivetrain = "Tração Dianteira (FWD)", tankCapacity = "55 litros",
      isHybrid = false, notes = "SUV médio Ford.")

  val all = listOf(
      corollaGliHybrid2026, corollaAltis2025, corollaHybrid2025, corollaCrossHybrid2025, yarisCrossHybrid2026, hilux2025,
      strada2025, argo2025, pulse2025, fastback2025, toro2025, mobi2025,
      poloTsi2025, tera2026, nivus2025, tcross2025, virtus2025, saveiro2025,
      onix2025, tracker2025, montana2025, s10_2025,
      hb20_2025, creta2025, konaHybrid2026,
      bydSongPro2026, bydAtto2_2026, bydDolphinMini2026, bydDolphin2025,
      havalH6Hev2025, havalH6Phev2026,
      renegade2025, compass2025, commander2025,
      hrV2025, city2025, wrv2025,
      kwid2025, kardian2025,
      tiggo5x2025, tiggo7_2025,
      kicks2025,
      ranger2025, territory2025
  )
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
    val odometerKm: Float = 0f, // km (from ECU PID 01A6 or integrated trip accumulator)
    val tripDistanceKm: Float = 0f, // km (current trip distance)
    val rangeKm: Int = 0, // km (calculated range based on remaining fuel & consumption)
    val fuelLevelPercent: Float? = null, // % (PID 012F tank level if supported)
    val fuelFlowLitersPerHour: Float = 0f, // L/h
    val totalFuelConsumedLiters: Float = 0f, // L
    val hybridBatterySoc: Int = 0, // % (HV Battery State of Charge via Toyota 7E2/21CE)
    val hybridMotorPowerKw: Float = 0f, // kW (Electric motor/generator power)
    val isHybridDetected: Boolean = false, // true if Toyota Hybrid ECU 7E2 responded
    val oilTemp: Int? = null, // °C (PID 015C, se suportado)
    val ambientTemp: Int? = null, // °C (PID 0146, se suportado)
    val catalystTemp: Int? = null, // °C (PID 013C, se suportado)
    val tripDurationSec: Int = 0,
    val tripMaxSpeed: Float = 0f,
    val tripAvgSpeed: Float = 0f,
    val hybridExtras: Map<String, String> = emptyMap(), // "Rótulo|unidade" -> valor (PIDs híbridos mapeados)
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

enum class PaymentStatus {
  PENDING, // Pendente / Vencida
  PARTIAL, // Recebida Parcial
  PAID     // Recebido / Quitado
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
    val avgConsumption: Float,
    val totalAmount: Double = 0.0, // Valor cobrado pela viagem (ex: R$ 200,00)
    val paidAmount: Double = 0.0,  // Valor recebido / pago até o momento
    val timestampMs: Long = 0L,    // Timestamp para ordenação cronológica das mais antigas para as mais novas
    val notes: String = ""
) {
  val remainingAmount: Double
    get() = (totalAmount - paidAmount).coerceAtLeast(0.0)

  val paymentStatus: PaymentStatus
    get() = when {
      totalAmount <= 0.0 -> PaymentStatus.PAID
      paidAmount >= totalAmount - 0.001 -> PaymentStatus.PAID
      paidAmount > 0.001 -> PaymentStatus.PARTIAL
      else -> PaymentStatus.PENDING
    }
}

fun TripRecord.toJsonObject(): org.json.JSONObject {
  val obj = org.json.JSONObject()
  obj.put("id", id)
  obj.put("date", date)
  obj.put("vehicleName", vehicleName)
  obj.put("distanceKm", distanceKm.toDouble())
  obj.put("durationMinutes", durationMinutes)
  obj.put("avgSpeed", avgSpeed.toDouble())
  obj.put("maxSpeed", maxSpeed.toDouble())
  obj.put("fuelConsumedLiters", fuelConsumedLiters.toDouble())
  obj.put("avgConsumption", avgConsumption.toDouble())
  obj.put("totalAmount", totalAmount)
  obj.put("paidAmount", paidAmount)
  obj.put("timestampMs", timestampMs)
  obj.put("notes", notes)
  return obj
}

fun tripRecordFromJson(json: org.json.JSONObject): TripRecord {
  return TripRecord(
      id = json.optString("id", java.util.UUID.randomUUID().toString()),
      date = json.optString("date", "Hoje"),
      vehicleName = json.optString("vehicleName", "Veículo"),
      distanceKm = json.optDouble("distanceKm", 0.0).toFloat(),
      durationMinutes = json.optInt("durationMinutes", 0),
      avgSpeed = json.optDouble("avgSpeed", 0.0).toFloat(),
      maxSpeed = json.optDouble("maxSpeed", 0.0).toFloat(),
      fuelConsumedLiters = json.optDouble("fuelConsumedLiters", 0.0).toFloat(),
      avgConsumption = json.optDouble("avgConsumption", 0.0).toFloat(),
      totalAmount = json.optDouble("totalAmount", 0.0),
      paidAmount = json.optDouble("paidAmount", 0.0),
      timestampMs = json.optLong("timestampMs", 0L),
      notes = json.optString("notes", "")
  )
}

/** Histórico recente para os gráficos ao vivo (mais antigo -> mais novo). */
data class ChartHistory(
    val rpm: List<Float> = emptyList(),
    val speed: List<Float> = emptyList(),
    val load: List<Float> = emptyList(),
    val coolant: List<Float> = emptyList()
)

/**
 * Classifica o combustível pela % de etanol que a ECU informa (PID 01 52).
 * No Brasil a gasolina comum e a premium já vêm com ~30% de etanol (E30) e a ECU só ESTIMA o teor
 * (costuma errar alguns pontos): 33% = gasolina comum, não "etanol".
 */
fun fuelBlendLabel(ethanolPct: Int): String = when {
  ethanolPct <= 0 -> "Sem leitura"
  ethanolPct <= 40 -> "Gasolina comum (mistura ~E30)"
  ethanolPct <= 70 -> "Mistura gasolina + etanol"
  else -> "Etanol (E85 / E100)"
}
