# ❓ Perguntas frequentes

Dúvidas que aparecem antes de mergulhar na documentação técnica.

---

## Sobre o app

<details>
<summary><b>Preciso de internet para usar?</b></summary>

**Não.** Toda a comunicação é na **rede local** (UDP) ou por **cabo USB**. O app
nunca acessa a internet — nem tem permissão para isso além do necessário para a
rede local.

</details>

<details>
<summary><b>Funciona no iPhone?</b></summary>

**Não, e não é planejado.** O app depende de **USB HID**, que o iOS não permite a
aplicativos comuns. Sem esse caminho não dá para configurar a rede do painel.
Detalhe da decisão em [DECISOES.md §1](DECISOES.md#1-kotlin-nativo-em-vez-de-flutter).

</details>

<details>
<summary><b>Quantos painéis o app controla?</b></summary>

O registro interno suporta **até 100 painéis** (mesmo limite do sistema original).
Cada um aparece com nome próprio, status e brilho independentes. A varredura cobre
uma sub-rede **/24** (254 endereços).

</details>

<details>
<summary><b>O álbum criado no celular abre no programa do PC?</b></summary>

**Sim.** O formato `.alb` é idêntico: mesmo cabeçalho de 4 linhas, mesmos blocos,
mesma codificação (ISO-8859-1). É possível copiar o arquivo de um lado para o
outro. Como extrair o `.alb` do celular:
[RECEITAS §12](RECEITAS.md#12-ler-o-alb-gerado).

</details>

<details>
<summary><b>Preciso de USB OTG mesmo usando Wi-Fi?</b></summary>

Só na **primeira vez**, para dizer ao painel em qual rede entrar (`AT+SAVETRANSLINK`).
Depois disso ele fica na rede e tudo funciona por Wi-Fi. Se o painel já estiver
configurado, você nunca precisa do cabo.

</details>

<details>
<summary><b>Por que preciso permitir "fontes desconhecidas"?</b></summary>

Porque o APK não vem da Play Store. É um APK de **debug**, assinado com a chave de
desenvolvimento. Para distribuir de verdade, gere um APK de release com keystore
próprio — ver [DECISOES.md §13](DECISOES.md#13-apk-de-debug-sem-chave-de-release).

</details>

<details>
<summary><b>O agendamento funciona com o app fechado?</b></summary>

**Ainda não.** Hoje ele dispara enquanto o app está aberto. Fazer isso funcionar
com o app fechado exige validação real de vários dias por causa do *Doze mode* e
das otimizações de bateria — e um agendamento que falha em silêncio é pior que não
ter. Ver [DECISOES.md §11](DECISOES.md#11-agendamento-em-segundo-plano-adiado).

</details>

---

## Sobre o painel

<details>
<summary><b>Dá para deixar o painel colorido?</b></summary>

**Não nesta geração de hardware.** A matriz é **monocromática** — o protocolo não
tem campo de cor. A opção "cor do LED" em Config muda apenas a **prévia na tela**,
para você combinar visualmente com a cor real do seu painel.

</details>

<details>
<summary><b>Dá para girar o painel para vertical?</b></summary>

O editor tem um **modo retrato**, que muda o *layout* do conteúdo (você compõe em
pé). Mas a **rotação real da imagem** depende do firmware do controlador — se ele
não girar, o texto sai deitado. É uma das perguntas em aberto para a LedBlock
(ver [HANDOFF](../HANDOFF.md#perguntas-para-a-ledblock-destravariam-novas-funções)).

</details>

<details>
<summary><b>Por que o texto perde os acentos?</b></summary>

**Não perde — eles são remapeados.** As fontes do painel só têm glifos para ASCII
32–108. Como o texto é forçado a maiúsculas, os slots minúsculos `a`–`l` ficam
livres e guardam os 12 acentuados do português (`Ç` vira `i`, `Ã` vira `b`…). O
painel desenha o glifo acentuado correto. Tabela completa em
[PROTOCOLO §5](PROTOCOLO.md#5-texto-e-acentos).

</details>

<details>
<summary><b>Ativei o subtítulo e o cabeçalho sumiu. É bug?</b></summary>

**Não.** Eles ocupam a mesma linha no topo e são excludentes — comportamento do
sistema original. Subtítulo ligado ⇒ **Título + Subtítulo**. Desligado ⇒
**Cabeçalho + Título**. O app deixa isso explícito escondendo o campo.

</details>

<details>
<summary><b>O que é o "auto-brilho"?</b></summary>

O painel tem um **sensor de luz ambiente**. Ligando o auto-brilho, ele clareia de
dia e escurece à noite sozinho. Tecnicamente: soma-se **128** ao byte de brilho
(`INICIAR=228` = brilho 100 + sensor ligado). O app mostra a leitura do sensor ao vivo.

</details>

<details>
<summary><b>O que significa "⚠ Desatualizado" no painel?</b></summary>

O painel informa o **CRC do conteúdo gravado nele**. O app compara com o CRC do
álbum que você enviou. Se forem diferentes, o painel **não está exibindo o que você
mandou** — reenvie. Se nunca houve envio por este app, aparece "Sem referência".

</details>

<details>
<summary><b>Cabe quanto conteúdo no painel?</b></summary>

Depende do modelo. O painel informa a memória livre no `STATUS=`, e o app calcula
antes de enviar — a barra na aba Enviar mostra **"cabe"** ou **"NÃO cabe"** com o
tamanho em bytes.

</details>

---

## Sobre o projeto

<details>
<summary><b>Isso é um produto oficial da LedBlock?</b></summary>

**Não.** É um porte desenvolvido de forma independente a partir do software Windows
original, com o objetivo de ser **entregue à LedBlock**. A marca, o hardware e o
protocolo pertencem à empresa. Ver [LICENSE](../LICENSE).

</details>

<details>
<summary><b>Como o protocolo foi descoberto?</b></summary>

Foi **extraído do código-fonte Delphi** do próprio app Windows (`Ofertas.pas`,
8.546 linhas), não por engenharia reversa do tráfego. Cada trecho portado cita a
rotina de origem, e os testes comparam a saída **byte a byte** com arquivos reais
gerados pelo programa original.

</details>

<details>
<summary><b>Posso usar este código no meu projeto?</b></summary>

**Não sem autorização.** O repositório é público para consulta e avaliação técnica,
mas nenhuma licença de código aberto é concedida — ver [LICENSE](../LICENSE).

</details>

<details>
<summary><b>O app foi testado num painel de verdade?</b></summary>

**Ainda não** — é o item nº 1 do roadmap. Tudo foi validado com testes automatizados
(comparando bytes contra arquivos reais) e com um **painel simulado**. O
[HANDOFF.md](../HANDOFF.md) traz um roteiro de 10 passos para a validação em bancada.

</details>

<details>
<summary><b>Achei um trecho de código estranho. Conserto?</b></summary>

**Provavelmente não.** Boa parte das esquisitices é **fidelidade proposital** ao
protocolo (CRC que ignora 2 bytes, endianness assimétrico, `N_DATA = 30`…). O painel
espera exatamente esses bytes. Leia
[as armadilhas](COMECE-AQUI.md#6-armadilhas--leia-antes-de-mexer) e
[DECISOES §2](DECISOES.md#2-fidelidade-ao-original-acima-de-boas-práticas) antes.

</details>

<details>
<summary><b>Mudei o código, gerei o APK, e nada mudou no app.</b></summary>

Você esqueceu de aumentar o **`versionCode`** em `app/build.gradle.kts`. O Android
não substitui um APK instalado que tenha o mesmo `versionCode`. É a pegadinha nº 1
do projeto — ver [RECEITAS §1](RECEITAS.md#1-gerar-uma-versão-para-instalar).

</details>

---

Não achou sua pergunta? Abra uma [issue](../../issues) — ou veja
[COMECE-AQUI](COMECE-AQUI.md) · [ARQUITETURA](ARQUITETURA.md) ·
[PROTOCOLO](PROTOCOLO.md) · [RECEITAS](RECEITAS.md) · [DECISOES](DECISOES.md).
