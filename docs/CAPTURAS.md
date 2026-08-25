# 📱 Capturas de tela

Telas reais do aplicativo, capturadas em um emulador **Pixel 6 · Android 15 (API 35)**.

> As imagens desta página são capturas do app em execução. Para ver **o que o painel
> de LED exibe** em cada configuração, veja [EXEMPLOS.md](EXEMPLOS.md).

---

## Editar — o editor de ofertas

<img src="capturas/01-editar.png" alt="Aba Editar no tema escuro" width="320">

A tela principal. De cima para baixo:

- **Prévia de LED ao vivo** dentro da moldura do equipamento (com parafusos e a
  etiqueta LEDBLOCK) — mostra exatamente o que vai para o painel, no tamanho real
- Leitura técnica em fonte monoespaçada: `PRÉVIA AO VIVO · MEIA · HORIZONTAL · 96×92`
- **Orientação** em controle segmentado (Horizontal / Vertical)
- **Modelos** por segmento — um toque preenche cabeçalho, medida e rodapé
- Interruptores de **Meia tela** e **Habilitar quadro**
- Campos do produto: Cabeçalho, Título, Subtítulo, Preço, Medida, Auxiliar, Rodapé

---

## Enviar — transmissão

<img src="capturas/02-enviar.png" alt="Aba Enviar no tema escuro" width="320">

Escolha do álbum, do transporte (**Rede Wi-Fi** ou **USB**) e do painel de destino.
Durante a transferência aparece um anel de progresso com o percentual; o card
**Status** registra o que está acontecendo em fonte monoespaçada.

Quando o painel de destino já é conhecido, uma **barra de memória** avisa se o
álbum cabe **antes** de enviar.

---

## Painéis — o parque de equipamentos

<img src="capturas/03-paineis.png" alt="Aba Painéis no tema escuro" width="320">

Ao abrir, o app **varre a rede sozinho** — daí o estado "Procurando painéis…".
Cada painel encontrado vira um card com status (online / instável / offline),
**brilho**, **auto-brilho por sensor de luz**, **selo de sincronismo (CRC)** e os
botões Ligar / Desligar / Identificar / Renomear.

Abaixo, a configuração do **Wi-Fi do painel via USB** — que só fica ativa com o
cabo OTG conectado.

---

## Agenda — programação

<img src="capturas/04-agenda.png" alt="Aba Agenda no tema escuro" width="320">

Agendamento de envio por data e hora, com opção **Diariamente** e **brilho por
tarefa**. Útil para trocar as ofertas da manhã e da tarde automaticamente.

> ⚠️ Hoje o agendamento dispara **com o app aberto** — ver
> [DECISOES §11](DECISOES.md#11-agendamento-em-segundo-plano-adiado).

---

## Config — ajustes e diagnóstico

<img src="capturas/05-config.png" alt="Aba Config no tema escuro" width="320">

**Tema** (Sistema / Claro / Escuro), **cor do LED na prévia** para combinar com o
painel real, **efeito global das telas**, rede, senha de transmissão e os dois
diagnósticos (do dispositivo e do protocolo).

---

## Tema claro

O app segue o tema do sistema, ou você força em **Config → Tema**.

<p>
<img src="capturas/07-editar-claro.png" alt="Aba Editar no tema claro" width="320">
&nbsp;&nbsp;
<img src="capturas/06-config-claro.png" alt="Aba Config no tema claro" width="320">
</p>

> 💡 Repare que **a placa de LED continua preta** nos dois temas — ela representa um
> equipamento físico, que não muda de cor porque o celular está no modo claro.

---

## Como estas capturas foram feitas

Emulador headless, sem intervenção manual:

```bash
# criar e subir o emulador (renderização por software no convidado)
avdmanager create avd -n painel -k "system-images;android-35;default;x86_64" -d pixel_6
emulator -avd painel -no-window -no-audio -no-boot-anim -gpu guest

# instalar e abrir
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n br.com.painelofertas/.ui.MainActivity

# tema escuro e captura
adb shell cmd uimode night yes
adb exec-out screencap -p > docs/capturas/01-editar.png
```

> ⚠️ Com `-gpu swiftshader_indirect` o `screencap` devolve uma imagem **preta** —
> o app renderiza numa superfície acelerada que não entra no framebuffer capturado.
> **`-gpu guest`** força renderização por software no convidado e resolve.
