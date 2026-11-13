package com.example.model

/**
 * Consulta proprietária (fora do padrão OBD-II) para ler um valor de uma ECU específica,
 * ex.: SOC da bateria híbrida. Só use com comandos VERIFICADOS para o seu veículo.
 * Ex. (Prius Gen4, NÃO validado para Corolla): header "747", request "227A76",
 * responsePrefix "627A76", byteIndex 0, multiplier 20, divisor 51.
 */
data class CustomPidQuery(
    val header: String,
    val request: String,
    val responsePrefix: String,
    val byteIndex: Int = 0,
    val multiplier: Float = 1f,
    val divisor: Float = 1f
)
