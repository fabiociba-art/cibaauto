package com.example.data

import com.example.model.DTCLevel

/**
 * Biblioteca de códigos DTC com descrições em português.
 * Baseada em códigos OBD-II genéricos + específicos comuns em veículos brasileiros
 * (Toyota/Corolla híbrido, Fiat, VW, GM, Hyundai, BYD).
 */
object DtcLibrary {

    data class DtcInfo(
        val title: String,
        val description: String,
        val system: String,
        val severity: DTCLevel = DTCLevel.WARNING,
        val possibleCauses: String = "",
        val recommendation: String = ""
    )

    private val library = mapOf(
        // ---- Combustível / Ar ----
        "P0100" to DtcInfo("Sensor MAF", "Circuito do sensor de fluxo de ar (MAF) com mau funcionamento.", "Powertrain", possibleCauses = "Sensor MAF sujo ou defeituoso, fiação", recommendation = "Limpar ou substituir o sensor MAF"),
        "P0101" to DtcInfo("Sensor MAF", "Faixa/desempenho do sensor MAF fora do esperado.", "Powertrain"),
        "P0102" to DtcInfo("Sensor MAF", "Sinal baixo do sensor MAF.", "Powertrain"),
        "P0103" to DtcInfo("Sensor MAF", "Sinal alto do sensor MAF.", "Powertrain"),
        "P0110" to DtcInfo("Sensor IAT", "Circuito do sensor de temperatura do ar de admissão.", "Powertrain"),
        "P0115" to DtcInfo("Sensor de Temperatura do Motor", "Circuito do sensor de temperatura do líquido de arrefecimento.", "Powertrain"),
        "P0116" to DtcInfo("Sensor de Temperatura do Motor", "Faixa/desempenho do sensor de temperatura do motor.", "Powertrain"),
        "P0117" to DtcInfo("Sensor de Temperatura do Motor", "Sinal baixo do sensor de temperatura do motor.", "Powertrain"),
        "P0118" to DtcInfo("Sensor de Temperatura do Motor", "Sinal alto do sensor de temperatura do motor.", "Powertrain"),
        "P0120" to DtcInfo("Sensor TPS", "Circuito do sensor de posição da borboleta (TPS).", "Powertrain"),
        "P0121" to DtcInfo("Sensor TPS", "Faixa/desempenho do TPS fora do esperado.", "Powertrain"),
        "P0122" to DtcInfo("Sensor TPS", "Sinal baixo do TPS.", "Powertrain"),
        "P0123" to DtcInfo("Sensor TPS", "Sinal alto do TPS.", "Powertrain"),
        "P0130" to DtcInfo("Sonda Lambda B1S1", "Circuito da sonda lambda (Banco 1 Sensor 1).", "Powertrain", possibleCauses = "Sonda defeituosa, fiação, vazamento de escape"),
        "P0131" to DtcInfo("Sonda Lambda B1S1", "Tensão baixa da sonda lambda (Banco 1 Sensor 1).", "Powertrain"),
        "P0132" to DtcInfo("Sonda Lambda B1S1", "Tensão alta da sonda lambda (Banco 1 Sensor 1).", "Powertrain"),
        "P0133" to DtcInfo("Sonda Lambda B1S1", "Resposta lenta da sonda lambda (Banco 1 Sensor 1).", "Powertrain"),
        "P0134" to DtcInfo("Sonda Lambda B1S1", "Nenhuma atividade detectada na sonda lambda (Banco 1 Sensor 1).", "Powertrain"),
        "P0135" to DtcInfo("Aquecedor Sonda Lambda", "Circuito do aquecedor da sonda lambda (Banco 1 Sensor 1).", "Powertrain"),
        "P0171" to DtcInfo("Mistura Pobre", "Sistema muito pobre (Banco 1). Excesso de ar ou falta de combustível.", "Powertrain", DTCLevel.WARNING, "Vazamento de vácuo, filtro de ar, bomba de combustível, injetores", "Verificar vazamentos e pressão de combustível"),
        "P0172" to DtcInfo("Mistura Rica", "Sistema muito rico (Banco 1). Excesso de combustível.", "Powertrain", DTCLevel.WARNING, "Injetores, sensor MAF, pressão de combustível alta"),
        "P0174" to DtcInfo("Mistura Pobre B2", "Sistema muito pobre (Banco 2).", "Powertrain"),
        "P0175" to DtcInfo("Mistura Rica B2", "Sistema muito rico (Banco 2).", "Powertrain"),

        // ---- Ignição / Misfire ----
        "P0300" to DtcInfo("Falha de Ignição", "Falha de ignição aleatória / múltiplos cilindros.", "Powertrain", DTCLevel.CRITICAL, "Velas, bobinas, combustível, compressão", "Verificar velas e bobinas"),
        "P0301" to DtcInfo("Falha Cilindro 1", "Falha de ignição detectada no cilindro 1.", "Powertrain", DTCLevel.CRITICAL),
        "P0302" to DtcInfo("Falha Cilindro 2", "Falha de ignição detectada no cilindro 2.", "Powertrain", DTCLevel.CRITICAL),
        "P0303" to DtcInfo("Falha Cilindro 3", "Falha de ignição detectada no cilindro 3.", "Powertrain", DTCLevel.CRITICAL),
        "P0304" to DtcInfo("Falha Cilindro 4", "Falha de ignição detectada no cilindro 4.", "Powertrain", DTCLevel.CRITICAL),
        "P0305" to DtcInfo("Falha Cilindro 5", "Falha de ignição detectada no cilindro 5.", "Powertrain", DTCLevel.CRITICAL),
        "P0306" to DtcInfo("Falha Cilindro 6", "Falha de ignição detectada no cilindro 6.", "Powertrain", DTCLevel.CRITICAL),
        "P0325" to DtcInfo("Sensor de Detonação", "Circuito do sensor de detonação (knock sensor).", "Powertrain"),
        "P0335" to DtcInfo("Sensor de Rotação", "Circuito do sensor de posição do virabrequim (CKP).", "Powertrain", DTCLevel.CRITICAL),
        "P0340" to DtcInfo("Sensor de Fase", "Circuito do sensor de posição do comando (CMP).", "Powertrain"),

        // ---- Emissões / Catalisador ----
        "P0420" to DtcInfo("Catalisador", "Eficiência do catalisador abaixo do limiar (Banco 1).", "Powertrain", DTCLevel.WARNING, "Catalisador degradado, sonda lambda", "Verificar catalisador e sondas"),
        "P0430" to DtcInfo("Catalisador B2", "Eficiência do catalisador abaixo do limiar (Banco 2).", "Powertrain"),
        "P0440" to DtcInfo("Sistema EVAP", "Sistema de controle de emissões evaporativas com mau funcionamento.", "Powertrain"),
        "P0441" to DtcInfo("Sistema EVAP", "Fluxo de purga incorreto do sistema EVAP.", "Powertrain"),
        "P0442" to DtcInfo("Sistema EVAP", "Pequeno vazamento detectado no sistema EVAP.", "Powertrain"),
        "P0455" to DtcInfo("Sistema EVAP", "Grande vazamento detectado no sistema EVAP (tampa do tanque?).", "Powertrain", recommendation = "Verificar tampa do tanque"),
        "P0456" to DtcInfo("Sistema EVAP", "Vazamento muito pequeno no sistema EVAP.", "Powertrain"),

        // ---- Velocidade / Marcha lenta ----
        "P0500" to DtcInfo("Sensor de Velocidade", "Sensor de velocidade do veículo (VSS) com mau funcionamento.", "Powertrain"),
        "P0505" to DtcInfo("Controle de Marcha Lenta", "Sistema de controle de marcha lenta com mau funcionamento.", "Powertrain"),
        "P0506" to DtcInfo("Marcha Lenta Baixa", "Rotação de marcha lenta menor que a esperada.", "Powertrain"),
        "P0507" to DtcInfo("Marcha Lenta Alta", "Rotação de marcha lenta maior que a esperada.", "Powertrain"),

        // ---- Bateria / Elétrico ----
        "P0560" to DtcInfo("Tensão do Sistema", "Tensão do sistema elétrico com mau funcionamento.", "Powertrain"),
        "P0562" to DtcInfo("Tensão Baixa", "Tensão do sistema baixa.", "Powertrain", DTCLevel.WARNING, "Bateria, alternador, conexões"),
        "P0563" to DtcInfo("Tensão Alta", "Tensão do sistema alta.", "Powertrain"),

        // ---- Híbrido / Toyota específico (comum em Corolla Hybrid) ----
        "P0A80" to DtcInfo("Bateria HV", "Substituir pacote de bateria híbrida (HV).", "Híbrido", DTCLevel.CRITICAL, "Células degradadas", "Diagnóstico em concessionária"),
        "P0A7F" to DtcInfo("Bateria HV", "Deterioração do pacote de bateria híbrida.", "Híbrido", DTCLevel.CRITICAL),
        "P0A94" to DtcInfo("Inversor", "Desempenho do conversor DC/DC.", "Híbrido", DTCLevel.WARNING),
        "P0AA6" to DtcInfo("Isolamento HV", "Falha de isolamento do sistema de alta tensão.", "Híbrido", DTCLevel.CRITICAL),
        "P0AFA" to DtcInfo("Bateria HV Baixa", "Tensão baixa do pacote de bateria híbrida.", "Híbrido", DTCLevel.WARNING),
        "P3000" to DtcInfo("Controle HV", "Mau funcionamento do sistema de controle da bateria HV.", "Híbrido", DTCLevel.CRITICAL),
        "P3004" to DtcInfo("Bateria HV", "Alta tensão do pacote de bateria híbrida.", "Híbrido", DTCLevel.WARNING),
        "P3011" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 1.", "Híbrido"),
        "P3012" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 2.", "Híbrido"),
        "P3013" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 3.", "Híbrido"),
        "P3014" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 4.", "Híbrido"),
        "P3015" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 5.", "Híbrido"),
        "P3016" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 6.", "Híbrido"),
        "P3017" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 7.", "Híbrido"),
        "P3018" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 8.", "Híbrido"),
        "P3019" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 9.", "Híbrido"),
        "P3020" to DtcInfo("Bloco Bateria HV", "Resistência alta no bloco de bateria 10.", "Híbrido"),

        // ---- Transmissão ----
        "P0700" to DtcInfo("Transmissão", "Sistema de controle da transmissão com mau funcionamento.", "Powertrain", DTCLevel.WARNING),
        "P0715" to DtcInfo("Sensor de Entrada", "Circuito do sensor de velocidade de entrada da transmissão.", "Powertrain"),
        "P0720" to DtcInfo("Sensor de Saída", "Circuito do sensor de velocidade de saída da transmissão.", "Powertrain"),
        "P0730" to DtcInfo("Relação de Marcha", "Relação de marcha incorreta.", "Powertrain", DTCLevel.WARNING),

        // ---- Chassis / ABS ----
        "C1201" to DtcInfo("ABS / ECU Motor", "Mau funcionamento na comunicação ECU motor ↔ ABS.", "Chassis"),
        "C1241" to DtcInfo("Tensão Baixa ABS", "Tensão baixa no sistema ABS.", "Chassis"),
        "C1336" to DtcInfo("Sensor de Aceleração", "Sensor de aceleração lateral com mau funcionamento.", "Chassis"),

        // ---- Rede ----
        "U0100" to DtcInfo("Comunicação ECU", "Perda de comunicação com o módulo de controle do motor (ECM/PCM).", "Network", DTCLevel.CRITICAL),
        "U0101" to DtcInfo("Comunicação TCM", "Perda de comunicação com o módulo de controle da transmissão (TCM).", "Network"),
        "U0121" to DtcInfo("Comunicação ABS", "Perda de comunicação com o módulo ABS.", "Network"),
        "U0140" to DtcInfo("Comunicação Body", "Perda de comunicação com o módulo de controle da carroceria (BCM).", "Network"),
        "U0155" to DtcInfo("Comunicação Cluster", "Perda de comunicação com o painel de instrumentos.", "Network"),

        // ---- Toyota / Corolla específicos comuns ----
        "P1349" to DtcInfo("VVT", "Sistema de variação de fase (VVT) com mau funcionamento.", "Powertrain", recommendation = "Verificar solenóide VVT e óleo"),
        "P1604" to DtcInfo("Partida", "Dificuldade de partida detectada.", "Powertrain"),
        "P1605" to DtcInfo("Partida", "Mau funcionamento no controle de partida.", "Powertrain"),
        "P2101" to DtcInfo("Acelerador Eletrônico", "Circuito do atuador do acelerador eletrônico.", "Powertrain", DTCLevel.WARNING),
        "P2111" to DtcInfo("Acelerador Eletrônico", "Sistema do acelerador eletrônico travado aberto.", "Powertrain", DTCLevel.CRITICAL),
        "P2112" to DtcInfo("Acelerador Eletrônico", "Sistema do acelerador eletrônico travado fechado.", "Powertrain", DTCLevel.CRITICAL),
        "P2122" to DtcInfo("Pedal Acelerador", "Sinal baixo do sensor de posição do pedal (APP).", "Powertrain"),
        "P2123" to DtcInfo("Pedal Acelerador", "Sinal alto do sensor de posição do pedal (APP).", "Powertrain"),
        "P2135" to DtcInfo("TPS Correlação", "Correlação entre sensores TPS A/B fora do esperado.", "Powertrain"),
        "P2195" to DtcInfo("Sonda Lambda", "Sinal da sonda lambda preso em mistura pobre (Banco 1 Sensor 1).", "Powertrain"),
        "P2196" to DtcInfo("Sonda Lambda", "Sinal da sonda lambda preso em mistura rica (Banco 1 Sensor 1).", "Powertrain"),

        // ---- Fiat / VW / GM comuns no Brasil ----
        "P0011" to DtcInfo("Comando de Válvulas", "Posição do comando de admissão adiantada (Banco 1).", "Powertrain"),
        "P0014" to DtcInfo("Comando de Válvulas", "Posição do comando de escape adiantada (Banco 1).", "Powertrain"),
        "P0016" to DtcInfo("Correlação CKP/CMP", "Correlação entre virabrequim e comando fora do esperado.", "Powertrain", DTCLevel.WARNING),
        "P0087" to DtcInfo("Pressão de Combustível", "Pressão do trilho de combustível muito baixa.", "Powertrain", DTCLevel.WARNING),
        "P0088" to DtcInfo("Pressão de Combustível", "Pressão do trilho de combustível muito alta.", "Powertrain"),
        "P0090" to DtcInfo("Regulador de Pressão", "Circuito do regulador de pressão de combustível.", "Powertrain"),
        "P0234" to DtcInfo("Turbo", "Condição de sobrealimentação (turbo) acima do esperado.", "Powertrain", DTCLevel.WARNING),
        "P0299" to DtcInfo("Turbo", "Condição de subalimentação (turbo) abaixo do esperado.", "Powertrain"),
        "P0401" to DtcInfo("EGR", "Fluxo insuficiente no sistema EGR.", "Powertrain"),
        "P0402" to DtcInfo("EGR", "Fluxo excessivo no sistema EGR.", "Powertrain"),
        "P0480" to DtcInfo("Ventoinha", "Circuito de controle da ventoinha de arrefecimento 1.", "Powertrain"),
        "P0481" to DtcInfo("Ventoinha", "Circuito de controle da ventoinha de arrefecimento 2.", "Powertrain")
    )

    fun lookup(code: String): DtcInfo {
        val normalized = code.trim().uppercase()
        library[normalized]?.let { return it }

        // Fallback genérico por prefixo
        return when {
            normalized.startsWith("P01") -> DtcInfo("Combustível / Sensores de Ar", "Código relacionado ao sistema de medição de ar/combustível.", "Powertrain")
            normalized.startsWith("P02") -> DtcInfo("Injetores / Combustível", "Código relacionado a injetores ou sistema de combustível.", "Powertrain")
            normalized.startsWith("P03") -> DtcInfo("Ignição / Misfire", "Código relacionado a ignição ou falha de combustão.", "Powertrain", DTCLevel.WARNING)
            normalized.startsWith("P04") -> DtcInfo("Emissões", "Código relacionado ao controle de emissões.", "Powertrain")
            normalized.startsWith("P05") -> DtcInfo("Velocidade / Marcha Lenta", "Código relacionado a velocidade do veículo ou controle de marcha lenta.", "Powertrain")
            normalized.startsWith("P06") -> DtcInfo("Circuito da ECU", "Código relacionado a circuitos internos da ECU.", "Powertrain")
            normalized.startsWith("P0A") || normalized.startsWith("P30") -> DtcInfo("Sistema Híbrido", "Código relacionado ao sistema híbrido / alta tensão.", "Híbrido", DTCLevel.WARNING)
            normalized.startsWith("C") -> DtcInfo("Chassis / ABS", "Código relacionado a chassis, ABS ou controle de tração.", "Chassis")
            normalized.startsWith("B") -> DtcInfo("Carroceria", "Código relacionado a carroceria ou conforto.", "Body")
            normalized.startsWith("U") -> DtcInfo("Rede / Comunicação", "Código relacionado a comunicação entre módulos.", "Network", DTCLevel.WARNING)
            else -> DtcInfo("Código de Diagnóstico", "Código $normalized detectado. Consulte o manual do veículo para detalhes específicos.", "Desconhecido", DTCLevel.INFO)
        }
    }

    fun allCodes(): List<String> = library.keys.sorted()
    fun count(): Int = library.size
}
