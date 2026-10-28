# Recuperação e publicação — melhorias da loja PAFANNY

Guia em passos simples para publicar, voltar atrás e desligar efeitos.

**Como o site vai ao ar:** todo `git push` na branch `main` do GitHub
(`CodeVinnycius/GestaoNexus-Pafanny`) faz o **Railway** montar o `Dockerfile` e publicar
sozinho em `pafanny.com.br`. Não existe outra etapa.

**Ponto de restauração:** a tag `producao-antes-efeitos-2026-10-20` (commit `b104000`) é
exatamente a versão que estava no ar antes das melhorias.

**O banco de dados não é alterado por nenhuma destas mudanças.** Publicar ou voltar atrás não
mexe em produtos, preços, fotos nem senhas. As fotos originais também nunca são alteradas
(as miniaturas são geradas à parte, numa pasta temporária do servidor).

---

## 1. Publicar a nova versão (quando aprovar)

No PowerShell, dentro da pasta `EstoqueSpring`:

```powershell
# 1. Guarda o ponto de restauração também no GitHub (não publica nada)
git push origin producao-antes-efeitos-2026-10-20

# 2. Leva as melhorias para a main (sem merge commit; falha se algo estiver diferente)
git switch main
git pull --ff-only
git merge --ff-only melhorias-efeitos

# 3. PUBLICA: o Railway começa o deploy assim que isto termina
git push origin main
```

Depois:

1. Painel do Railway → projeto **GestaoNexus-Pafanny** → serviço **GestaoNexus** →
   **Deployments**. Espere o deploy novo ficar **Active** (uns 3–5 minutos).
2. Abra `https://pafanny.com.br` numa janela anônima e confira: o topo, o catálogo, um
   "Quero esse pijama" (deve abrir o WhatsApp com a mensagem da peça) e o menu no celular.
3. Abra `https://pafanny.com.br/login.html` e entre no sistema, para confirmar que o admin
   continua normal.
4. No primeiro minuto depois do deploy o servidor prepara as miniaturas das fotos em segundo
   plano; até lá algumas fotos podem vir no tamanho original (só um pouco mais pesadas).
5. Se algo estiver estranho, siga o passo 2 abaixo.

---

## 2. Voltar para a versão anterior

### Opção A — pelo Railway (mais rápida, sem mexer no git)

1. Painel do Railway → **GestaoNexus** → **Deployments**.
2. Encontre o deploy anterior (commit `b104000`, "remove o crédito by VinnyciusDev").
3. Clique nos três pontinhos dele → **Rollback**. Em 1–2 minutos o site antigo volta.

Atenção: a `main` no GitHub continua com as melhorias e o **próximo push** na `main`
publica tudo de novo. Para tirar de vez, faça também a Opção B.

### Opção B — pelo git (definitiva)

Desfaz todas as melhorias criando commits de "volta" (nada do histórico é apagado, sem
force-push). **Testado: o código volta idêntico ao da tag, sem conflitos.**

```powershell
git switch main
git pull --ff-only
git revert --no-edit producao-antes-efeitos-2026-10-20..main
git push origin main     # publica a volta
```

Conferência: `git diff producao-antes-efeitos-2026-10-20 main -- src` não mostra nada.

---

## 3. Desligar só um efeito

**Importante:** desfazer um grupo isolado com `git revert <commit>` **dá conflito na maioria
dos grupos**, porque os grupos seguintes mexeram nos mesmos trechos dos mesmos arquivos
(testado commit a commit). O jeito seguro é desligar pelo arquivo, como abaixo. Também dá
para me pedir: eu faço a remoção como um commit novo e testo.

Toda a "vida" nova fica em arquivos próprios, separados da loja original:

| Arquivo | O que tem |
|---|---|
| `src/main/resources/static/css/efeitos.css` | todos os estilos dos efeitos, em blocos com nome (`/* ── NOME ── */`) |
| `src/main/resources/static/js/efeitos.js` | catálogo, chips, inclinação dos cards, header, corações |
| `src/main/resources/static/js/rolagem.js` | scroll suave, header que some, progresso, topo, animações de rolagem |
| `src/main/resources/static/js/secoes.js` | depoimentos, galeria, botão de pausar a faixa |
| `src/main/resources/static/js/vendor/lenis.min.js` | biblioteca do scroll suave (MIT) |

**Desligar tudo de uma vez:** em `src/main/resources/static/loja.html`, apague as linhas do
`efeitos.css`, `efeitos.js`, `rolagem.js` e `secoes.js`. O visual volta ao original. As
correções e as melhorias de velocidade continuam valendo. A faixa, os depoimentos e a galeria
somem sozinhos, porque vêm com `hidden` no HTML.

**Desligar um efeito específico:**

| Efeito | Como desligar |
|---|---|
| Grão de tecido | `efeitos.css`, no topo: troque `--grao-opacidade: .045` por `0` |
| Degradê entre seções | `efeitos.css`: apague o bloco **GRADIENTES** |
| Ondas entre seções | `efeitos.css`: apague o bloco **ONDAS ENTRE AS SEÇÕES** |
| Hero: manchas que respiram, traço no "abraça", zoom da foto | `efeitos.css`: bloco **HERO** |
| Hero: foto em arco, enfeites, "role para baixo" | `efeitos.css`: bloco **HERO RICO** |
| Botões com gradiente e brilho | `efeitos.css`: bloco **BOTÕES** |
| Faixa com as palavras da marca | `loja.html`: apague o `<div class="fx-faixa">` inteiro |
| Nossas linhas (cards grandes, título que sobe) | `efeitos.css`: bloco **NOSSAS LINHAS** |
| Cards: inclinação 3D | `efeitos.js`: apague a seção **CARDS: inclinação 3D** |
| Cards: brilho, sombra dourada, olhar rápido, Novidade, chips, modal | `efeitos.css`: bloco **CARDS E CATÁLOGO** |
| Catálogo: skeleton, 2ª foto, entrada dos cards | `efeitos.css` bloco **CATÁLOGO** + `efeitos.js` seção **CATÁLOGO** |
| FAQ animado | `efeitos.css`: bloco **FAQ** |
| Corações no "Quero esse" | `efeitos.js`: seção **CORAÇÕES** |
| Pulsação do WhatsApp | `efeitos.css`: no bloco **HEADER, MENU MOBILE E WHATSAPP FLUTUANTE**, apague `.fx-fab-pulso` |
| Scroll suave (Lenis) | `loja.html`: apague o atributo `data-lenis="..."` do `<script src="/js/rolagem.js">` |
| Animações guiadas pelo scroll (cascata, desfoque, palavras, parallax, linha) | `rolagem.js`: troque a linha `const SCROLL_DRIVEN = ...` por `const SCROLL_DRIVEN = false;` (volta o reveal simples) |
| Header que some ao rolar | `efeitos.css`: no bloco **ROLAGEM**, apague `.header.fx-escondido { ... }` |
| Barra de progresso | `efeitos.css`: no bloco **ROLAGEM**, acrescente `.fx-progresso { display: none; }` |
| Botão "voltar ao topo" | `efeitos.css`: no bloco **ROLAGEM**, acrescente `.fx-topo { display: none !important; }` |
| Tom de fundo por seção | `efeitos.css`: no bloco **ROLAGEM**, acrescente `.fx-aura { display: none; }` |
| Depoimentos | `loja.html`: apague a `<section ... id="depoimentos">` inteira (ou, no bloco **DEPOIMENTOS** do `efeitos.css`, apague a linha `.fx-depoimentos[hidden] { display: block; }`) |
| Galeria estilo Instagram | `loja.html`: apague o `<ul class="fx-galeria" ...></ul>` |

Grupos de commits que **podem** ser revertidos sozinhos sem conflito: `1b10ca7` (fontes
locais), `09dda07` (responsividade), `80791ef` (ajustes de performance). Os demais, pelo
arquivo, como na tabela.

Depois de mudar qualquer coisa, teste localmente (seção 5) e publique como no passo 1.

---

## 4. Conteúdo para preencher

- **Depoimentos:** já estão preenchidos com 3 depoimentos reais (Camila R., Patrícia L. e
  Vanessa P.). Para trocar ou acrescentar, veja o comentário logo acima de
  `<section ... id="depoimentos">` no `loja.html`: copie um `<figure>`, troque o texto e o nome
  e atualize o "1 de N". Use só depoimentos **reais**, com autorização da cliente.
- **Badge "Novidade":** no `efeitos.js`, em `const CODIGOS_NOVIDADE = [];`, coloque os códigos
  das peças novas, ex.: `['1915', '747']`. Para só visualizar antes: abra a loja com
  `?novidades=1915,747` no fim do endereço.

---

## 5. Testar localmente antes de publicar

O site local usa uma **cópia** do banco e das fotos, nunca as originais. No PowerShell, na pasta
`EstoqueSpring`:

```powershell
.\scripts\rodar-loja-local.ps1    # abre em http://localhost:8081
```

Testes (outra janela, na pasta `EstoqueSpring\e2e`; na primeira vez rode `npm install`):

```powershell
npx playwright test qualidade whatsapp             # Chrome, Firefox, Safari, iPhone, Android e Edge
npx playwright test responsivo --project=chromium  # 17 tamanhos de tela, com screenshots
node ferramentas/teste-bloqueador.js               # com uBlock Origin Lite e regras da EasyList
```

Os testes se recusam a rodar contra o site de produção.
