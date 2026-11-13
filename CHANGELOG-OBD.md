# Alterações (OBD + painel)

- ObdManager reescrito: leitura até o prompt `>`, init só conclui com resposta da ECU (0100),
  PIDs suportados via bitmap, PIDs rápidos/lentos, reconexão automática, fallback de socket (SPP seguro,
  inseguro, RFCOMM canal 1). Valores indisponíveis = "--" (nunca inventados).
- Consumo real: 015E (L/h) ou MAF 0110 -> km/L instantâneo e médio; "~" quando estimado pelo INMETRO.
- Etanol (0152) agora é lido; "Turbo" removido da tela inicial.
- Tela inicial: consumo médio e instantâneo abaixo de velocímetro/RPM; Bateria 12 V e SOC substituem Turbo/G-Force.
- SOC: PID padrão 015B; senão consulta proprietária opcional em VehicleProfile.socQuery (null por padrão).
- ViewModel: força G do acelerômetro preservada em modo OBD; sair do demo para o simulador; trocar perfil atualiza a conexão.
- Tela ligada durante o uso; removida permissão BLUETOOTH_SCAN (não usada).

# Perfis, códigos de erro e diagnóstico
- Perfis: `model/Vehicle.kt` (81 veículos) substitui o antigo VehicleProfile.kt (havia declaração duplicada).
  Membros de compatibilidade adicionados (name, tankCapacityLiters, hasTurbo, maxRpm, maxSpeed, inmetro*, socQuery).
- `InmetroConsumption`: agora escolhe a chave mais específica (antes "corolla" vencia "corolla cross"); híbrido sem entrada própria usa o padrão híbrido.
- `DtcLibrary`: P3011-P3024 corrigidos (bloco da bateria HV "fraco"), P3004/P3006 corrigidos/adicionados; `isKnown()`.
- Diagnóstico: modos 03/07/0A/04 + MIL (PID 0101) em `ObdManager`; parser em `obd/DtcParser.kt`;
  tela `DiagnosticsScreen`; aviso automático no painel quando a luz acende ou surgem códigos; ícone com ponto vermelho na barra.
- Settings: busca na lista de perfis. Testes: `DtcParserTest`, `ExampleUnitTest` atualizado.

# Sensores "sem leitura" (PIDs não anunciados pela ECU)
- `ObdManager`: sondagem de PIDs Mode 01 que a ECU não declara na lista de suportados (mesma lógica já usada
  para 0152 e 015B). Cobre óleo (015C), STFT (0106), LTFT (0107), pressão do combustível (010A),
  pressão absoluta do trilho (0159), avanço (010E), EGR (012C), EVAP (0132) e pedais D/E (0149/014A).
- Uma consulta a cada 3 ciclos, no máximo 3 tentativas por PID, contadores próprios. Se responder, o sensor
  entra na rotação normal; se não, segue "sem leitura". Sensores já declarados pela ECU não são alterados.
