package com.example.data

/**
 * Consumos oficiais Inmetro (PBEV) em km/L.
 * Usados como referência para as cores dos medidores de consumo.
 * Verde = próximo ou acima do valor oficial; abaixo vai para amarelo e vermelho.
 */
object InmetroConsumption {

    data class Ref(
        val cityGas: Float,
        val highwayGas: Float,
        val cityEth: Float,
        val highwayEth: Float
    ) {
        /** Referência principal (melhor valor gasolina) */
        val referenceGas: Float get() = maxOf(cityGas, highwayGas)
        val referenceEth: Float get() = maxOf(cityEth, highwayEth)
    }

    private val table = mapOf(
        "corolla hybrid" to Ref(18.5f, 15.7f, 12.8f, 11.1f),
        "corolla cross hybrid" to Ref(17.7f, 14.6f, 12.5f, 10.1f),
        "corolla" to Ref(12.3f, 14.9f, 8.6f, 10.7f),
        "corolla cross" to Ref(11.6f, 13.3f, 8.2f, 8.8f),
        "hilux" to Ref(9.0f, 11.5f, 6.5f, 8.0f),
        "yaris" to Ref(13.5f, 15.2f, 9.5f, 10.8f),
        "onix" to Ref(13.9f, 17.4f, 9.7f, 12.2f),
        "onix plus" to Ref(13.9f, 17.4f, 9.7f, 12.2f),
        "tracker" to Ref(11.2f, 13.5f, 7.8f, 9.5f),
        "pulse" to Ref(12.5f, 14.8f, 8.8f, 10.5f),
        "fastback" to Ref(12.0f, 14.2f, 8.5f, 10.0f),
        "t-cross" to Ref(11.8f, 14.0f, 8.2f, 9.8f),
        "nivus" to Ref(12.2f, 14.5f, 8.5f, 10.2f),
        "polo" to Ref(13.0f, 15.5f, 9.2f, 11.0f),
        "virtus" to Ref(12.8f, 15.2f, 9.0f, 10.8f),
        "hb20" to Ref(13.2f, 15.0f, 9.3f, 10.6f),
        "creta" to Ref(11.5f, 13.8f, 8.0f, 9.6f),
        "byd song" to Ref(16.0f, 14.0f, 11.0f, 9.5f),
        "byd king" to Ref(44.0f, 36.0f, 30.0f, 25.0f),
        "default" to Ref(12.0f, 14.0f, 8.5f, 10.0f)
    )

    fun forProfile(name: String, isHybrid: Boolean = false): Ref {
        val key = name.lowercase()
        // Prefere a chave mais longa (mais específica): "corolla cross hybrid" > "corolla cross" > "corolla"
        val best = table.entries
            .filter { it.key != "default" && key.contains(it.key) }
            .maxByOrNull { it.key.length }
        // Híbrido sem entrada própria não deve herdar o consumo da versão a combustão
        val usable = best?.takeIf { !isHybrid || it.key.contains("hybrid") || it.key.startsWith("byd") }
        return usable?.value
            ?: if (isHybrid) Ref(16.0f, 14.0f, 11.0f, 9.5f) else table["default"]!!
    }

    fun forProfile(profile: com.example.model.VehicleProfile): Ref {
        val base = "${profile.brand} ${profile.model}".lowercase()
        val text = if (profile.isHybrid && !base.contains("hybrid")) "$base hybrid" else base
        return forProfile(text, profile.isHybrid)
    }

    /**
     * Cor do consumo: verde até a referência oficial, depois amarelo e vermelho.
     * reference = valor Inmetro (ex: 15.7 ou 18.5 para Corolla Hybrid gasolina)
     */
    fun colorForConsumption(kml: Float, reference: Float): androidx.compose.ui.graphics.Color {
        val green = androidx.compose.ui.graphics.Color(0xFF00CC55)
        val yellow = androidx.compose.ui.graphics.Color(0xFFFFAA00)
        val red = androidx.compose.ui.graphics.Color(0xFFFF3333)
        val yellowStart = reference * 0.75f
        val redStart = reference * 0.50f
        return when {
            kml >= reference -> green
            kml >= yellowStart -> androidx.compose.ui.graphics.lerp(yellow, green, (kml - yellowStart) / (reference - yellowStart))
            kml >= redStart -> androidx.compose.ui.graphics.lerp(red, yellow, (kml - redStart) / (yellowStart - redStart))
            else -> red
        }
    }
}
