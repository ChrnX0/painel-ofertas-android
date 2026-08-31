# Histórico de versões

Todas as mudanças relevantes do projeto. Formato baseado em
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

> ⚠️ O `versionCode` **precisa aumentar** a cada build instalável — o Android não
> substitui um APK que tenha o mesmo `versionCode`.

---

## [0.46.0] — 2026-08-31 · `versionCode 48`

### Corrigido — dois gargalos reais de desempenho
- **`SquircleShape` não implementava `equals`.** O Compose decide se um
  modificador mudou comparando parâmetros: sem `equals`, cada recomposição criava
  uma instância nova, todo `clip`/`border` se considerava alterado e **invalidava
  o desenho** — o que, para um `Outline.Generic`, significa reconstruir o `Path`
  de ~60 pontos. Espalhado por dezenas de elementos, era caro. `RoundedCornerShape`
  implementa `equals` exatamente por isso
- Formas mais usadas viraram **instâncias de topo** (`SquircleChip`, `SquircleTile`,
  `SquircleTab`, `SquircleBezel`): não alocam por recomposição
- **Prévia de LED agora é rasterizada em bitmap.** Uma placa 96×92 tem ~8.800
  células, cada acesa custando quatro círculos: até **35 mil operações por
  quadro**, repetidas sempre que algo por perto se mexia (o fôlego, o arraste, a
  aurora). Agora é desenhada **uma vez** num `ImageBitmap` via `drawWithCache` e
  depois só copiada — refaz apenas quando o tamanho ou o conteúdo muda
- **`breathingBorder` passou a usar `drawWithCache`**: o contorno é montado uma
  vez por tamanho, e só a cor muda a cada quadro
- Resultado medido no emulador: CPU em repouso de **~100% para ~60%**

### Alterado — a gaveta entrou no sistema de design
Era a única superfície que não tinha recebido nada: fundo chapado, ícones cinzas,
o logo como adesivo branco de canto duro e dois terços de vazio.
- Cada destino tem **seu tom** (Editar azul, Painéis verde-água, Agenda lilás,
  Config âmbar) e o mesmo **quadradinho de ícone tingido** dos cartões
- Item selecionado ganha o banho de cor do próprio destino e o **contorno que
  respira**; todos têm física de toque
- Cada destino ganhou uma linha de apoio ("Montar e publicar as telas")
- O logo virou **emblema**: canto superelíptico e anel no tom da marca
- O vazio virou **rodapé de estado** — quantos painéis na rede, se há cabo USB, e
  a versão. Responde "o app está vendo meu painel?" sem navegar até Painéis

### Alterado — cartões de painel recolhem
- Aberto, o cartão é uma parede: nome, sincronia, IP, memória, CRC, brilho, sensor
  e sete botões. Com dois ou três painéis, achar o certo virava rolagem
- Fechado mostra só o que responde "é este?" — bolinha de estado, nome, IP e
  brilho — e abre com um toque. **Um painel sozinho já vem aberto**
- O aviso "desatualizado" aparece mesmo fechado: é justamente o que faz abrir

### Nota sobre os ANRs no emulador
O trace apontou a causa e **não é o app**: a `RenderThread` trava em
`GrGLCompileAndAttachShader → qemu_pipe_read`. Cada shader GL novo faz uma
ida-e-volta pelo pipe do QEMU até o host, e qualquer combinação visual inédita
custa um shader. Por isso o ANR aparece na *primeira* vez que uma tela nova é
desenhada, em builds novas e antigas. Em aparelho real a compilação é de
milissegundos e fica em cache. **Ainda assim, os dois gargalos acima eram reais e
valem por si.**

## [0.45.0] — 2026-08-30 · `versionCode 47`

### Adicionado — trocar de tela arrastando
- As quatro telas viraram um **`HorizontalPager`**: arraste para o lado e a
  interface acompanha o dedo, em vez de uma animação disparar depois do toque
- **Profundidade no arraste**: a página que sai encolhe para 88% e esmaece, a que
  entra cresce até o lugar — dá camadas ao movimento em vez de dois retângulos
  trocando de posição
- **Trilho de posição** sob a barra superior. Sem a barra de abas de antes, o
  gesto seria invisível; o trilho é a pista de que há telas ao lado. O cursor lê a
  posição **contínua** do pager (`currentPage + currentPageOffsetFraction`), então
  desliza junto com o dedo em vez de pular quando a página troca
- O menu só marca a tela quando o pager **assenta** (`settledPage`): no meio do
  arraste ainda dá para voltar atrás sem que a seleção pisque

### Corrigido — conflito de gestos com a gaveta
- `ModalNavigationDrawer` captura arrasto horizontal em **toda a área de
  conteúdo**, não só na borda: com o pager, os dois gestos brigavam e a gaveta
  vencia sempre. Agora ela abre mão do gesto enquanto está fechada
  (`gesturesEnabled = drawerState.isOpen`) — abrir é pelo botão sanduíche, e
  arrastar para fechar continua funcionando
- `beyondViewportPageCount = 0`: só a tela atual fica composta. As telas são
  pesadas (o editor desenha milhares de LEDs) e manter as vizinhas vivas custaria
  caro por nada — o trabalho em andamento não se perde porque vive no ViewModel

## [0.44.0] — 2026-08-30 · `versionCode 46`

### Adicionado — o app respira
- **`BreathProvider`: um pulmão só.** Um único oscilador (0→1→0, ciclo de 4,2 s)
  compartilhado por tudo que é vivo. É o ponto da coisa: se cada elemento tivesse
  a própria animação de respiro, pulsariam fora de fase e o resultado seria
  **tremor**, não vida. Com um fôlego só, aurora, halo do painel, contorno dos
  cartões e botão Publicar incham e murcham **juntos** — o app lê como organismo
- O fôlego é lido **dentro** de lambdas de `graphicsLayer`/`drawBehind`, então só
  a fase de desenho reexecuta: nada recompõe, nada é remedido. Ler fora
  recomporia a árvore inteira 15×/s
- **Halo na placa de LED**: o painel vaza luz para fora da moldura, e o brilho
  pulsa. Reaproveita o sprite da aurora — um *blit*, não um gradiente por quadro
- **`Modifier.breathingBorder`**: cartões grandes acendem e apagam pelo
  **contorno**. Escalar o cartão rerrasterizaria o texto a cada passo e
  produziria um tremeluzir feio nas letras; o contorno deixa a geometria parada e
  move só a luz
- Aurora agora também respira: incha ~20% e clareia no mesmo compasso

### Alterado — molas bem mais expressivas
- `bouncy` de 0,55 → **0,34** de amortecimento; `springy` 0,42 → **0,24**. A 0,55
  a oscilação assentava antes de o olho registrar que houve uma
- Pressão afunda 8% (era 3,5%); pastilha de tela vai a 88%
- Pastilha selecionada: 46 → **74 dp** de largura (era 48→62)
- Botão de escolha selecionado cresce a 1,035 e o irmão encolhe a 0,94
- Entrada em cascata: escala inicial 0,94 → **0,82**, deslize 1/7 → **1/3**

### Alterado — o cartão "Painel" saiu do editor
As duas ações eram reais, mas nenhuma pertencia ali:
- **"Trazer para editar"** foi para a **página do painel na prévia deslizável** —
  o usuário está justamente olhando o conteúdo gravado quando decide mexer nele.
  O cartão à parte obrigava a lembrar do que tinha visto e procurar o botão lá
  embaixo. O botão ainda diz quantas telas virão
- **"Limpar painel"** foi para **Painéis**, junto de Ligar/Desligar/Identificar.
  Com vários painéis, "Limpar painel" solto no editor não dizia **qual** seria
  apagado; agora a confirmação nomeia o painel e lembra de salvar antes
- Removidos `PainelCard`, o diálogo de limpeza e `limpar()` do editor

## [0.43.0] — 2026-08-30 · `versionCode 45`

Um **sistema** de design orgânico, não efeitos avulsos: forma, movimento e cor
passam a ter vocabulário próprio, e o app inteiro fala essa língua.

### Adicionado — forma
- **Canto superelíptico (squircle)** em cartões, folhas, botões e pastilhas
  (`theme/Squircle.kt`). Um `RoundedCornerShape` é um arco de círculo colado numa
  reta: na emenda a curvatura salta de zero ao máximo e o olho lê "dois pedaços
  grudados". A superelipse `|x/r|ⁿ+|y/r|ⁿ=1` (n≈4,5) espalha a curvatura pelo
  canto inteiro — é a forma do One UI
- Herda de `CornerBasedShape` (não do `Shape` cru) porque é o que o Material
  exige em `MaterialTheme.shapes`: componentes **adaptam** a forma recebida — a
  folha inferior pega `shapes.large` e zera os cantos de baixo
- Cantos pequenos (8/12 dp) seguem em arco de círculo de propósito: ninguém
  enxerga a diferença, e o recorte usa o caminho rápido
- **`SoftDivider`**: traço que nasce e morre em transparente. Separa sem fatiar

### Adicionado — movimento
- **Vocabulário de molas** (`Motion.gentle/bouncy/snappy/springy`) no lugar de
  curvas de tempo. Mola tem massa e atrito: acelera, passa um pouco do ponto e
  assenta. Também é interrompível — tocar de novo no meio muda o destino a partir
  da **velocidade atual**, sem corte
- **`Modifier.pressBounce`**: o elemento encolhe sob o dedo e volta com mola. É a
  interação mais barata que existe e a que mais muda a percepção — deixa de ser
  desenho e passa a ceder ao toque
- Pastilha da tela selecionada **se alarga** (48→62 dp) em vez de só trocar de
  cor: a mudança ganha direção
- Botão **Publicar respira** enquanto está pronto (pulso de 1,7 s, some no
  instante em que o envio começa) e **transforma o conteúdo** entre Publicar →
  progresso → "No ar"
- Marca de seleção dos botões de escolha entra girando/crescendo
- Entrada em cascata (`Appear`) no editor e na gaveta: a tela se monta
- Troca de tela e de conteúdo com mola, não com `tween`

### Adicionado — fundo vivo
- **`AuroraBackground`**: três manchas pastel derivando atrás de tudo, em
  trajetórias de Lissajous com períodos **primos entre si** (29/37/43 s) — a
  combinação não se repete de forma perceptível, então não parece um GIF em laço
- Duas otimizações necessárias, ambas medidas no emulador (renderização por
  software): (1) o gradiente é rasterizado **uma vez** num bitmap de 96², e cada
  quadro faz só *blits* ampliados — a versão ingênua, com `radialGradient` por
  quadro, causava **ANR**; (2) redesenho a **4 fps** em vez de 60 — a taxa de uma
  animação deve casar com a velocidade do conteúdo, não com a do monitor: uma
  mancha que leva 29 s para dar a volta anda 1 px por quadro a 60 fps
- Custo final medido: **+16 pontos de CPU** sobre a base (97% → 113%), incluindo
  squircles e todas as animações novas

### Alterado — cor pastel nos DOIS temas
- **`AccentPalette` por tema** (`theme/Accents.kt`): o mesmo pastel não serve aos
  dois. `#8AB4F8` brilha sobre carvão e **some sobre branco** (contraste ~1,8:1).
  O tema claro usa o **mesmo matiz**, ~35% mais fundo — macio, porém legível
- `Accent.Blue` e companhia agora leem `LocalAccents`, então os **78 usos** se
  adaptam sozinhos: um nome por cor, sem variantes espalhadas pelo código
- `OnAccent` idem: quase-preto sobre acento claro (escuro), branco sobre acento
  fundo (claro) — sem isso, botão pastel no tema claro ficava ilegível
- Tema claro repintado em pastel: primário `#4A76C4` no lugar do `#1565C0`
  saturado, contêineres lavados, e **nenhuma superfície em branco 100%** — branco
  puro faz qualquer pastel ao lado parecer sujo

## [0.42.0] — 2026-08-30 · `versionCode 44`

Duas frentes: **menos telas para o mesmo trabalho** e **cor de verdade** no tema.

### Alterado — a aba "Enviar" deixou de existir
- **Publicar virou uma ação, não um destino.** O caminho antigo era "salvar álbum →
  trocar de aba → reencontrar o álbum → escolher painel → enviar". Agora é **um
  toque** na barra Publicar, que já salva o álbum antes de mandar
- **Destino escolhido na própria barra**: toque em "PARA" e abre a folha *Publicar
  em* — um painel, vários, um grupo inteiro ou o USB, com a senha de transmissão
  ali mesmo, no momento em que ela importa
- **Envio para vários painéis com barra única** que nunca volta para trás, dizendo
  qual painel está indo (`Painel 2 de 3 (Açougue) · 47%`) e um resumo por painel
  ao final quando algum falha
- **"Digitar o IP na mão"** dentro da folha — painel em outra sub-rede ou que ainda
  não respondeu à varredura continua alcançável. Era a única função que a aba
  Enviar tinha e não existia em outro lugar
- Removidos `EnviarScreen.kt` e `SendViewModel.kt` (o estado de destino existia em
  duplicata) e o botão "Salvar e enviar", que virou redundante
- Menu lateral agora tem **4 itens** (Editar · Painéis · Agenda · Config)

### Alterado — o bloco de ajustes da tela
- Eram **quatro controles em três idiomas** (dois segmentados no topo, dois
  interruptores num cartão lá embaixo), misturando o que vale para o painel inteiro
  com o que vale só para a tela atual. Agora é **um cartão só**, na ordem em que se
  pensa: **o painel** (deitado/em pé) → **esta tela ocupa** (inteiro/metade) →
  **esta tela mostra** (oferta/texto livre) → **mostrar no painel** (o liga/desliga
  de verdade, o único que continua sendo um interruptor)
- As escolhas viraram **botões com o desenho do resultado** — o lojista vê a forma
  da tela em vez de ler "meia tela" e ter que imaginar
- Vocabulário unificado: acabou o "quadro" convivendo com "tela"
- `setHalf` / `setEnabled` no ViewModel: os dois formulários repetiam o mesmo par
  de interruptores porque os campos moram em profundidades diferentes nos tipos

### Alterado — mais cor, mesmo tom pastel
- Banho de cor dos cartões de `0.16` para **`0.22`** (no escuro o pastel diluía em
  cinza; nesse ponto a cor se lê e o texto ainda fica em ~7:1 de contraste)
- **Contorno no tom do cartão**: área pequena aceita cor bem mais saturada sem
  atrapalhar a leitura — é o que faz a cor parecer intencional
- Ícone do cabeçalho com anel da própria cor; sobrancelhas de seção podem assumir
  o tom da seção
- Superfícies do tema escuro com um leve azul-arroxeado no lugar do cinza puro,
  e o claro com fundo levemente azulado para o pastel dos cartões aparecer
- Três acentos novos: `Peach`, `Mint`, `Sky`

## [0.41.0] — 2026-08-30 · `versionCode 43`

### Adicionado
- **Leitura de NFC — o app já está pronto para painéis com a tecnologia**: encostar
  o celular numa etiqueta identifica o painel (id, nome, IP, grupo), registra na
  lista e abre a aba Painéis. Não depende do protocolo do painel, só do NFC do
  celular — funciona hoje com etiqueta colada e funcionará direto quando vier de
  fábrica
- Formato documentado e **tolerante** (`nfc/PanelTag.kt`): registro NDEF de texto
  ou URI `ledblock://`, com `lb=1` obrigatório para **ignorar crachás e cartões**
  de outros sistemas. Chaves desconhecidas são ignoradas, então etiquetas gravadas
  por versões futuras continuam legíveis
- +7 testes (`PanelTagTest`), incluindo ida-e-volta gravar/ler

### Investigado
- **Comandos ociosos do firmware**: o fonte original declara `CMD_RESPOSTA_TEMPO_REAL=3`,
  `CMD_RESPOSTA_GRAVAR=4` e `CMD_RESPOSTA_BUFFER=5` e **nunca os usa**. O nome do
  primeiro sugere um **modo de tempo real** que destravaria conteúdo dinâmico
  (relógio, contador). Documentado no `HANDOFF.md` como pergunta à LedBlock — nada
  foi enviado ao painel, por segurança

---

## [0.40.0] — 2026-08-30 · `versionCode 42`

### Adicionado
- **Efeitos com prévia ao vivo**: cada opção (Padrão · Pisca/Inverte · Pisca/Padrão)
  vira uma **mini-placa de LED animada** mostrando o efeito de verdade, com uma
  frase explicando o que faz. O lojista vê antes de mandar
- **Efeito aplicado na hora**: ao escolher, o app envia `ONLINE=<índice>` a todos os
  painéis conhecidos e confirma. Antes só valia no próximo batimento de `STATUS`
- **Identificação do módulo guardada**: a leitura de firmware/MAC/IP fica salva
  (com "lido há…") para consulta e suporte, mesmo com o USB desconectado

### Verificado no código-fonte original
- Os efeitos do painel são **exatamente três** (`ONLINE=0|1|2`) e são **globais**.
  **Não existe** efeito por tela: os campos `F3..F6` do cabeçalho de quadro são
  flags de conteúdo (subtítulo/centavos na Oferta; tipo de borda na Mensagem) e o
  byte empacotado não tem bit livre. Também **não existe** scroll, transição ou
  animação no protocolo
- **Atualização de firmware não existe** em nenhuma das 10 versões do fonte Delphi.
  Ver `HANDOFF.md` → "Atualização de firmware": o painel tem dois processadores
  (PIC gateway + ESP8266) e atualizar às cegas arrisca **brick sem recuperação**.
  Documentadas as 6 informações que a LedBlock precisa fornecer para destravar

---

## [0.39.0] — 2026-08-29 · `versionCode 41`

### Adicionado
- **Grupos de painéis**: cada painel ganha um **grupo/setor** (ex.: "Açougue"). Na
  aba Enviar aparecem os grupos — **um toque marca todos os painéis do setor** e o
  envio vai para o grupo inteiro
- **Importar planilha de preços** (CSV/TXT do Excel, Sheets ou PDV) direto no
  diálogo de criação em lote: `PICANHA;9,90;O KILO` vira a oferta pronta.
  Reconhece `;`, tabulação e vírgula, **sem confundir a vírgula decimal de 9,90**;
  normaliza `12.50` → `12,50`, ignora cabeçalho e aspas, aceita CRLF do Windows
- +10 testes (`PriceListParserTest`) com os formatos reais de planilha

---

## [0.38.0] — 2026-08-29 · `versionCode 40`

### Adicionado
- **Criação em lote — o álbum inteiro de uma vez**: cole a lista de ofertas (uma por
  linha) e cada linha vira uma tela pronta, diagramada com o preço em destaque e
  **nomeada pelo produto**. Quem troca 20 preços por semana passa a fazer isso numa
  colada. Opção de somar às telas atuais ou substituir
- **Prévia animada**: botão **Ver rodando** exibe a sequência girando com o **tempo
  de cada tela**, como o painel vai mostrar — dá para conferir o álbum de relance
- +5 testes (`BatchTest`) da lógica de lote: nome vindo do produto, limite de duas
  palavras, linhas sem preço, linhas em branco e a separação de cada oferta

---

## [0.37.0] — 2026-08-29 · `versionCode 39`

### Adicionado
- **Composição inteligente — digitar vira desenhar**: escreva `PICANHA 9,90 O KILO`
  numa linha só e o app **reconhece o preço**, separa em três linhas e monta o
  cartaz com **hierarquia**: o preço em letra grande, produto e medida menores.
  É o que faz uma oferta parecer profissional
- Reconhece `9,90`, `12.50`, `1.234,56`; **não** confunde com `100 G` ou `CX 12`
  (exige separador decimal). Respeita as quebras que o usuário digitar
- A UI confirma o que entendeu: *"Reconheci 9,90 como preço — vai em letra grande"*
- Interruptor **Destacar o preço** para desligar e voltar ao tamanho uniforme
- +6 testes (15 no total em `AutoLayoutTest`), cobrindo separação, formatos de
  preço, falsos positivos e a garantia de hierarquia

---

## [0.36.0] — 2026-08-29 · `versionCode 38`

### Adicionado
- **Motor de diagramação (o texto se ajusta à tela de verdade)**: no modo Livre,
  escreva o texto corrido — o app **quebra em linhas por palavra**, testa da maior
  fonte para a menor e usa **a maior em que tudo cabe (largura E altura juntas)**,
  centralizando o bloco. Palavra maior que o painel é quebrada dentro dela; quebras
  digitadas pelo usuário são respeitadas
- **Alinhamento** (esquerda / centro / direita) e **tamanho máximo** — o app usa
  esse teto ou menos, o que couber
- **Retorno do que foi decidido**: "3 linha(s) · fonte Terceira", ou o aviso de que
  não cabe nem no menor tamanho
- **9 testes de unidade** do motor (`AutoLayoutTest`), com fontes sintéticas — cobrem
  quebra por palavra, quebra dentro da palavra, escolha de fonte, centralização
  vertical e o caso que não cabe

### Corrigido
- O auto-ajuste anterior media só a **largura de cada linha isolada** — podia
  estourar a **altura** do painel. Agora largura e altura são avaliadas juntas

---

## [0.35.0] — 2026-08-29 · `versionCode 37`

### Adicionado
- **Auto-justificar (modo Livre)**: o app centraliza o texto na largura, distribui
  as linhas na altura e escolhe **a maior fonte que couber** — o lojista só escreve.
  Novo `render/AutoLayout.kt`; com o ajuste desligado, os campos manuais voltam
- **Aviso de texto cortado**: se o conteúdo estoura o painel, um aviso aparece na
  prévia dizendo **quanto** passou (px em largura/altura) e o que fazer. Evita
  mandar para o display algo que apareceria pela metade
- **Agendamento em segundo plano de verdade**: `AlarmManager` + serviço em primeiro
  plano (`schedule/Scheduler.kt`, `ScheduleService.kt`) — o envio programado
  acontece **com o app fechado**, e os alarmes são recriados após reiniciar o
  celular. Antes só funcionava com o app aberto
- **Histórico de publicações**: as últimas 12 telas publicadas ficam guardadas;
  um toque recupera qualquer uma (a oferta de terça que volta na quinta)

---

## [0.34.0] — 2026-08-29 · `versionCode 36`

Passe de usabilidade: menos cerimônia, mais retorno, nada de trabalho perdido.

### Adicionado
- **Barra "Publicar" fixa no editor** — a ação principal do app numa só: salva o
  álbum **e** envia ao painel, sem sair da tela. Mostra **para onde vai**, avisa se
  **não cabe** na memória, e dá o retorno ali (enviando % · ✓ No painel agora ·
  falhou). Antes eram 7 passos por troca de preço; agora é **um toque**
- **Guia de primeira execução** ("Como funciona", 3 passos) — some ao dispensar
- **Rascunho automático**: o trabalho em andamento é salvo continuamente e
  restaurado ao abrir; o app pode ser encerrado sem perder o que foi digitado
- **Envio para vários painéis** (aba Enviar): marque quantos quiser e envie o mesmo
  álbum a todos, com progresso e resumo do que falhou

### Alterado
- A barra Publicar **sobe com o teclado** (`imePadding`) — nunca fica escondida
- Alvos de toque das telas numeradas em 48dp e descrições de acessibilidade
  ("Tela 2: Promoção, selecionada") em vez de só o número

---

## [0.33.0] — 2026-08-29 · `versionCode 35`

### Alterado
- **Prévia "No painel" automática**: o app agora baixa **sozinho** o que está no
  painel e mostra na prévia deslizável — sem precisar tocar em "Sincronizar". Ele
  re-sincroniza sozinho só quando o conteúdo do painel muda (compara o CRC do
  `STATUS`), então não fica baixando à toa. O botão Sincronizar continua como
  atualização manual

---

## [0.32.0] — 2026-08-29 · `versionCode 34`

### Adicionado
- **Modo Padrão / Livre** no editor: um seletor no topo do conteúdo troca a tela
  entre **Padrão** (oferta estruturada) e **Livre** (texto solto com posição/fonte
  por linha) — reaproveitando os tipos Oferta/Mensagem num só lugar

### Alterado
- **"Complementos" fundido**: a **Medida** foi para dentro do cartão **Preço** (é a
  unidade do preço), e **Auxiliar + Rodapé** viraram um cartão **Rodapé** sempre
  visível. Some o cartão colapsável confuso

---

## [0.31.0] — 2026-08-29 · `versionCode 33`

### Corrigido
- **Bug crítico de descoberta**: o app só achava o painel se o campo "IP deste
  celular" fosse apagado. Causa: o IP **salvo** tinha prioridade sobre o **detectado**;
  um valor velho (DHCP mudou / outra Wi-Fi) fazia a varredura procurar na sub-rede
  errada e mandar o painel responder pro IP errado. Agora a **detecção sempre vence**
  (o salvo é só último recurso), o detector **prefere a interface Wi-Fi (wlan)**, e o
  campo virou **somente leitura** (informativo) — sem como digitar um IP errado

---

## [0.30.0] — 2026-08-29 · `versionCode 32`

Reforma grande a partir do retorno do cliente: navegação por menu, editor com faixa
fixa, e diagnóstico de rede.

### Adicionado
- **Menu lateral (gaveta ☰)** no lugar da barra de abas inferior. Barra superior:
  ☰ à esquerda, **logo maior centralizado**, indicadores à direita
- **Indicadores por ícone**: o texto "Wi-Fi"/"USB" virou **ícone** (📶 / USB),
  mantendo a bolinha colorida de status
- **Editor com faixa fixa**: fileira de **telas numeradas** (One UI) + **"＋"** que
  abre um diálogo para **nomear** a nova tela e escolher o tipo; **prévia deslizável**
  (arraste entre *Editando* ↔ *No painel*); a info do painel fica fixa — só o
  formulário rola
- **Nomes por tela**: cada tela do álbum tem nome editável (nos quadradinhos e na
  Sequência)
- **Novo álbum** no cartão Álbum
- **Console de diagnóstico TX/RX** (aba Painéis) — o "Memo2" do app Windows que faltava:
  registra o que o app envia e o que o painel responde, com hora
- **Feedback ao enviar** comandos ao painel (aviso na tela)

### Corrigido
- **Sincronizar/Limpar travados**: o botão exigia um painel *ONLINE* naquele instante
  (o status esfria pra "instável" em ~15 s); agora vale **qualquer painel conhecido**
- Botão "Sincronizar" com a palavra quebrada — botões do cartão Painel empilhados

---

## [0.29.0] — 2026-08-29 · `versionCode 31`

Reorganização de abas + passe de cor pastel (retorno do cliente).

### Alterado
- **Organização das abas** (mais lógica): **Segurança de transmissão** foi de Config
  para **Enviar** (é sobre o envio); **Diagnóstico do dispositivo** foi de Config para
  **Painéis** (é sobre o aparelho conectado). Config fica só com Tema, Cor do LED,
  Efeito, Sobre e o autoteste do protocolo
- **Cor pastel espalhada**: azul primário do tema escuro suavizado; **banho leve de
  cor** nos cartões (`accentCardColors`) e **botões coloridos por função**
  (Sincronizar/Ligar/Receber verde, Limpar/Desligar rosa, Identificar âmbar) — via
  `AccentButton`/`AccentOutlinedButton`. Colorido, porém suave

---

## [0.28.0] — 2026-08-29 · `versionCode 30`

Continuação do retorno do cliente: config por painel + paleta pastel.

### Adicionado
- **Configuração por painel** (seção expansível no card): **apelido/observação** e a
  **rede própria** do painel — Wi-Fi (SSID/senha), **DHCP** ou **IP fixo/gateway/
  máscara** — tudo persistido no histórico, com **Salvar** e **Aplicar via USB**
- Novos campos no `Panel` + `PanelRepository.setConfig` + persistência no
  `PairedPanelsStore`
- **Excluir álbuns**: cada álbum salvo (no card Álbum do editor) agora tem um **✕**
  com confirmação — expõe o `AlbumStore.delete` que existia mas não tinha UI

### Alterado
- **Paleta pastel** (`Accent`): cores suaves nos ícones dos cartões e nos chips de
  status — colorido, mas sem berrar. Aplicada em Painéis, Editar e Config
- Cartão global de Wi-Fi via USB renomeado para **"Conectar painel novo (via USB)"**
  (a rede dos painéis já conhecidos agora se edita no próprio card)
- Cabeçalhos coloridos (`CardHeader`) nas telas de **Config** (Tema, Cor do LED,
  Efeito, Segurança)

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
