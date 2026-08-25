# Contribuindo

Obrigado por olhar este projeto. Este guia diz **como mexer sem quebrar o que já
funciona** — especialmente a compatibilidade com o painel físico e com o app Windows.

> 🚦 Primeira vez aqui? Leia **[docs/COMECE-AQUI.md](docs/COMECE-AQUI.md)** antes.

---

## Antes de escrever código

1. **Rode os testes.** `./gradlew test` — se algo já estava quebrado, o problema não é seu.
2. **Leia as [armadilhas](docs/COMECE-AQUI.md#6-armadilhas--leia-antes-de-mexer).**
   Boa parte das "esquisitices" do código é **fidelidade proposital** ao sistema
   original. "Consertar" quebra o painel.
3. **Abra uma issue** se a mudança for grande, para combinarmos a direção antes.

---

## A regra mais importante

> ### 🔒 Mudou algo em `protocol/`? O teste é obrigatório — e compara **bytes**.

O pacote `protocol/` é o contrato com o hardware **e** com o app Windows. Ele tem
vetores de teste reais (`nelPai` → 49 bytes / CRC `0xD644`; `mensagem` → 111 bytes
/ CRC `0x06A2`). Se um deles quebrar:

- **provavelmente foi você, não o teste**;
- um álbum gerado no Android pode deixar de abrir no PC;
- no pior caso, o painel na loja mostra lixo.

Não ajuste o valor esperado do teste para "passar". Entenda por que mudou.

---

## Padrões do código

| Regra | Por quê |
|---|---|
| `protocol/` **não importa Android** | É o que mantém os testes rodando na JVM, rápido, sem emulador |
| Comentários e nomes em **português** quando for termo do domínio (`quadro`, `álbum`, `consumo`) | O domínio veio do sistema original; traduzir confunde |
| Rotina portada do Delphi cita a origem | Ex.: `// porte de Monta_Oferta (Ofertas.pas:1600-1750)` — facilita conferir a fidelidade |
| Estado de tela em **ViewModel** | Sobrevive a rotação e troca de aba |
| Erro de rede/USB **nunca** derruba o app | `runCatching` / try-catch nas bordas de I/O |
| Texto que vai para o fio passa por `AccentMap.normalize()` | Impede glifo errado e injeção de `0xFF`/`CR` |

**Estilo:** o padrão do Kotlin/Android Studio. Sem ferramenta de lint obrigatória —
mantenha a consistência com os arquivos ao redor.

---

## Fluxo de trabalho

```bash
git checkout -b feat/minha-mudanca
# … código + teste …
./gradlew test
git commit -m "feat: descrição curta no imperativo"
git push -u origin feat/minha-mudanca
```

Prefixos de commit: `feat:` · `fix:` · `docs:` · `refactor:` · `test:` · `chore:`

### Checklist do PR

- [ ] `./gradlew test` passa
- [ ] Mudança em `protocol/` tem teste comparando bytes
- [ ] `versionCode` incrementado, se for gerar APK para instalar
- [ ] Documentação atualizada, se mudou comportamento
      ([PROTOCOLO](docs/PROTOCOLO.md) · [ARQUITETURA](docs/ARQUITETURA.md) · [RECEITAS](docs/RECEITAS.md))
- [ ] `CHANGELOG.md` atualizado
- [ ] Descrito **o que foi testado em hardware real**, se aplicável

---

## Testar sem hardware

Dá para desenvolver quase tudo sem um painel:

| O quê | Como |
|---|---|
| Codec, CRC, fontes, layout | `./gradlew test` |
| Máquina de transferência completa | `TransferEngineTest` — implementa um `PanelLink` falso que responde `APAGADO`/`NEXT=` como o painel |
| Integridade do protocolo no aparelho | App → **Config → Diagnóstico do protocolo** (deve dar `✅ OK — 49 bytes, CRC 0xD644`) |

O que **só** o painel físico confirma está listado em
[HANDOFF.md](HANDOFF.md#o-que-precisa-da-validação-de-vocês).

---

## Reportando um problema

Abra uma [issue](../../issues) com:

- **O que aconteceu** e o que você esperava
- **Onde**: modelo do aparelho, versão do Android, versão do app (Config → Sobre)
- **Como conectou**: Wi-Fi ou USB
- **Havia painel real envolvido?** — isso muda completamente o diagnóstico
- Mensagem de status exibida na tela, se houver

Para envio que falha, a tabela
[sintoma → causa → solução](docs/RECEITAS.md#11-depurar-o-envio-falha) já resolve
boa parte dos casos.

---

## Licença

Este projeto **não é open source** — veja [LICENSE](LICENSE). O produto, a marca e
o protocolo pertencem à **LedBlock Indicadores Inteligentes**. Ao contribuir, você
concorda que sua contribuição segue os mesmos termos.
