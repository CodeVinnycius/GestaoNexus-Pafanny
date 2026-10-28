# Contribuindo

Este projeto roda a loja de verdade, então o cuidado principal é um só: **nada chega na
`main` sem ter sido testado**, porque cada push na `main` publica o site automaticamente.

## Antes de começar

1. Leia o [README](README.md) para subir o projeto localmente.
2. Crie uma branch a partir da `main`, com um nome que diga o assunto:
   `melhorias-catalogo`, `corrige-receita`, `docs-api`.
3. Nunca use um banco real para testar. No Windows, o `scripts/rodar-loja-local.ps1` sobe
   tudo com uma cópia do banco e senhas descartáveis.

## Commits

Um commit por assunto, com a mensagem em português no formato `tipo(escopo): o que muda`:

```
feat(loja): selo "Novidade" nas peças novas
fix: receita do painel aparecia negativa
docs: explica a restauração de backup no README
refactor: padroniza nomes em português no back-end
```

Tipos usados aqui: `feat`, `fix`, `perf`, `refactor`, `test`, `docs` e `chore`. O escopo é
opcional; `loja` é o mais comum (tudo que é da vitrine).

Commits pequenos e separados facilitam um `git revert` se algo der errado no ar.

## Antes de abrir o PR

- `mvn test` passando.
- Se mexeu na vitrine, rode os testes do `e2e/` contra o site local:

  ```bash
  cd e2e
  npx playwright test qualidade whatsapp
  ```

- Confira no celular (ou no modo responsivo do navegador) que nada ficou cortado e que o
  botão "Quero esse" ainda abre o WhatsApp com a mensagem da peça.
- Nenhum segredo no código: senhas, chaves e números ficam em variáveis de ambiente
  (veja `application.properties.example`).

## Algumas convenções

- Código, comentários e mensagens em português. Nomes que são convenção do Spring ou do Java
  (`filterChain`, `doFilterInternal`...) ficam como são.
- Comentário bom explica o porquê; o quê, o código já diz.
- Efeitos visuais da vitrine ficam isolados em `css/efeitos.css`, `js/efeitos.js`,
  `js/rolagem.js` e `js/secoes.js`: sem esses arquivos, a página tem que continuar completa.
- Nada de conteúdo que só aparece com JavaScript. O estado inicial de uma animação só vale
  quando o `<html>` tem a classe `.js`.
- Evite nomes de classe ou arquivo com `ads`, `banner`, `track`, `promo` ou `sponsor`:
  bloqueadores de anúncio escondem esses elementos.

## Publicação

Quem publica é o dono do projeto, seguindo o [`docs/RECUPERACAO.md`](docs/RECUPERACAO.md),
que também explica como voltar atrás.
