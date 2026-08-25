## O que muda

<!-- Descreva em uma ou duas frases. -->

## Por quê

<!-- Que problema resolve? Se houver issue, referencie: Resolve #123 -->

## Como testei

<!-- Marque o que se aplica -->
- [ ] `./gradlew test` passa
- [ ] Testei no app rodando (aparelho/emulador)
- [ ] **Testei contra um painel físico** — descreva o resultado abaixo

<!-- Se testou em hardware, conte o que aconteceu: -->

## Checklist

- [ ] Mudança em `protocol/` tem teste **comparando bytes**
      (veja [CONTRIBUTING](../blob/main/CONTRIBUTING.md#a-regra-mais-importante))
- [ ] `versionCode` incrementado, se este PR gera APK para instalar
- [ ] `CHANGELOG.md` atualizado
- [ ] Documentação ajustada, se o comportamento mudou
- [ ] Não "consertei" nenhuma esquisitice que é
      [fidelidade proposital ao protocolo](../blob/main/docs/DECISOES.md#2-fidelidade-ao-original-acima-de-boas-práticas)
