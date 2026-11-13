package com.example.obd

/**
 * Decodificação das respostas dos modos 03/07/0A (códigos de falha) e do PID 0101.
 * Lógica pura (sem Android) para poder ser testada com testes unitários.
 */
object DtcParser {

    private fun isHex(s: String) = s.isNotEmpty() && s.all { it in '0'..'9' || it in 'A'..'F' }

    /**
     * Separa a resposta bruta do ELM327 em mensagens (uma por ECU), em hexadecimal sem espaços.
     * Trata respostas multi-frame (linha de contagem "00A" e linhas "0:", "1:" ...) e ignora ruído
     * como SEARCHING..., NO DATA e respostas negativas incompletas.
     */
    fun splitMessages(raw: String): List<String> {
        val msgs = mutableListOf<StringBuilder>()
        for (line in raw.split('\r', '\n')) {
            val l = line.replace(" ", "").uppercase()
            if (l.isEmpty()) continue
            if (l.length >= 2 && l[1] == ':') {
                val rest = l.substring(2)
                if (!isHex(rest)) continue
                if (l[0] == '0' || msgs.isEmpty()) msgs.add(StringBuilder(rest)) else msgs.last().append(rest)
                continue
            }
            if (!isHex(l)) continue          // SEARCHING..., NO DATA, BUS INIT...
            if (l.length % 2 != 0) continue  // linha de contagem de bytes do ELM (ex.: "00A")
            msgs.add(StringBuilder(l))
        }
        return msgs.map { it.toString() }
    }

    fun decodePair(a: Int, b: Int): String {
        val type = "PCBU"[(a shr 6) and 0x03]
        val d1 = (a shr 4) and 0x03
        val d2 = a and 0x0F
        return "%c%d%X%02X".format(type, d1, d2, b and 0xFF)
    }

    /**
     * @param responseByte "43" (modo 03), "47" (modo 07) ou "4A" (modo 0A)
     * @param canWithCount true em CAN (ISO 15765-4): o primeiro byte após o modo é a quantidade de DTCs.
     *                     false nos protocolos antigos (J1850/ISO 9141/KWP): pares até 0000.
     */
    fun decodeDtcs(raw: String, responseByte: String, canWithCount: Boolean): List<String> {
        val result = linkedSetOf<String>()
        for (msg in splitMessages(raw)) {
            if (!msg.startsWith(responseByte)) continue
            val bytes = msg.substring(responseByte.length).chunked(2).map { it.toInt(16) }
            val payload: List<Int> = if (canWithCount) {
                if (bytes.isEmpty()) continue
                val count = bytes[0]
                bytes.drop(1).take(count * 2)
            } else bytes
            var i = 0
            while (i + 1 < payload.size) {
                val a = payload[i]
                val b = payload[i + 1]
                if (a != 0 || b != 0) result.add(decodePair(a, b))
                i += 2
            }
        }
        return result.toList()
    }

    /** Resposta válida do modo (mesmo sem códigos, ex.: "4300"). */
    fun hasValidResponse(raw: String, responseByte: String): Boolean =
        splitMessages(raw).any { it.startsWith(responseByte) }

    /** PID 0101: Pair(MIL acesa, quantidade de DTCs confirmados). */
    fun parseMonitorStatus(raw: String): Pair<Boolean, Int>? {
        for (msg in splitMessages(raw)) {
            if (!msg.startsWith("4101") || msg.length < 6) continue
            val a = msg.substring(4, 6).toInt(16)
            return Pair((a and 0x80) != 0, a and 0x7F)
        }
        return null
    }

    /** Número do protocolo ativo a partir da resposta do ATDPN ("A6", "6"...). */
    fun isCanProtocol(dpnReply: String): Boolean {
        val cleaned = dpnReply.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }
        if (cleaned.isEmpty()) return true // desconhecido: assume CAN (maioria dos carros recentes)
        val proto = if (cleaned[0] == 'A' && cleaned.length > 1) cleaned[1] else cleaned[0]
        return proto in '6'..'9'
    }
}
