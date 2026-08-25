# 🍳 Receitas — como fazer as coisas

> Tarefas comuns, passo a passo, com os arquivos exatos. Se você nunca leu o
> projeto, veja antes o [COMECE-AQUI.md](COMECE-AQUI.md).

| | |
|---|---|
| [1. Gerar uma versão para instalar](#1-gerar-uma-versão-para-instalar) | [7. Adicionar um comando novo ao painel](#7-adicionar-um-comando-novo-ao-painel) |
| [2. Mudar um texto da interface](#2-mudar-um-texto-da-interface) | [8. Adicionar uma aba nova](#8-adicionar-uma-aba-nova) |
| [3. Mudar cores / tema](#3-mudar-cores--tema) | [9. Adicionar um teste](#9-adicionar-um-teste) |
| [4. Adicionar um campo na Oferta](#4-adicionar-um-campo-na-oferta) | [10. Depurar: o painel não aparece](#10-depurar-o-painel-não-aparece) |
| [5. Mudar o layout da Oferta](#5-mudar-o-layout-da-oferta) | [11. Depurar: envio falha](#11-depurar-o-envio-falha) |
| [6. Adicionar uma configuração que persiste](#6-adicionar-uma-configuração-que-persiste) | [12. Ler o `.alb` gerado](#12-ler-o-alb-gerado) |

---

## 1. Gerar uma versão para instalar

> ⚠️ **A pegadinha nº 1 do projeto.** Se você não aumentar o `versionCode`, o
> Android **não substitui** o app instalado e sua mudança "não aparece".

**`app/build.gradle.kts`**
```kotlin
versionCode = 27          // ← SEMPRE +1
versionName = "0.25.0"    // ← rótulo legível (aparece em Config → Sobre)
```

```bash
./gradlew assembleDebug
# APK em: app/build/outputs/apk/debug/app-debug.apk
```

Instale por cima (não precisa desinstalar). Confira a versão em **Config → Sobre**
para ter certeza de que é o build novo.

---

## 2. Mudar um texto da interface

Os textos estão **direto no Compose** (não há `strings.xml` completo — o app é
monolíngue em pt-BR).

Ache com busca global no texto exato:
```bash
grep -rn "Salvar e enviar" app/src/main/java/
```

Arquivos por tela:

| Tela | Arquivo |
|---|---|
| Editar | `ui/screens/EditarScreen.kt` |
| Enviar | `ui/screens/EnviarScreen.kt` |
| Painéis | `ui/screens/PaineisScreen.kt` |
| Agenda | `ui/screens/AgendaScreen.kt` |
| Config | `ui/screens/ConfigScreen.kt` |
| Nome das abas | `ui/MainActivity.kt` → `enum class Destino` |

---

## 3. Mudar cores / tema

**`ui/theme/Color.kt`** — a paleta. Duas listas completas: `Dark*` e `Light*`.

> ⚠️ **Sempre mexa nas duas.** Se você trocar só a escura, o tema claro fica errado.

```kotlin
val DarkPrimary = Color(0xFF4AA3FF)      // azul de acento (escuro)
val LightPrimary = Color(0xFF1565C0)     // azul de acento (claro)
val DarkSecondaryContainer = Color(0xFF143A5C)  // chips/abas selecionados
```

> 💡 `secondaryContainer` é o que colore **chips e a aba selecionada**. Se você
> deixar o padrão do Material, ele vira **lavanda** — foi exatamente o que dava
> "cara de app genérico" e por isso está definido explicitamente.

**Outros arquivos de aparência:**

| Quero mudar | Arquivo |
|---|---|
| Arredondamento de cards/botões | `ui/theme/Theme.kt` → `AppShapes` · `ui/components/Kit.kt` → `ButtonShape` |
| Fontes | `ui/theme/Type.kt` (Archivo + IBM Plex Mono, em `res/font/`) |
| Cor do LED na prévia | `ui/theme/Color.kt` → `LedColors` (o usuário escolhe em Config) |
| Moldura da prévia | `ui/components/Kit.kt` → `LedBezel` |
| Como o LED é desenhado | `ui/components/PanelPreview.kt` |

---

## 4. Adicionar um campo na Oferta

Exemplo: um campo **"Validade"**.

**Passo 1 — o dado.** `render/OfertaLayout.kt` → `data class OfertaSpec`
```kotlin
data class OfertaSpec(
    val cabecalho: String = "",
    // …
    val validade: String = "",     // ← novo
)
```

**Passo 2 — desenhar.** No mesmo arquivo, em `OfertaLayout.build()`, escolha um
**slot livre** (veja [slots ocupados](PROTOCOLO.md#slots-conhecidos)) e posicione:
```kotlin
centered(spec.validade, F_MICRO, 78, 8)   // texto, fonte, linha, slot
```

**Passo 3 — a interface.** `ui/screens/EditarScreen.kt` → `OfeForm()`
```kotlin
OutlinedTextField(
    s.validade, { set(s.copy(validade = it)) },
    label = { Text("Validade") }, singleLine = true,
    modifier = Modifier.fillMaxWidth(),
)
```

**Passo 4 — o teste.** `app/src/test/.../render/OfertaLayoutTest.kt`: monte um
`OfertaSpec` com o campo e verifique que aparece um `PanelRecord.Text` com o slot certo.

✅ **Não precisa mexer no `BinaryCodec`** — ele serializa qualquer registro. É a
vantagem de o layout produzir `PanelRecord` genéricos.

---

## 5. Mudar o layout da Oferta

Tudo em **`render/OfertaLayout.kt`**, na função `build()`. É código linear, leia de cima a baixo.

| Quero… | Onde |
|---|---|
| Mudar as fontes por tamanho do preço | bloco `val reaisFont = when { reais.length <= 2 -> F_GRANDE … }` |
| Mudar a altura do preço | `val reaisRow = 26` |
| Mudar a barra sob o preço | `val barRow = …` + `PanelRecord.Graphic(...)` |
| Mudar onde ficam medida/auxiliar | `val infoRow = …` |
| Mudar o rodapé | `centered(spec.rodape, F_MICRO, 84, 6)` |

**Como centralizar** (o padrão do arquivo):
```kotlin
val x = ((panelW - fonts.font(fonte).measure(texto)) / 2).coerceAtLeast(1)
```

> 💡 A prévia usa **o mesmo** `PanelFrame` que vai para o painel. Mudou aqui, mudou
> nos dois. Rode o app e olhe a prévia — é o feedback mais rápido.

---

## 6. Adicionar uma configuração que persiste

Exemplo: um ajuste "modo compacto".

**Passo 1 —** `data/Stores.kt` → `SettingsStore`:

```kotlin
// simples (lido sob demanda):
var modoCompacto: Boolean
    get() = prefs.getBoolean("modoCompacto", false)
    set(v) = prefs.edit().putBoolean("modoCompacto", v).apply()

// OU observável (a UI reage na hora — use quando muda a aparência):
private val _modoCompacto = MutableStateFlow(prefs.getBoolean("modoCompacto", false))
val modoCompacto: StateFlow<Boolean> = _modoCompacto.asStateFlow()
fun setModoCompacto(v: Boolean) {
    prefs.edit().putBoolean("modoCompacto", v).apply()
    _modoCompacto.value = v
}
```

**Passo 2 —** `ui/screens/ConfigScreen.kt`:
```kotlin
val modoCompacto by container.settings.modoCompacto.collectAsState()
// …
Switch(modoCompacto, { container.settings.setModoCompacto(it) })
```

> 💡 Use **`StateFlow`** quando a mudança precisa refletir na hora em outra tela
> (é o caso de `themeMode` e `ledColor`). Para o resto, o `var` simples basta.

---

## 7. Adicionar um comando novo ao painel

> ⚠️ Só adicione comandos que **existam no firmware**. Inventar comando pode travar
> o controlador. Veja [PROTOCOLO.md §11](PROTOCOLO.md#11-configuração-wi-fi-esp-at).

**Enviar** (qualquer lugar com um `PanelLink`):
```kotlin
scope.launch { UdpLink(ip, container.udp).sendText("MEUCOMANDO=42") }
```

**Receber uma resposta nova** — `net/PanelMessage.kt`:

1. Declare o tipo:
```kotlin
data class MinhaResposta(val valor: Int) : PanelMessage
```
2. Reconheça no `parse()`:
```kotlin
t.startsWith("MEUCMD=") -> MinhaResposta(t.removePrefix("MEUCMD=").trim().toIntOrNull() ?: 0)
```
3. Trate onde importa (ex.: `discovery/PanelDiscovery.kt` → `handle()`)

---

## 8. Adicionar uma aba nova

**`ui/MainActivity.kt`:**

```kotlin
private enum class Destino(val label: String, val icon: ImageVector) {
    EDITAR("Editar", Icons.Filled.Edit),
    // …
    RELATORIO("Relatório", Icons.Filled.Assessment),   // ← nova
}
```

E no `when` do `AnimatedContent`:
```kotlin
Destino.RELATORIO -> RelatorioScreen()
```

Crie `ui/screens/RelatorioScreen.kt` seguindo o padrão das outras:

```kotlin
@Composable
fun RelatorioScreen() {
    val container = rememberContainer()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            SectionLabel("Análise")
            Text("Relatório", style = MaterialTheme.typography.headlineSmall)
        }
        // …
    }
}
```

> 💡 5 abas já é o limite confortável de uma `NavigationBar`. Acima disso, considere
> agrupar em vez de acrescentar.

---

## 9. Adicionar um teste

`app/src/test/java/br/com/painelofertas/…` — JUnit puro, roda na JVM (rápido).

```kotlin
class MeuTeste {
    @Test fun `codec preserva o texto`() {
        val album = Album(name = "T", frames = listOf(/* … */))
        val r = album.compile()
        assertEquals(49, r.consumo)
        assertEquals(0xD644, r.crc)
    }
}
```

```bash
./gradlew test                                    # tudo
./gradlew test --tests "*BinaryCodecTest*"        # uma classe
```

Relatório legível: `app/build/reports/tests/testDebugUnitTest/index.html`

> 🔒 **Mudou algo em `protocol/`? Teste é obrigatório** — é o que garante a
> compatibilidade com o app Windows. Compare **bytes**, não comportamento.

---

## 10. Depurar: o painel não aparece

Siga na ordem:

| # | Verifique | Como |
|---|---|---|
| 1 | Celular e painel na **mesma rede**? | Redes de visitante e faixas 5 GHz separadas costumam isolar dispositivos |
| 2 | O **IP local** está certo? | Aba **Painéis** → campo "IP local". Deve ser o IP do celular na rede (ex.: `192.168.0.x`) |
| 3 | A sub-rede bate? | A varredura é **/24** — só encontra painéis em `192.168.0.*` se o celular for `192.168.0.*` |
| 4 | O roteador bloqueia? | Muitos roteadores têm **"isolamento de clientes" / AP isolation**. Desligue |
| 5 | O painel foi configurado? | Ele precisa ter recebido `AT+SAVETRANSLINK` apontando para **este** celular. Reconfigure por USB |
| 6 | Force uma varredura | Aba **Painéis** → **Procurar** |

> 💡 O `SAVETRANSLINK` grava **o IP do aparelho** que configurou. Se o celular
> mudou de IP (DHCP), o painel manda as respostas para o endereço antigo. Solução:
> IP fixo/reserva no roteador, ou reconfigurar por USB.

---

## 11. Depurar: o envio falha

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| "Painel não respondeu ao apagar" | Painel não recebe, ou **senha de transmissão** errada | Confira a rede; teste com senha desligada (Config → Segurança) |
| Para no meio, "Falha no bloco N" | Perda de pacote / Wi-Fi fraco | Aproxime; teste por USB. Já são **50 tentativas** por bloco |
| Envia tudo mas o painel não muda | Faltou o `INICIAR=` chegar, ou o quadro está com `ENABLE=0` | Veja se algum quadro está desabilitado no editor |
| `NEGADO` | Senha de transmissão incorreta | Config → Segurança → confira a senha |
| Selo continua **⚠ Desatualizado** | CRC do painel ≠ CRC enviado | O conteúdo não gravou. Reenvie e observe |
| "Não cabe" na barra de memória | Álbum maior que a memória livre | Reduza quadros/texto |

**Onde instrumentar:** `transfer/TransferEngine.kt` — o `onProgress` já reporta
`Uploading`/`Failed`. Para ver o tráfego cru, adicione um log no
`net/UdpNetwork.kt` (envio) e em `net/PanelMessage.parse()` (recepção).

**Teste sem hardware:** `TransferEngineTest` sobe um painel simulado. Se você mudou
a máquina de transferência, esse teste pega o erro na hora.

---

## 12. Ler o `.alb` gerado

Os álbuns ficam no armazenamento interno do app:
```
/data/data/br.com.painelofertas/files/albums/<nome>.alb
```

Com o app **debug** instalado:
```bash
adb exec-out run-as br.com.painelofertas cat files/albums/Album\ 1.alb
```

Ou puxe todos:
```bash
adb exec-out run-as br.com.painelofertas tar c files/albums > albums.tar
```

O arquivo é texto **ISO-8859-1** — abra em qualquer editor. A estrutura está em
[PROTOCOLO.md §2](PROTOCOLO.md#2-camada-de-texto-formato-alb).

> 💡 Como o formato é o mesmo do app Windows, dá para **copiar um `.alb` gerado no
> PC** para a pasta do app (ou vice-versa) e comparar byte a byte. É o teste de
> compatibilidade definitivo.

---

## Precisa de algo que não está aqui?

| Assunto | Documento |
|---|---|
| Onde fica cada arquivo | [ARQUITETURA.md](ARQUITETURA.md) |
| Detalhe do protocolo | [PROTOCOLO.md](PROTOCOLO.md) |
| Validar com painel real | [HANDOFF.md](../HANDOFF.md) |
| Visão do produto | [README](../README.md) |
