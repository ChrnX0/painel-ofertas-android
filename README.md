<div align="center">

# Painel de Ofertas — Android

**Porte Android nativo do software Windows "Painel de Ofertas"**
Controle de painéis de LED de ofertas/preços por **Wi-Fi** e **USB**.

[![Android](https://img.shields.io/badge/Android-7.0%2B%20(API%2024)-3DDC84?logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)](#)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)](#)
[![Testes](https://img.shields.io/badge/testes-~33%20verdes-2E7D32)](#testes)
[![Status](https://img.shields.io/badge/status-aguardando%20valida%C3%A7%C3%A3o%20em%20hardware-FB8C00)](#status-do-projeto)

</div>

---

## Sobre

App Android nativo (Kotlin + Jetpack Compose) que reproduz o software Windows
"Painel de Ofertas" para controlar **painéis de LED de ofertas/preços** de loja.
A comunicação acontece por **Wi-Fi/UDP** e por **USB HID** (configuração do módulo
Wi-Fi ESP-AT através de uma ponte Microchip).

O protocolo foi **extraído do código-fonte Delphi original** e validado
byte-a-byte contra os arquivos reais do app Windows — o núcleo (codec, CRC,
fontes bitmap, máquina de transferência) roda coberto por testes automatizados.

**Interoperável:** os álbuns são gravados no mesmo formato `.alb` do app Windows,
então o conteúdo criado no Android abre no PC e vice-versa.

> ### ⚠️ Status do projeto
> Este app **ainda não foi testado contra um painel físico**. Tudo foi derivado do
> código-fonte original e validado com testes de unidade e um painel simulado.
> O documento **[HANDOFF.md](HANDOFF.md)** traz um **roteiro de validação de 10
> passos** para conferir em bancada.

---

## Funcionalidades

| Aba | O que faz |
|---|---|
| **Editar** | Montador de sequência: um álbum com vários quadros (**Oferta** de preço e/ou **Mensagem** de texto) — reordenar, duplicar, tempo por quadro, borda, orientação. **Prévia de LED ao vivo** no tamanho real do painel. **Modelos** prontos (Açougue, Hortifruti, Bebidas…) e leitura do preço formatado enquanto digita. |
| **Enviar** | Envia o álbum por Wi-Fi (IP) ou USB com **anel de progresso** e trava anti-duplo-toque; **barra de memória** avisa se o conteúdo cabe no painel antes de mandar; também **Recebe** o conteúdo atual do painel. |
| **Painéis** | **Auto-conexão** ao abrir (varre a sub-rede sozinho), status online/instável/offline, **brilho**, **auto-brilho por sensor de luz**, **selo de sincronismo (CRC)**, ligar/desligar, identificar e renomear. Configura o **Wi-Fi do painel via USB** (ler/escanear redes, entrar na rede). |
| **Agenda** | Agenda o envio de um álbum para um painel em data/hora, com opção **Diariamente** e brilho por tarefa. |
| **Config** | Tema (Sistema/Claro/Escuro), cor do LED na prévia, efeito global das telas, IP local, senha de transmissão, **Diagnóstico do dispositivo** (chip, firmware, MAC) e **Diagnóstico do protocolo** (compila os arquivos reais no aparelho e confere o CRC). |

### Recursos além do app Windows

Três capacidades do hardware apareceram no código-fonte original mas **não estavam
expostas** na interface do app Windows. Foram implementadas aqui:

- 🔆 **Sensor de luz / auto-brilho** — o byte de brilho tem um flag no **bit 128**;
  com ele ativo, o painel ajusta o brilho sozinho conforme a luz do ambiente. O
  app mostra a leitura do sensor ao vivo.
- ✅ **Sincronismo por CRC** — o painel reporta o CRC do conteúdo gravado nele; o app
  compara com o que foi enviado e mostra **✓ Sincronizado** / **⚠ Desatualizado**.
- 🔎 **Diagnóstico de hardware** — leitura *read-only* de identidade: descritores USB
  (fabricante/produto/série + VID → chip) e, via ESP-AT, `AT+GMR` (firmware) e
  `AT+CIFSR` (MAC/IP).

**Robustez:** o estado de edição sobrevive à troca de aba e à rotação (ViewModels);
transferências rodam num escopo que não é cancelado ao navegar; erros de rede/USB
não derrubam o app; o texto é sanitizado (acentos viram os placeholders corretos;
caracteres inválidos não corrompem o painel).

---

## Começando

### Pré-requisitos
- **Android Studio** (Ladybug 2024.2+) — já inclui JDK, Gradle e Android SDK
- Aparelho **Android 7.0+** (API 24). Para o caminho **USB**, precisa de **USB OTG**

### Compilar e rodar
1. Android Studio → **Open** → selecione a pasta do projeto
2. Aguarde o **Gradle Sync** (na 1ª vez baixa Gradle e dependências)
3. **Run ▶** no aparelho

```bash
./gradlew assembleDebug     # gera app/build/outputs/apk/debug/app-debug.apk
./gradlew test              # roda os testes de unidade
```

### APK de release (assinado)
O APK de debug serve para testes. Para distribuir:

```bash
keytool -genkey -v -keystore painel.jks -keyalg RSA -keysize 2048 -validity 10000 -alias painel
```

Depois adicione um `signingConfigs` no `app/build.gradle.kts`, referencie em
`buildTypes { release { signingConfig = ... } }` e rode `./gradlew assembleRelease`.

> Guarde o keystore e as senhas com segurança — é a identidade do app; sem ele não
> dá para publicar atualizações.

---

## Testes

`./gradlew test` — ~33 testes de unidade cobrindo:

| Teste | Cobre |
|---|---|
| `protocol/BinaryCodecTest` | Compila `nelPai` (49 B, CRC `0xD644`) e `mensagem` (111 B, CRC `0x06A2`) byte-a-byte; round-trip; CRC-16/XMODEM; sanitização de texto |
| `protocol/AlbumTest`, `ProtocolFieldsTest` | Modelo `.alb`, campos do quadro, tabela de duração |
| `render/FlbFontTest`, `AccentMapTest`, `OfertaLayoutTest` | Fontes bitmap, mapa de acentos, layout da oferta |
| `transfer/TransferEngineTest` | Envio/recebimento em blocos contra um painel simulado |
| `editor/FrameDraftTest` | Round-trip dos rascunhos de quadro |

---

## Arquitetura

```
br.com.painelofertas/
├── protocol/   # Codec binário, CRC, modelo .alb, normalização de texto (Kotlin puro)
├── render/     # Fontes .flb, layout da Oferta, renderização da prévia
├── net/        # UDP (socket, pacotes, parser), Encriptor, LocalIp
├── transfer/   # Máquina de Enviar/Receber em blocos (coroutines)
├── usb/        # USB HID (device, link, configurador Wi-Fi ESP-AT)
├── discovery/  # Varredura da sub-rede + liveness dos painéis
├── data/       # Repositórios: painéis, álbuns (observável), config, agenda
├── editor/     # FrameDraft (rascunho editável de quadro)
├── AppContainer / PainelApp   # injeção de dependências + ciclo de vida
└── ui/         # Compose: MainActivity + theme + components + screens + vm
```

- `protocol/` é **Kotlin puro** (sem dependência de Android) → 100% testável offline
- **Estado** em ViewModels de escopo de Activity — sobrevive a rotação/troca de aba
- **Dados observáveis** (`StateFlow`) para álbuns, painéis e preferências

---

## Protocolo (resumo)

| Item | Detalhe |
|---|---|
| **Hardware** | Módulo Wi-Fi **ESP8266** (firmware ESP-AT) atrás de ponte **USB-HID Microchip** (VID `0x04D8`, PID `0xF002`) |
| **UDP** | App → painel na porta **17065** (texto + `CR`); respostas na **17066** |
| **Quadro** | `:TYPE;ADSIZE;DUR;F3;F4;F5;F6;ENABLE;` |
| **Registro** | `;LEN;SLOT;ROW;COL;FONT;TEXTO` — ou `;5;0;R1;C1;R2;C2;` para gráfico |
| **Transferência** | Blocos de 60 bytes, *stop-and-wait*, ack `NEXT=<offset>` |
| **Comandos** | `STATUS`, `INICIAR=<brilho>`, `ONOFF=0`, `APAGAR=`, `DADO=`, `CARREGAR`, `LIDO=`, `ONLINE=<efeito>`, `SERVIDOR=<ip>`, `SENHA=` |
| **Brilho** | 0–100; **+128 liga o sensor de luz** (auto-brilho) |
| **CRC** | CRC-16/XMODEM (poly `0x1021`) sobre `bytes[0..len-3]` |

Referência completa e roteiro de validação: **[HANDOFF.md](HANDOFF.md)**.

---

## Limitações conhecidas

- **Validação em hardware pendente** — USB HID, entrada no Wi-Fi (ESP-AT), sensor
  de luz e CRC ao vivo foram implementados fiéis à especificação extraída, mas só
  o painel físico confirma 100%.
- **Agendamento em segundo plano** — hoje o agendador dispara apenas com o app
  aberto. Disparar com o app fechado exigiria `WorkManager`/`AlarmManager` e
  validação em campo (Doze mode + otimizações de bateria de fabricantes).
- **Rotação do painel** — o app tem um modo retrato de *layout*; a rotação real da
  imagem depende do firmware do controlador.

---

## Propriedade e marca

**Painel de Ofertas** é um produto da **LedBlock Indicadores Inteligentes**
([ledblock.com.br](https://www.ledblock.com.br)) — a marca, o logotipo, o hardware
e o protocolo do painel pertencem à empresa.

Este repositório contém um **porte Android** desenvolvido de forma independente a
partir do software Windows original, com o objetivo de ser entregue à LedBlock.
Não é um produto oficial da empresa e não há vínculo comercial. Todos os direitos
sobre a marca e o produto permanecem com a LedBlock.
