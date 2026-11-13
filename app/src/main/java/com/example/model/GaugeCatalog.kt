package com.example.model

/**
 * Sensor que pode ser exibido em um manômetro do dashboard.
 * [valueOf] devolve null quando não há leitura atual (o manômetro mostra "--").
 * [pids] = PIDs do SensorRegistry que precisam ser lidos com prioridade enquanto o sensor está na tela.
 */
class GaugeSensor(
    val id: String,
    val name: String,
    val shortName: String,
    val unit: String,
    val min: Float,
    val max: Float,
    val decimals: Int,
    val origin: DataOrigin,
    val group: String,
    val pids: Set<Int> = emptySet(),
    val originOf: ((TelemetryData) -> DataOrigin)? = null,
    val valueOf: (TelemetryData) -> Float?
) {
    fun originFor(t: TelemetryData): DataOrigin = originOf?.invoke(t) ?: origin

    fun format(v: Float): String {
        val n = "%.${decimals}f".format(v)
        return when {
            unit.isBlank() -> n
            unit in COMPACT_UNITS -> n + unit
            else -> "$n $unit"
        }
    }

    private companion object {
        val COMPACT_UNITS = setOf("°C", "%", "V", "°")
    }
}

object GaugeCatalog {

    const val SLOT_COUNT = 5
    const val GROUP_MAIN = "Principais"

    /** Manômetros originais do dashboard (a 5ª posição só aparece em veículos híbridos). */
    val DEFAULT_SLOTS: List<String> = listOf("oil_temp", "coolant_temp", "ethanol", "battery12", "hybrid_soc")

    private val fixed: List<GaugeSensor> = listOf(
        GaugeSensor("oil_temp", "Temperatura do óleo", "Óleo", "°C", -20f, 120f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> t.oilTempC.takeUnless { it.isNaN() } }),
        GaugeSensor("coolant_temp", "Temperatura do líquido de arrefecimento", "Água", "°C", 0f, 300f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> t.coolantTempC.takeUnless { it.isNaN() } }),
        GaugeSensor("ethanol", "Porcentagem de etanol na gasolina", "% Etanol gasolina", "%", 0f, 100f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> t.ethanolPercent.takeUnless { it.isNaN() } }),
        GaugeSensor("battery12", "Tensão da bateria auxiliar 12 V", "Bateria 12 V", "V", 0f, 16f, 1, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> t.batteryVoltage.takeIf { it > 0.1f } }),
        GaugeSensor("hybrid_soc", "SOC da bateria híbrida", "SOC híbrida", "%", 0f, 100f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> if (t.hybridSocAvailable) t.hybridBatterySocPercent else null }),
        GaugeSensor("engine_load", "Carga calculada do motor", "Carga", "%", 0f, 100f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> if (t.ecuLinkOk) t.engineLoadPercent else null }),
        GaugeSensor("throttle", "Posição da borboleta (TPS)", "Borboleta", "%", 0f, 100f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> if (t.ecuLinkOk) t.throttlePercent else null }),
        GaugeSensor("speed", "Velocidade do veículo", "Velocidade", "km/h", 0f, 220f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> if (t.ecuLinkOk) t.speedKmh else null }),
        GaugeSensor("rpm", "Rotação do motor", "RPM", "rpm", 0f, 6500f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> if (t.ecuLinkOk) t.rpm else null }),
        GaugeSensor("fuel_pct", "Combustível no tanque (%)", "Tanque", "%", 0f, 100f, 0, DataOrigin.MEASURED, GROUP_MAIN,
            valueOf = { t -> if (t.fuelLevelAvailable) t.fuelLevelPercent else null }),
        GaugeSensor("fuel_liters", "Combustível no tanque (litros)", "Litros", "L", 0f, 60f, 1, DataOrigin.CALCULATED, GROUP_MAIN,
            valueOf = { t -> if (t.fuelLevelAvailable && !t.fuelLiters.isNaN()) t.fuelLiters else null }),
        GaugeSensor("avg_consumption", "Consumo médio", "Consumo médio", "km/L", 0f, 30f, 1, DataOrigin.CALCULATED, GROUP_MAIN,
            originOf = { t -> if (t.consumptionIsEstimated) DataOrigin.ESTIMATED else DataOrigin.CALCULATED },
            valueOf = { t -> t.avgConsumptionKmPerL.takeIf { it > 0f } }),
        GaugeSensor("inst_consumption", "Consumo instantâneo", "Consumo inst.", "km/L", 0f, 30f, 1, DataOrigin.CALCULATED, GROUP_MAIN,
            originOf = { t -> if (t.consumptionIsEstimated) DataOrigin.ESTIMATED else DataOrigin.CALCULATED },
            valueOf = { t ->
                val c = t.instantConsumptionKmPerL
                if (t.speedKmh >= 3f && c > 0f && c < 99f) c else null
            })
    )

    private val shortNames = mapOf(
        "stft_b1" to "STFT", "ltft_b1" to "LTFT", "fuel_pressure" to "Pressão comb.", "map" to "MAP",
        "iat" to "Ar admissão", "maf" to "MAF", "fuel_rail_gauge" to "Trilho comb.", "fuel_rail_abs" to "Trilho abs.",
        "baro" to "Atmosférica", "abs_load" to "Carga abs.", "timing" to "Avanço", "ambient_temp" to "Ar externo",
        "o2_wr_b1s1_lambda" to "Lambda B1S1", "cmd_equiv_ratio" to "Lambda cmd.", "cat_temp_b1s1" to "Catalisador",
        "egr_cmd" to "EGR", "evap_purge" to "Purga", "evap_pressure" to "EVAP", "rel_throttle" to "Borb. rel.",
        "cmd_throttle" to "Borb. cmd.", "pedal_d" to "Pedal D", "pedal_e" to "Pedal E", "ctrl_voltage" to "Tensão ECU",
        "run_time" to "Tempo motor", "warmups" to "Aquecim.", "dist_since_clear" to "Km s/ apagar",
        "time_since_clear" to "Min s/ apagar", "dist_mil" to "Km c/ MIL", "time_mil" to "Min c/ MIL",
        SensorRegistry.AFR_ID to "AFR"
    )

    private fun fromRegistry(def: SensorDef): GaugeSensor {
        val pids = when {
            def.id == SensorRegistry.AFR_ID -> setOf(0x24, 0x44) // AFR é calculado a partir da lambda
            def.pid >= 0 -> setOf(def.pid)
            else -> emptySet()
        }
        return GaugeSensor(
            id = def.id,
            name = def.name,
            shortName = shortNames[def.id] ?: def.name.take(14),
            unit = def.unit,
            min = def.minValue,
            max = def.maxValue,
            decimals = def.decimals,
            origin = def.origin,
            group = def.category.label,
            pids = pids,
            valueOf = { t -> t.sensors[def.id] }
        )
    }

    val all: List<GaugeSensor> = fixed + SensorRegistry.all.map { fromRegistry(it) }

    val byId: Map<String, GaugeSensor> = all.associateBy { it.id }

    /** Lista salva -> lista válida de [SLOT_COUNT] posições (ids inválidos voltam ao original). */
    fun sanitize(saved: List<String>?): List<String> =
        List(SLOT_COUNT) { i -> saved?.getOrNull(i)?.takeIf { it in byId } ?: DEFAULT_SLOTS[i] }
}
