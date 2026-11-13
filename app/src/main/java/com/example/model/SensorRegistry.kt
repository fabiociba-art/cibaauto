package com.example.model

/** Como o app obteve o valor: lido direto da ECU, calculado a partir de leituras reais ou estimado. */
enum class DataOrigin(val label: String, val marker: String) {
    MEASURED("medido", ""),
    CALCULATED("calculado", " ƒ"),
    ESTIMATED("estimado", " ~")
}

enum class SensorCategory(val label: String) {
    AIR_FUEL("Ar e combustível"),
    ENGINE("Motor e ignição"),
    EMISSIONS("Emissões e sondas"),
    PEDALS("Borboleta e pedais"),
    ELECTRICAL("Elétrica"),
    TIME_DISTANCE("Tempo e distância")
}

/**
 * Definição de um sensor padrão OBD-II (modo 01). [decode] recebe só os bytes de dados da resposta
 * (já sem o cabeçalho 41xx) e devolve null se vierem bytes a menos.
 * [pid] = -1 indica sensor derivado (calculado pelo app, não consultado na ECU).
 */
class SensorDef(
    val id: String,
    val name: String,
    val unit: String,
    val pid: Int,
    val category: SensorCategory,
    val minValue: Float,
    val maxValue: Float,
    val decimals: Int = 0,
    val origin: DataOrigin = DataOrigin.MEASURED,
    val decode: (IntArray) -> Float? = { null }
) {
    fun format(value: Float): String {
        val number = "%.${decimals}f".format(value)
        return if (unit.isBlank()) number else "$number $unit"
    }
}

object SensorRegistry {

    const val AFR_ID = "afr"

    private fun u8(f: (Int) -> Float): (IntArray) -> Float? = { b -> if (b.isEmpty()) null else f(b[0]) }

    private fun u16(f: (Int) -> Float): (IntArray) -> Float? =
        { b -> if (b.size < 2) null else f((b[0] shl 8) or b[1]) }

    private fun s16(raw: Int): Int = if (raw >= 0x8000) raw - 0x10000 else raw

    /** Sensores padrão que a ECU pode (ou não) suportar. O app só consulta os que ela declarar. */
    val standard: List<SensorDef> = listOf(
        SensorDef("stft_b1", "Correção combustível curto prazo (STFT)", "%", 0x06, SensorCategory.AIR_FUEL, -25f, 25f, 1,
            decode = u8 { (it - 128) * 100f / 128f }),
        SensorDef("ltft_b1", "Correção combustível longo prazo (LTFT)", "%", 0x07, SensorCategory.AIR_FUEL, -25f, 25f, 1,
            decode = u8 { (it - 128) * 100f / 128f }),
        SensorDef("fuel_pressure", "Pressão do combustível", "kPa", 0x0A, SensorCategory.AIR_FUEL, 0f, 765f, 0,
            decode = u8 { it * 3f }),
        SensorDef("map", "Pressão do coletor (MAP)", "kPa", 0x0B, SensorCategory.AIR_FUEL, 0f, 255f, 0,
            decode = u8 { it.toFloat() }),
        SensorDef("iat", "Temperatura do ar de admissão", "°C", 0x0F, SensorCategory.AIR_FUEL, -40f, 120f, 0,
            decode = u8 { it - 40f }),
        SensorDef("maf", "Fluxo de ar (MAF)", "g/s", 0x10, SensorCategory.AIR_FUEL, 0f, 120f, 1,
            decode = u16 { it / 100f }),
        SensorDef("fuel_rail_gauge", "Pressão do trilho de combustível", "kPa", 0x23, SensorCategory.AIR_FUEL, 0f, 20000f, 0,
            decode = u16 { it * 10f }),
        SensorDef("fuel_rail_abs", "Pressão absoluta do trilho", "kPa", 0x59, SensorCategory.AIR_FUEL, 0f, 20000f, 0,
            decode = u16 { it * 10f }),
        SensorDef("baro", "Pressão atmosférica", "kPa", 0x33, SensorCategory.AIR_FUEL, 60f, 110f, 0,
            decode = u8 { it.toFloat() }),
        SensorDef("abs_load", "Carga absoluta do motor", "%", 0x43, SensorCategory.ENGINE, 0f, 100f, 0,
            decode = u16 { it * 100f / 255f }),
        SensorDef("timing", "Avanço da ignição", "°", 0x0E, SensorCategory.ENGINE, -20f, 60f, 1,
            decode = u8 { it / 2f - 64f }),
        SensorDef("ambient_temp", "Temperatura externa (ambiente)", "°C", 0x46, SensorCategory.ENGINE, -40f, 60f, 0,
            decode = u8 { it - 40f }),
        SensorDef("o2_wr_b1s1_lambda", "Sonda lambda banco 1 sensor 1", "λ", 0x24, SensorCategory.EMISSIONS, 0f, 2f, 3,
            decode = u16 { it * 2f / 65536f }),
        SensorDef("cmd_equiv_ratio", "Relação ar/combustível comandada", "λ", 0x44, SensorCategory.EMISSIONS, 0f, 2f, 3,
            decode = u16 { it * 2f / 65536f }),
        SensorDef("cat_temp_b1s1", "Temperatura do catalisador B1S1", "°C", 0x3C, SensorCategory.EMISSIONS, -40f, 1000f, 0,
            decode = u16 { it / 10f - 40f }),
        SensorDef("egr_cmd", "EGR comandada", "%", 0x2C, SensorCategory.EMISSIONS, 0f, 100f, 0,
            decode = u8 { it * 100f / 255f }),
        SensorDef("evap_purge", "Válvula de purga do cânister (comando)", "%", 0x2E, SensorCategory.EMISSIONS, 0f, 100f, 0,
            decode = u8 { it * 100f / 255f }),
        SensorDef("evap_pressure", "Pressão do sistema EVAP", "Pa", 0x32, SensorCategory.EMISSIONS, -8192f, 8191f, 0,
            decode = u16 { s16(it) / 4f }),
        SensorDef("rel_throttle", "Borboleta relativa", "%", 0x45, SensorCategory.PEDALS, 0f, 100f, 0,
            decode = u8 { it * 100f / 255f }),
        SensorDef("cmd_throttle", "Borboleta comandada", "%", 0x4C, SensorCategory.PEDALS, 0f, 100f, 0,
            decode = u8 { it * 100f / 255f }),
        SensorDef("pedal_d", "Pedal do acelerador D", "%", 0x49, SensorCategory.PEDALS, 0f, 100f, 0,
            decode = u8 { it * 100f / 255f }),
        SensorDef("pedal_e", "Pedal do acelerador E", "%", 0x4A, SensorCategory.PEDALS, 0f, 100f, 0,
            decode = u8 { it * 100f / 255f }),
        // Tensão que a ECU enxerga na alimentação (12 V). Não é a bateria de alta tensão.
        SensorDef("ctrl_voltage", "Tensão de alimentação da ECU", "V", 0x42, SensorCategory.ELECTRICAL, 0f, 20f, 2,
            decode = u16 { it / 1000f }),
        SensorDef("run_time", "Tempo de motor ligado", "s", 0x1F, SensorCategory.TIME_DISTANCE, 0f, 65535f, 0,
            decode = u16 { it.toFloat() }),
        SensorDef("warmups", "Aquecimentos desde apagar códigos", "", 0x30, SensorCategory.TIME_DISTANCE, 0f, 255f, 0,
            decode = u8 { it.toFloat() }),
        SensorDef("dist_since_clear", "Distância desde apagar códigos", "km", 0x31, SensorCategory.TIME_DISTANCE, 0f, 65535f, 0,
            decode = u16 { it.toFloat() }),
        SensorDef("time_since_clear", "Tempo desde apagar códigos", "min", 0x4E, SensorCategory.TIME_DISTANCE, 0f, 65535f, 0,
            decode = u16 { it.toFloat() }),
        SensorDef("dist_mil", "Distância com luz de avaria acesa", "km", 0x21, SensorCategory.TIME_DISTANCE, 0f, 65535f, 0,
            decode = u16 { it.toFloat() }),
        SensorDef("time_mil", "Tempo com luz de avaria acesa", "min", 0x4D, SensorCategory.TIME_DISTANCE, 0f, 65535f, 0,
            decode = u16 { it.toFloat() })
    )

    /** Sensores calculados pelo app a partir de leituras reais (sempre marcados como calculados). */
    val derived: List<SensorDef> = listOf(
        SensorDef(AFR_ID, "Relação ar/combustível (AFR)", ":1", -1, SensorCategory.EMISSIONS, 5f, 25f, 1,
            origin = DataOrigin.CALCULATED)
    )

    val all: List<SensorDef> = standard + derived

    val byId: Map<String, SensorDef> = all.associateBy { it.id }
}
