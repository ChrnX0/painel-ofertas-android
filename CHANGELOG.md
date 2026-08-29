# Histórico de versões

Todas as mudanças relevantes do projeto. Formato baseado em
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

> ⚠️ O `versionCode` **precisa aumentar** a cada build instalável — o Android não
> substitui um APK que tenha o mesmo `versionCode`.

---

## [0.27.0] — 2026-08-29 · `versionCode 29`

Reorganização de usabilidade a partir do retorno do cliente + passe de cor e movimento.

### Adicionado
- **Histórico de painéis pareados**: os painéis agora são **persistidos**
  (`PairedPanelsStore` em SharedPreferences/JSON) — reaparecem ao abrir o app mesmo
  **offline**, com **chip de status colorido** e marca de **"visto há …"**
- **Cor nos cartões**: cabeçalhos com **ícone colorido** (`CardHeader`) — rede em azul,
  senha em âmbar, painel em verde
- **Mais animação**: `animateContentSize` nos cartões que expandem (Textos,
  Complementos, Wi-Fi, Senha)

### Alterado
- **Rede do aparelho** (IP do celular + DHCP) **movida de Config para a aba Painéis**,
  onde os painéis são gerenciados
- **Enviar**: passo 1 renomeado para **"Qual álbum enviar"** com a legenda "Os álbuns
  que você salvou na aba Editar" — deixa claro de onde vêm os álbuns
- Painéis lembrados começam "bem offline" para o liveness não os marcar como
  "instável" antes de um `STATUS` real

---

## [0.25.0 – 0.26.0] — 2026-08-29 · `versionCode 27–28`

Rodada de refino de UX das 5 telas (padrão One UI, com movimento) + fechamento de
paridade com o app Windows. Testado em emulador Android 15 (claro e escuro).

### Adicionado
- **Status de conexão na barra**: pílulas **Wi-Fi** e **USB** com bolinha que traduz
  a fase — 🟢 online · 🟡 procurando (pulsa) · 🔵 transferindo (pisca) · 🔴 erro ·
  ⚪ desligado (`ConnectionCenter` + `StatusPill`)
- **Sincronizar com o painel** no editor: lê o que está gravado (Receber) e traz as
  telas para a sequência, para escolher onde inserir a nova e **excluir as que quiser**
- **Limpar painel** (com confirmação): apaga toda a memória (`APAGAR`→`INICIAR`)
- **Senha do painel via USB** (`SENHA=`, porte de `BitBtn10Click`): definir/remover a
  senha de transmissão gravada no painel — *fechava um gap de paridade*
- **Exportar/Importar `.alb`** para arquivo externo (SAF) — interop com o app Windows
  e backup — *fechava um gap de paridade*
- **Excluir painel** da lista (aba Painéis)
- Movimento estilo One UI: entradas escalonadas dos cartões e transição direcional
  entre abas

### Alterado
- **Descoberta muito mais rápida**: a varredura ia em 5 lotes com 10 s de intervalo
  (até 40 s); agora manda a /24 em rajadas curtas (~200 ms) e **reprocura sozinha a
  cada 8 s** até achar. Fim do "achei que travou"
- **Enviar** reescrita em **passos numerados** (1 O que enviar · 2 Para onde) e o botão
  virou **"Receber (ler o que está no painel)"**
- **Painéis**: o IP que confundia agora aparece como **"meu aparelho: …"** (é o celular,
  não o painel), com botão Procurar animado
- **Config**: "Tema" → **"Tema do aplicativo"**, com aviso de que não altera o painel
- Editor agrupado em cartões (Interruptores · Textos · Preço · **Complementos**
  colapsável)

### Removido
- Linha de **Modelos** (Açougue/Hortifruti/…) do editor

### Corrigido
- Sobreposição de textos no cartão "Wi-Fi do painel (via USB)" — o componente assumia
  estar dentro de uma `Column`, mas a animação de entrada empilha filhos como `Box`

---

## [0.24.0] — 2026-08-25 · `versionCode 26`

### Corrigido
- **Logotipo**: passa a usar a **imagem oficial** da LedBlock (com a margem branca
  recortada) em vez de uma recriação vetorial que estava com as cores erradas

---

## [0.23.0] — 2026-08-25 · `versionCode 25`

### Alterado
- Revertido o cabeçalho grande estilo One UI: ele fundia o logotipo com o nome da
  tela. Volta a barra simples com o logotipo, e o título dentro de cada tela
- Interruptores de **DHCP** e **senha** alinhados à direita, com respiro do rótulo

---

## [0.21.0 – 0.22.0] — 2026-08-25 · `versionCode 23–24`

### Adicionado
- Passe visual inspirado no One UI: cantos mais arredondados em cards e botões

---

## [0.20.0] — 2026-08-25 · `versionCode 22`

### Adicionado
- **Desfazer** ao excluir um quadro (`SnackbarHost` global + `undoDelete()`)

---

## [0.19.0] — 2026-08-24 · `versionCode 21`

### Adicionado
- **Modelos de oferta**: Açougue · Hortifruti · Bebidas · Padaria · Frios · Limpeza —
  preenchem cabeçalho, medida e rodapé num toque
- **Barra de memória** no envio: avisa se o álbum cabe no painel *antes* de enviar

---

## [0.17.0 – 0.18.0] — 2026-08-24 · `versionCode 19–20`

Recursos do hardware que existiam no firmware mas **não eram expostos** pelo app
Windows — descobertos lendo o código-fonte original.

### Adicionado
- **Sensor de luz / auto-brilho** — flag no bit 128 do byte de brilho; mostra a
  leitura do sensor ao vivo
- **Selo de sincronismo por CRC** — compara o CRC gravado no painel com o enviado
  (✓ Sincronizado / ⚠ Desatualizado)
- **Auto-conexão** — varre a sub-rede ao abrir o app e ao entrar na aba Painéis
- **Diagnóstico do dispositivo** — chip/fabricante/série via descritores USB e
  firmware/MAC via `AT+GMR` e `AT+CIFSR` (consultas *read-only*)

### Verificado
- Os efeitos globais **já chegavam** ao painel via `ONLINE=<índice>` na resposta ao
  `STATUS=` — não havia bug

---

## [0.14.0 – 0.16.0] — 2026-08-24 · `versionCode 16–18`

### Alterado
- Editor reorganizado em blocos: Prévia → Conteúdo → Sequência → Álbum
- Formulário da Oferta com o **preço como destaque** (leitura `= R$ 9,90` ao vivo) e
  interruptores agrupados em card

### Corrigido
- **Subtítulo × cabeçalho**: agora o campo some da tela com explicação, deixando
  claro que são excludentes (comportamento do sistema original, não bug)

---

## [0.11.0 – 0.13.0] — 2026-08-21 · `versionCode 13–15`

### Adicionado
- **Identidade visual** própria: paleta LedBlock (o azul da marca como acento),
  tipografia **Archivo** + **IBM Plex Mono** para dados técnicos
- Prévia do LED em **moldura de equipamento**, com brilho, halo e reflexo
- **Cor do LED** configurável na prévia (âmbar/vermelho/verde/azul/branco)
- Layout **responsivo** — coluna com largura de leitura em tablets
- Estados vazios ilustrados, transições entre abas, vibração ao salvar/enviar

### Corrigido
- Chips e abas selecionadas deixam de usar o lilás padrão do Material

---

## [0.10.0] — 2026-08-21 · `versionCode 12`

### Adicionado
- **Tema** Sistema / Claro / Escuro
- **Modo retrato** (vertical) no editor
- Logotipo na barra superior

---

## [0.8.0 – 0.9.0] — 2026-08-21 · `versionCode 9–11`

### Adicionado
- Lista de álbuns observável (`StateFlow`) — telas atualizam sozinhas
- Fluxo **"Salvar e enviar →"**
- Gerenciador de painéis com brilho ao vivo e ligar/desligar
- Botão voltar navega entre abas em vez de fechar o app

---

## [0.7.0] — 2026-08-21 · `versionCode 8`

Passe de robustez após duas auditorias de código.

### Adicionado
- Montador de sequência: múltiplos quadros, reordenar, duplicar, tempo, borda
- **ViewModels** de escopo de Activity — estado sobrevive a rotação e troca de aba;
  transferências não são canceladas ao navegar
- Barra de progresso e trava anti-duplo-toque
- Confirmação ao sobrescrever álbum

### Corrigido
- Erros de rede não derrubam mais o app
- **Sanitização de texto** aplicada na compilação (evita injetar `0xFF`/`CR`)
- Vazamento de conexão USB ao desconectar; laço de leitura sem *busy-wait*
- Teto de sanidade no tamanho do download

---

## [0.3.0] — 2026-08-20 · `versionCode 3`

### Adicionado
- Editor de **Oferta** completo (`OfertaLayout`) e de **Mensagem**
- Prévia no tamanho real do painel (meia tela / tela cheia)
- Agenda com recorrência diária e brilho por tarefa
- Abas Painéis, Wi-Fi e Config

---

## [0.1.0] — 2026-08-19 · `versionCode 1`

### Adicionado
- Núcleo de protocolo portado do Delphi e **validado byte-a-byte**:
  codec binário, CRC-16/XMODEM, formato `.alb`, mapa de acentos
- Parser das fontes bitmap `.flb` e renderização da prévia
- Máquina de transferência (enviar/receber em blocos) com painel simulado
- Transporte UDP e USB HID
- Estrutura do app em Compose com as 5 abas
