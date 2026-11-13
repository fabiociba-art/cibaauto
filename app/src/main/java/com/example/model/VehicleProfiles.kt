package com.example.model

/** Ponto único de acesso aos perfis pré-carregados (fonte: PreloadedVehicles). */
object VehicleProfiles {
    val DEFAULT_PROFILES: List<VehicleProfile> = PreloadedVehicles.all
    val DEFAULT: VehicleProfile = PreloadedVehicles.corollaGliHybrid2026
}
