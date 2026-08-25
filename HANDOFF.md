# Entrega — Painel de Ofertas (Android)

Documento para a equipe de desenvolvimento da **LedBlock Indicadores Inteligentes**.

Este é um **porte Android nativo** (Kotlin + Jetpack Compose) do software Windows
"Painel de Ofertas". Foi construído **fiel ao protocolo** do app original — a
lógica de comunicação e o formato dos dados foram reproduzidos byte-a-byte e
cobertos por testes automatizados. O objetivo é dar à LedBlock uma base sólida,
limpa e testada para oferecer o Painel de Ofertas também no Android.

> **Leia primeiro:** nada neste app foi testado contra um painel físico. Tudo foi
> derivado do código-fonte Delphi original e validado com testes e um painel
> simulado. A seção **"O que precisa da validação de vocês"** lista exatamente o
> que confirmar em bancada.

## 📚 Para a equipe de desenvolvimento

Documentação de onboarding — pensada para alguém que nunca viu este código:

| Documento | O que traz |
|---|---|
| 🚦 **[docs/COMECE-AQUI.md](docs/COMECE-AQUI.md)** | Onboarding em ~30 min: glossário do domínio, montagem do ambiente, o caminho completo de um preço (do formulário ao painel), roteiro de leitura do código e **as armadilhas** que fazem perder horas |
| 🏗️ **[docs/ARQUITETURA.md](docs/ARQUITETURA.md)** | Mapa **arquivo por arquivo**: responsabilidade de cada um, o que pode depender de quê |
| 📡 **[docs/PROTOCOLO.md](docs/PROTOCOLO.md)** | Especificação completa do protocolo, com as rotinas do `Ofertas.pas` que originaram cada parte |
| 🍳 **[docs/RECEITAS.md](docs/RECEITAS.md)** | 12 tarefas passo a passo ("adicionar um campo", "depurar envio que falha", "ler o `.alb` gerado") |

---

## Estado da entrega

| Camada | Estado |
|---|---|
| Protocolo (codec binário, CRC, `.alb`, normalização de texto) | ✅ **Validado** byte-a-byte contra os arquivos reais + testes |
| Fontes `.flb` + renderização do preview | ✅ Validado (reaproveita os `.flb` originais) |
| Máquina de transferência (Enviar/Receber em blocos) | ✅ Testada com painel simulado |
| Descoberta de painéis + liveness (UDP) | ✅ Implementado |
| Interface completa (Editar, Enviar, Painéis, Agenda, Config) | ✅ Funcional |
| **USB HID + configuração WiFi (comandos AT)** | ⚠️ **Fiel à spec — falta validar no painel físico** |
| **Sensor de luz / auto-brilho, sincronismo por CRC, diagnóstico** | ⚠️ **Fiel à spec — falta validar no painel físico** |

**~33 testes de unidade** verdes (`./gradlew test`).

### Interoperabilidade
Os álbuns são gravados no mesmo formato `.alb` do app Windows (cabeçalho de 4
linhas + blocos `:`/`;`), então o conteúdo criado no Android abre no Windows e
vice-versa.

---

## Recursos além do app Windows

Durante o porte, três capacidades do hardware apareceram no fonte mas **não
estavam expostas** na interface original. Foram implementadas (e precisam de
validação):

1. **Sensor de luz / auto-brilho** — o byte de brilho tem um flag no **bit 128**
   (`SalvaBrilho`: `valor + 128` quando o sensor está ativo; daí `INICIAR=228`
   = 128+100). Com ele ligado, o painel ajusta o brilho sozinho conforme a luz do
   ambiente. O app tem um toggle por painel e mostra a leitura do sensor ao vivo
   (campo 7 do `STATUS=`).
2. **Sincronismo por CRC** — o `STATUS=` devolve o CRC do conteúdo gravado no
   painel (campo 6). O app compara com o CRC do álbum enviado e mostra um selo
   **✓ Sincronizado** / **⚠ Desatualizado** por painel.
3. **Diagnóstico do dispositivo** — leitura *read-only* de identidade: descritores
   USB (fabricante/produto/série + VID→chip) e, via ESP-AT, `AT+GMR` (versão do
   firmware) e `AT+CIFSR` (MAC/IP).

Também foram adicionados: **auto-conexão** ao abrir (varredura da sub-rede),
**barra de memória** (avisa se o álbum cabe antes de enviar), **modelos de oferta**
e **modo retrato** no editor (layout — a rotação real depende do firmware).

---

## O que precisa da validação de vocês (têm o hardware)

Tudo que dá para provar sem o painel foi provado. O que só o **painel físico**
confirma:

1. **USB HID** — o controlador é um Microchip (VID `0x04D8` / PID `0xF002`). O
   report de 64 bytes, os endpoints e o handling do ReportID foram implementados
   conforme extraído do fonte Delphi (`EnviarUSB`/`HIDCtrlDeviceData`).
2. **Entrada no WiFi (ESP-AT)** — a sequência `EXIT → CWMODE=1 → CWJAP → CWDHCP/CIPSTA
   → CIPMUX=0 → CIPMODE=1 → SAVETRANSLINK → CIPSEND`; as respostas reais do
   firmware podem exigir ajuste de timeout/parsing.
3. **Sensor/auto-brilho, CRC ao vivo, AT+GMR/CIFSR** — ver seção acima.

### Roteiro de validação sugerido

| # | Teste | Esperado |
|---|---|---|
| 1 | Painel ligado na mesma rede → abrir o app | Auto-conecta: o painel aparece na aba **Painéis** sem apertar nada |
| 2 | Aba **Editar** → montar oferta → **Salvar e enviar** | Conteúdo aparece correto no painel |
| 3 | Após o envio, olhar o card do painel | Selo deve virar **✓ Sincronizado** (CRC bate) |
| 4 | Aba **Painéis** → mexer no brilho → **Aplicar** | Brilho do painel muda |
| 5 | Ligar **Auto-brilho (sensor)** → **Aplicar** → cobrir/iluminar o sensor | Painel se ajusta sozinho; leitura do sensor muda no card |
| 6 | Conectar por **USB (OTG)** → aba **Config** | Mostra chip/fabricante/série do dispositivo |
| 7 | **Config** → "Ler firmware & MAC do módulo Wi-Fi" | Mostra versão do firmware (GMR) e MAC |
| 8 | Aba **Painéis** → "Configurar WiFi do painel" (com USB) | Lista redes e entra na rede escolhida |
| 9 | Aba **Enviar** → **Receber** | Baixa o conteúdo do painel e salva como álbum |
| 10 | **Config → Diagnóstico do protocolo** | ✅ OK — 49 bytes, CRC 0xD644 (núcleo íntegro) |

Se algo falhar, a aba **Config** e as mensagens de status mostram o que foi
enviado/recebido — útil para ajustar parsing/timeout.

---

## Como compilar

Requisitos: **Android Studio** (Ladybug+), que já traz JDK, Gradle e SDK.
`Open` a pasta do projeto → Gradle Sync → **Run ▶**. Ou:

```bash
./gradlew assembleDebug   # APK de teste (auto-assinado com a chave de debug)
./gradlew test            # testes de unidade
```

Para distribuir, gerar um **APK/AAB de release assinado**: criar um keystore
(`keytool`) e adicionar um `signingConfig` no `app/build.gradle.kts` — ver
`README.md`. O APK entregue é **debug** (instala com "fontes desconhecidas").

`minSdk 24` (Android 7) · `targetSdk 35` · Kotlin 2.0 · Compose BOM 2024.10

## Arquitetura (resumo)

Camadas limpas, sem dependências circulares:

```
protocol/  render/  net/  transfer/  usb/  discovery/  data/  editor/  ui/(+vm)
```

- `protocol/` é **Kotlin puro** (sem Android) — 100% testável offline.
- Estado de tela em **ViewModels** (escopo de Activity) — sobrevive a rotação/troca de aba.
- Dados observáveis (`StateFlow`) para álbuns, painéis e preferências.
- Detalhes completos, referência do protocolo e limitações no **`README.md`**.

## Continuidade / melhorias mapeadas (não bloqueiam)

- **Agendamento em segundo plano** (com o app fechado) via `WorkManager`/
  `AlarmManager`. Hoje o agendamento roda apenas com o app aberto. Não foi
  implementado porque exige validação em aparelho real ao longo de dias (Doze
  mode + otimizações agressivas de bateria de alguns fabricantes).
- Otimização de tamanho do APK (R8/minify) após validação em campo.
- Edição por toque direto na prévia (WYSIWYG).

## Perguntas para a LedBlock (destravariam novas funções)

O app já aciona **tudo** que o protocolo extraído do fonte permite. Para ir além,
precisaríamos de informação de vocês:

1. Existe **especificação completa** dos comandos do controlador do painel (além
   de `STATUS`, `INICIAR=`, `ONOFF=`, `APAGAR=`, `DADO=`, `CARREGAR`, `LIDO=`,
   `ONLINE=`, `SERVIDOR=`, `SENHA=`)?
2. O LED é **monocromático** ou existem modelos **RGB**? (o app hoje assume mono e
   deixa a cor da prévia configurável só para combinar visualmente)
3. O firmware suporta **rotação 90°** (painel em pé)? O app tem um modo retrato de
   *layout*, mas a rotação real depende do controlador.
4. Que **efeitos/animações** o firmware implementa além de Padrão / Pisca-Inverte /
   Pisca-Padrão (`ONLINE=<índice>`)?
5. Qual a **capacidade de memória** por modelo de painel? (o app calcula pelo
   `STATUS`, mas seria bom validar)

---

O código foi revisado por duas auditorias (correção/robustez e UI/UX); os pontos
achados foram corrigidos. Verificados contra o `Ofertas.pas` original: CRC,
extração de campos, tabela de duração, campos do `STATUS=`, layout dos pacotes
UDP e a assimetria de endianness do USB.
