# Perguntas técnicas para a LedBlock

> **Como usar:** encaminhe este documento (ou copie o texto) para o técnico
> responsável pelo firmware do painel. As perguntas estão em ordem de impacto e
> cada uma diz **o que ela destrava**. As respostas podem ser curtas.
>
> Contexto: o aplicativo Android do Painel de Ofertas já reproduz **tudo** que o
> protocolo do app Windows permite (validado byte a byte contra o código-fonte
> Delphi). Os itens abaixo são o que o app **não consegue fazer** sem informação
> de vocês — não por limitação do app, mas por falta de especificação.

---

## 1. Comandos 3, 4 e 5 do protocolo USB → destrava **conteúdo dinâmico**

No código-fonte do app Windows (`Ofertas.pas`, linhas 427-431) existem cinco
códigos de comando declarados. **Dois são usados; três nunca:**

```pascal
CMD_PADRAO              = 1;   // usado — comandos de texto
CMD_TRANSFER            = 2;   // usado — blocos de conteúdo
CMD_RESPOSTA_TEMPO_REAL = 3;   // declarado e NUNCA usado
CMD_RESPOSTA_GRAVAR     = 4;   // declarado e NUNCA usado
CMD_RESPOSTA_BUFFER     = 5;   // declarado e NUNCA usado
```

**Perguntas:**
- O firmware do painel implementa os comandos **3, 4 e 5**? O que cada um faz?
- `RESPOSTA_TEMPO_REAL` significa **escrever direto no display**, sem gravar na
  memória? Se sim, qual o **formato do pacote** (bytes) e a taxa máxima de envio?
- Existe alguma forma de o painel exibir **conteúdo que muda sozinho** — relógio,
  contador, temperatura, texto rolando?

**Por que importa:** hoje o app só grava um álbum estático que o painel rotaciona
por tempo. Com um modo de tempo real, seria possível relógio, contagem regressiva
de promoção e preço que atualiza sozinho.

---

## 2. Atualização de firmware → destrava **manutenção remota**

O app hoje **lê** a identificação do módulo Wi-Fi (`AT+GMR`, MAC, IP) e guarda para
consulta. **Não atualiza firmware**, por decisão de segurança: o painel tem dois
processadores (PIC/Microchip como ponte USB e ESP8266 para Wi-Fi), o computador
nunca fala direto com o ESP, e **corromper o PIC eliminaria o único caminho de
comunicação**, sem rota de recuperação conhecida. Uma varredura em todas as 10
versões do fonte Delphi confirmou que **não existe nenhuma rotina de gravação de
firmware** no app original.

**Perguntas:**
- Qual é o **procedimento oficial** de atualização de firmware do painel?
- O **PIC** tem bootloader por USB (ex.: Microchip HID Bootloader, mesmo
  VID `0x04D8`) ou a gravação exige **ICSP** com gravador e abrir o equipamento?
- Qual a **versão do ESP-AT** homologada por vocês? Vocês usam `AT+CIUPDATE` (OTA)?
  *(Atenção: o OTA padrão apaga o `AT+SAVETRANSLINK`, que é o que faz o painel
  encontrar o aplicativo — isso derrubaria todos os painéis do parque de uma vez.)*
- O ESP8266 tem **serial/GPIO0 acessível** na placa (para `esptool`)?

---

## 3. `RundLB.dll` → função oculta no app Windows

O app Windows tem uma funcionalidade **sem rótulo**, acionada clicando no texto de
copyright da aba "Sobre" (`Label25Click`, linha 8182). Ela lê o arquivo
`RundLB.dll` e envia **cada linha, literalmente**, ao painel via USB — com uma
máquina de estados que espera `OK` / `AT+RST` / `WIFI GOT IP`. A variável se chama
`GravaID`.

**Perguntas:**
- Para que serve? É **gravação de identidade / provisionamento de fábrica**?
- Existe um exemplo do arquivo e a documentação do que cada linha faz?
- Ela escreve em área **persistente/irreversível** do PIC?

**Por que importa:** é o único mecanismo de escrita de baixo nível que já existe no
sistema. Sem entender, não é seguro usar — nem para o app, nem para vocês.

---

## 4. Campos 2 e 3 da resposta `STATUS=`

O painel responde ao `STATUS` com:

```
STATUS=<id>,<estado>,<campo2>,<campo3>,<ini_memo>,<fim_memo>,<crc>,<intensidade>
```

O app Windows lê `id`, `estado`, `ini_memo`, `fim_memo`, `crc` e `intensidade` —
mas **nunca lê os campos 2 e 3**.

**Pergunta:** o que são? (Modelo? Versão de firmware? Temperatura? Horas ligado?)
Se algum trouxer **versão** ou **modelo**, o app passa a exibir isso sozinho, sem
precisar de cabo USB.

---

## 5. Efeitos e capacidades do display

O único comando de efeito é `ONLINE=<índice>` com três valores: `0` Padrão,
`1` Pisca/Inverte, `2` Pisca/Padrão. Confirmamos no fonte que **não há** efeito por
tela: os campos `F3..F6` do cabeçalho de quadro são flags de conteúdo (subtítulo e
centavos na Oferta; tipo de borda na Mensagem) e o byte empacotado não tem bit
livre.

**Perguntas:**
- O firmware implementa **outros efeitos** além desses três?
- Existe **rotação 90°** de verdade no controlador (para painel instalado em pé)?
  O app tem um modo retrato de *layout*, mas a rotação real depende de vocês.
- Os painéis são **monocromáticos** em todos os modelos, ou há **RGB**?
- Qual a **capacidade de memória** por modelo? (o app estima pelo `STATUS`)

---

## 6. NFC nos painéis novos

O aplicativo **já lê etiquetas NFC**: encostar o celular identifica o painel e o
registra, sem procurar na lista nem digitar IP. Isso depende só do celular — então
basta vocês gravarem a etiqueta. Formato sugerido (NDEF de texto ou URI
`ledblock://`):

```
lb=1;id=07;nome=Vitrine da Frente;ip=192.168.0.42;grupo=Acougue;modelo=LB-96;fw=1.2
```

- `lb=1` é obrigatório (faz o app ignorar crachás e cartões de outros sistemas)
- `id` = o mesmo identificador que o painel reporta no `STATUS=`
- os demais são opcionais; chaves novas são ignoradas por versões antigas do app

**Pergunta:** os modelos com NFC terão etiqueta gravável de fábrica? Faz sentido
adotarmos esse formato?

---

## Prioridade sugerida

| Ordem | Item | Destrava |
|---|---|---|
| 1º | Comandos 3, 4 e 5 (§1) | Conteúdo dinâmico — relógio, contador |
| 2º | Campos 2 e 3 do `STATUS=` (§4) | Versão/modelo sem precisar de cabo |
| 3º | Procedimento de firmware (§2) | Manutenção; hoje é presencial |
| 4º | Efeitos e capacidades (§5) | Saber o teto real do produto |
| 5º | `RundLB.dll` (§3) e NFC (§6) | Provisionamento e instalação |

Qualquer resposta, mesmo parcial, já permite avançar.
