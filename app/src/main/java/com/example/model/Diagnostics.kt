package com.example.model

import com.example.data.DtcLibrary

enum class DtcKind(val label: String) {
    STORED("Armazenados"),
    PENDING("Pendentes"),
    PERMANENT("Permanentes")
}

data class DtcEntry(
    val code: String,
    val kind: DtcKind,
    val info: DtcLibrary.DtcInfo,
    /** false = código fora da biblioteca (descrição genérica pelo prefixo) */
    val isKnown: Boolean
)

fun dtcEntryOf(code: String, kind: DtcKind): DtcEntry =
    DtcEntry(code, kind, DtcLibrary.lookup(code), DtcLibrary.isKnown(code))

data class DiagnosticsState(
    /** Luz de injeção (MIL): null = ainda não lida */
    val milOn: Boolean? = null,
    /** Quantidade de códigos confirmados informada pela ECU (PID 0101) */
    val reportedCount: Int = 0,
    val stored: List<DtcEntry> = emptyList(),
    val pending: List<DtcEntry> = emptyList(),
    val permanent: List<DtcEntry> = emptyList(),
    val isScanning: Boolean = false,
    /** 0 = nenhuma leitura completa ainda */
    val lastScanMs: Long = 0L,
    val message: String? = null,
    val isDemo: Boolean = false
) {
    val all: List<DtcEntry> get() = stored + pending + permanent

    val hasFaults: Boolean get() = milOn == true || reportedCount > 0 || all.isNotEmpty()

    val worstSeverity: DTCLevel
        get() = when {
            all.any { it.info.severity == DTCLevel.CRITICAL } -> DTCLevel.CRITICAL
            hasFaults -> DTCLevel.WARNING
            else -> DTCLevel.INFO
        }

    fun list(kind: DtcKind): List<DtcEntry> = when (kind) {
        DtcKind.STORED -> stored
        DtcKind.PENDING -> pending
        DtcKind.PERMANENT -> permanent
    }
}

/** Texto para compartilhar/copiar o relatório de diagnóstico. */
fun DiagnosticsState.toReport(vehicleName: String, timeText: String): String = buildString {
    appendLine("Relatório de diagnóstico - CibaAuto")
    appendLine("Veículo: $vehicleName")
    appendLine("Leitura: $timeText")
    appendLine("Luz de injeção (MIL): ${when (milOn) { true -> "ACESA"; false -> "apagada"; null -> "não lida" }}")
    appendLine()
    for (kind in DtcKind.values()) {
        val items = list(kind)
        appendLine("${kind.label}: ${if (items.isEmpty()) "nenhum" else items.size.toString()}")
        for (e in items) {
            appendLine("  ${e.code} - ${e.info.title}: ${e.info.description}")
            if (e.info.possibleCauses.isNotBlank()) appendLine("    Causas prováveis: ${e.info.possibleCauses}")
            if (e.info.recommendation.isNotBlank()) appendLine("    Recomendação: ${e.info.recommendation}")
        }
    }
}
