# 🧭 Decisões de projeto

> Por que o projeto é assim e não de outro jeito. Se você pensou *"isso está
> errado, vou consertar"* — leia aqui antes. Boa parte das esquisitices é
> **proposital**, e desfazê-las quebra o painel na loja.

| # | Decisão | Veredito |
|---|---|---|
| [1](#1-kotlin-nativo-em-vez-de-flutter) | Kotlin nativo, não Flutter | 🟢 Firme |
| [2](#2-fidelidade-ao-original-acima-de-boas-práticas) | Fidelidade > "código bonito" | 🟢 Firme |
| [3](#3-protocol-sem-nenhuma-dependência-de-android) | `protocol/` sem Android | 🟢 Firme |
| [4](#4-injeção-de-dependência-manual-appcontainer) | DI manual, sem Hilt/Koin | 🟡 Revisável |
| [5](#5-o-mesmo-panelframe-alimenta-a-prévia-e-o-fio) | Prévia e fio compartilham o `PanelFrame` | 🟢 Firme |
| [6](#6-panellink-abstrai-udp-e-usb) | `PanelLink` abstrai os transportes | 🟢 Firme |
| [7](#7-arquivos-alb-em-vez-de-banco-de-dados) | Arquivos `.alb`, sem banco | 🟢 Firme |
| [8](#8-viewmodels-de-escopo-de-activity) | ViewModels de escopo de Activity | 🟢 Firme |
| [9](#9-sem-dynamic-color-material-you) | Sem dynamic color | 🟡 Revisável |
| [10](#10-dois-bugs-do-original-corrigidos-de-propósito) | Dois bugs do original **corrigidos** | 🟢 Firme |
| [11](#11-agendamento-em-segundo-plano-adiado) | Agendamento em 2º plano adiado | 🔴 Pendente |
| [12](#12-não-explorar-o-firmware-por-tentativa-e-erro) | Não sondar o firmware às cegas | 🟢 Firme |
| [13](#13-apk-de-debug-sem-chave-de-release) | APK de debug, sem keystore no repo | 🟡 Revisável |

---

## 1. Kotlin nativo em vez de Flutter

**Contexto.** A escolha inicial era entre Flutter (multiplataforma) e Kotlin nativo.

**Decisão.** Kotlin + Jetpack Compose.

**Por quê.** O app precisa de **USB HID**, que é API nativa do Android
(`UsbManager`, endpoints de interrupção). Em Flutter isso viraria um plugin
nativo — ou seja, o mesmo código Kotlin **mais** uma camada de canal por cima. E o
iOS sequer permitiria esse acesso USB, então a promessa multiplataforma não se
realizaria justamente na parte mais crítica.

**Consequência.** Sem versão iOS. Aceitável: o público-alvo é o balcão da loja, e
o caminho USB é essencial para configurar a rede do painel.

---

## 2. Fidelidade ao original acima de "boas práticas"

**Contexto.** O protocolo tem esquisitices reais: CRC que ignora os 2 últimos
bytes, `N_DATA = 30` que não é o tamanho do bloco (60), espaço extra quando o
comando tem exatamente 76 caracteres, endianness assimétrico entre envio e
recepção.

**Decisão.** Reproduzir **tudo** exatamente como está, com comentário explicando.

**Por quê.** O painel na parede da loja **não vai ser atualizado**. Ele espera esses
bytes. "Melhorar" o protocolo significa simplesmente parar de funcionar.

**Consequência.** Código com comentários do tipo *"parece bug, mas é o protocolo"*.
Cada trecho portado cita a rotina de origem no `Ofertas.pas`. Ver
[armadilhas](COMECE-AQUI.md#6-armadilhas--leia-antes-de-mexer).

---

## 3. `protocol/` sem nenhuma dependência de Android

**Decisão.** O pacote `protocol/` (e `render/`) importa apenas Kotlin/stdlib.

**Por quê.**
- Testes rodam na **JVM**, em segundos, sem emulador — e são eles que garantem a
  compatibilidade byte-a-byte;
- deixa explícito que serialização não é assunto de UI;
- se um dia for preciso um utilitário desktop ou um servidor, o núcleo se move sem alteração.

**Consequência.** Nada de `Context` ali dentro. Quem precisa ler arquivo passa o
conteúdo já lido (é o que `FontRepository` faz com os `.flb`).

---

## 4. Injeção de dependência manual (`AppContainer`)

**Decisão.** Um `AppContainer` criado na `Application`, em vez de Hilt/Koin/Dagger.

**Por quê.** O grafo é pequeno e estável (7 objetos de vida longa). Hilt traria
processamento de anotações, tempo de build maior e uma camada a mais para quem
chega no projeto. `AppContainer.kt` é legível de cima a baixo — funciona como
documentação de quem depende de quem.

**Quando revisar.** Se o app crescer para dezenas de dependências ou passar a ter
vários escopos, migrar para Hilt passa a compensar.

---

## 5. O mesmo `PanelFrame` alimenta a prévia e o fio

**Decisão.** `OfertaLayout.build()` produz um `PanelFrame`, e esse **mesmo objeto**
vai tanto para o `PanelRenderer` (prévia na tela) quanto para o `BinaryCodec`
(bytes que vão para o painel).

**Por quê.** É o que garante que **o que o lojista vê é o que o painel mostra**. Se
houvesse dois caminhos de layout, eles divergiriam na primeira mudança.

**Consequência.** Mexer no layout muda os dois automaticamente — e a prévia vira o
melhor teste de posicionamento que existe.

---

## 6. `PanelLink` abstrai UDP e USB

**Decisão.** Uma interface com `sendText` / `sendErase` / `sendDataBlock` /
`incoming`, implementada por `UdpLink` e `UsbLink`.

**Por quê.** O `TransferEngine` — a parte mais delicada do projeto (stop-and-wait,
retransmissão, timeouts) — existe **uma vez só** e funciona nos dois transportes.
Também torna possível testar a máquina inteira com um `PanelLink` falso, sem hardware.

**Consequência.** As diferenças de enquadramento ficam **dentro** de cada
implementação (por exemplo: no USB o apagar não leva código de senha, e o offset
recebido é big-endian).

---

## 7. Arquivos `.alb` em vez de banco de dados

**Decisão.** Álbuns gravados como arquivos texto no armazenamento interno, em
ISO-8859-1 — exatamente o formato do app Windows.

**Por quê.** **Interoperabilidade.** Um álbum criado no Android abre no PC e
vice-versa. Um banco (Room) daria consultas que ninguém precisa e criaria um
formato paralelo que teria de ser convertido na hora de trocar arquivos.

**Consequência.** Sem busca/índice — irrelevante para dezenas de álbuns. A listagem
é um `StateFlow` que relê o diretório quando muda.

---

## 8. ViewModels de escopo de Activity

**Decisão.** `EditorViewModel`, `SendViewModel` e `AppViewModel` com escopo de
Activity (não por tela).

**Por quê.** Dois problemas reais:
1. O lojista monta uma oferta, gira o celular e **perde tudo** — inaceitável;
2. Uma transferência iniciada na aba Enviar era **cancelada ao trocar de aba**,
   deixando o painel com conteúdo pela metade.

Com escopo de Activity, o estado sobrevive à recomposição e o `viewModelScope`
continua vivo durante a navegação.

**Consequência.** O estado do editor persiste enquanto o app está aberto — que é
justamente o comportamento esperado.

---

## 9. Sem dynamic color (Material You)

**Decisão.** Paleta fixa da marca; o dynamic color do Android 12+ está desligado.

**Por quê.** O app carrega a identidade da **LedBlock**. Com dynamic color, ele
assumiria a cor do papel de parede de cada usuário — a marca sumiria e o app teria
aparência diferente em cada aparelho.

**Detalhe importante:** o `secondaryContainer` é definido **explicitamente**. Sem
isso, o Material aplica um lilás padrão em chips e na aba selecionada — foi
exatamente o que dava "cara de app genérico".

**Quando revisar.** Se a LedBlock preferir que o app siga o tema do sistema.

---

## 10. Dois bugs do original **corrigidos** de propósito

Contradiz a [decisão 2](#2-fidelidade-ao-original-acima-de-boas-práticas)? Não —
estes não afetam os bytes no fio, só a leitura de volta.

| Bug do original | O que fazia | Correção |
|---|---|---|
| **"bug do `z`"** | Ao reler um registro, o texto era truncado se contivesse um `z` minúsculo | `BinaryCodec.campoTexto()` divide corretamente no 6º `;` |
| **Acentos implícitos** | O mapeamento acento → placeholder acontecia espalhado, com risco de escapar caractere inválido | Centralizado em `AccentMap.normalize()`, aplicado na compilação — também impede injetar `0xFF`/`CR` |

**Por quê.** O primeiro corrompia dados do usuário; o segundo era risco de corromper
o **álbum inteiro** no painel. Nos dois casos o resultado no fio continua idêntico
para entradas válidas.

---

## 11. Agendamento em segundo plano adiado

**Situação.** O agendador dispara apenas **com o app aberto**.

**Por que não foi feito.** Fazer agendamento confiável com o app fechado exige
`WorkManager`/`AlarmManager` **e** lidar com *Doze mode* e com as otimizações
agressivas de bateria de alguns fabricantes. Isso só se valida deixando rodar
**horas ou dias em aparelho real**.

**Por que isso importa.** Um agendamento que falha em silêncio é **pior que não
ter**: o lojista confia que a oferta trocou às 8h e ela não trocou. Entregar sem
validar seria irresponsável.

**Próximo passo.** Implementar junto com a validação em campo. Ver [Roadmap](../README.md#️-roadmap).

---

## 12. Não explorar o firmware por tentativa e erro

**Decisão.** O app só envia comandos **documentados no código-fonte original** ou
comandos padrão do ESP-AT. Nada de sondar comandos desconhecidos para "descobrir
recursos escondidos".

**Por quê.** Do outro lado há um equipamento em produção na parede de uma loja. Um
comando inesperado pode travar o controlador ou corromper a memória. Consulta de
identidade (`AT+GMR`, `AT+CIFSR`, descritores USB) é segura e documentada —
exploração de firmware se faz **em bancada, com o fabricante**.

**Consequência.** O teto de funcionalidades é o que o firmware já oferece. Para ir
além, é preciso a especificação do controlador — ver as perguntas em
[HANDOFF.md](../HANDOFF.md#perguntas-para-a-ledblock-destravariam-novas-funções).

---

## 13. APK de debug, sem chave de release

**Decisão.** O repositório e o CI produzem **APK de debug**. Nenhum keystore é
versionado (`.gitignore` bloqueia `*.jks`, `*.keystore`, `keystore.properties`).

**Por quê.** A chave de assinatura é a **identidade do app** — quem a tem publica
atualizações em nome dele. Ela pertence a quem for distribuir (a LedBlock), não ao
repositório.

**Consequência.** Para instalar, é preciso permitir "fontes desconhecidas". Para
distribuir de verdade, gerar keystore próprio — instruções no
[README](../README.md#-começando).

---

## Como adicionar uma decisão aqui

Registre quando a escolha **não for óbvia** para quem chega, especialmente se
alguém pode querer "consertar" depois. Formato: contexto → decisão → por quê →
consequência → quando revisar.
