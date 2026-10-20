# Recuperação e publicação — melhorias de efeitos da loja

Guia em passos simples para publicar, voltar atrás e desligar efeitos.

**Como o site vai ao ar:** todo `git push` na branch `main` do GitHub
(`CodeVinnycius/GestaoNexus-Pafanny`) faz o **Railway** montar o `Dockerfile` e
publicar sozinho em `pafanny.com.br`. Não existe outra etapa.

**Ponto de restauração:** a tag `producao-antes-efeitos-2026-10-20` (commit `b104000`)
é exatamente a versão que estava no ar antes das melhorias.

**O banco de dados não é alterado por nenhuma destas mudanças** — publicar ou voltar
atrás não mexe em produtos, preços nem fotos.

---

## 1. Publicar a nova versão (quando aprovar)

Rode no PowerShell, dentro da pasta `EstoqueSpring`:

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

1. Abra o painel do Railway → projeto **GestaoNexus-Pafanny** → serviço **GestaoNexus** →
   **Deployments**. Espere o deploy novo ficar **Active** (uns 3–5 minutos).
2. Abra `https://pafanny.com.br` numa janela anônima e confira o topo, o catálogo e um
   "Quero esse pijama" (ele deve abrir o WhatsApp com a mensagem da peça).
3. Se algo estiver estranho, siga o passo 2 abaixo.

---

## 2. Voltar para a versão anterior

### Opção A — pelo Railway (mais rápida, sem mexer no git)

1. Painel do Railway → **GestaoNexus** → **Deployments**.
2. Encontre o deploy anterior (commit `b104000`, "remove o crédito by VinnyciusDev").
3. Clique nos três pontinhos dele → **Rollback**. Em 1–2 minutos o site antigo volta.

Atenção: a `main` no GitHub continua com as melhorias. O **próximo push** na `main`
publica tudo de novo. Para tirar de vez, faça também a Opção B.

### Opção B — pelo git (definitiva)

Desfaz as melhorias criando commits de "volta" (nada de histórico é apagado, sem force-push):

```powershell
git switch main
git pull --ff-only
git revert --no-edit producao-antes-efeitos-2026-10-20..main
git push origin main     # publica a volta
```

Conferência: `git diff producao-antes-efeitos-2026-10-20 main -- src` não deve mostrar
nada (o código da loja voltou a ser igual ao da tag).

---

## 3. Desligar só um efeito

Toda a "vida" nova está isolada em dois arquivos:

- `src/main/resources/static/css/efeitos.css`
- `src/main/resources/static/js/efeitos.js`

**Desligar tudo de uma vez:** em `src/main/resources/static/loja.html`, apague as linhas
`<link rel="stylesheet" href="/css/efeitos.css"/>` e
`<script src="/js/efeitos.js"></script>`. O visual volta ao original (as correções e o
catálogo sem JS continuam).

**Desligar um efeito específico** — apague o bloco indicado no `efeitos.css` (cada bloco
começa com um título `/* ── NOME ── */`) ou reverta o commit (`git revert <commit>`):

| Efeito | Onde fica | Commit |
|---|---|---|
| Grão de tecido | `efeitos.css`: troque `--grao-opacidade: .045` por `0` | `3a83c65` |
| Degradê entre seções e rodapé | `efeitos.css`, bloco **GRADIENTES** | `3a83c65` |
| Hero: manchas que respiram, traço no "abraça", zoom da foto | `efeitos.css`, bloco **HERO** | `6fcfbbd` |
| Botões com gradiente e brilho | `efeitos.css`, bloco **BOTÕES** | `3f25e2d` |
| Entrada em sequência e ícones da Essência | `efeitos.css`, blocos **REVEAL COM STAGGER** e **ESSÊNCIA** | `d10b887` |
| Catálogo: skeleton, 2ª foto, entrada dos cards | `efeitos.css` bloco **CATÁLOGO** + `efeitos.js` seção **CATÁLOGO** | `09c1457` |
| FAQ animado | `efeitos.css`, bloco **FAQ** | `0f3b6f5` |
| Header com blur, menu animado, pulsação do WhatsApp | `efeitos.css` bloco **HEADER…** + `efeitos.js` seção **HEADER E WHATSAPP** | `e8eab46` + `a531839` (reverta os dois juntos) |
| Corações no "Quero esse" | `efeitos.css` bloco **CORAÇÕES** + `efeitos.js` seção **CORAÇÕES** | `776c57e` |
| Peças em HTML para quem está sem JavaScript | `LojaPaginaController.java` | `2e755bb` + `5694b2c` (reverta os dois juntos, o mais novo primeiro) |

Não reverta `6b0d043` (correções): é ele que impede o topo da página de ficar em branco
quando a internet está lenta.

Depois de mudar qualquer coisa, publique como no passo 1 (só o `git push origin main`
se você já estiver na `main`).

---

## Testar localmente antes de publicar

O site local usa uma **cópia** do banco, nunca o de produção. Em PowerShell, na pasta
`EstoqueSpring` (a porta 8080 costuma estar ocupada por outro projeto, por isso 8081):

```powershell
$d = "$env:TEMP\pafanny-teste"; New-Item -ItemType Directory -Force $d | Out-Null
Copy-Item data\estoque.mv.db $d -Force
if (-not (Test-Path "$d\uploads")) { Copy-Item data\uploads $d -Recurse }
$env:SERVER_PORT = "8081"
$env:DB_URL = "jdbc:h2:file:$d\estoque;DB_CLOSE_ON_EXIT=FALSE"
$env:APP_UPLOADS_DIR = "$d\uploads"; $env:APP_DATA_DIR = $d
$env:APP_JWT_SECRET = [Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))
$env:APP_ADMIN_SENHA = "teste-local-" + (Get-Random)
$env:APP_LOJA_WHATSAPP = "5511986032979"; $env:APP_LOJA_INSTAGRAM = "usepafanny"
& "C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd" -o spring-boot:run "-Dspring-boot.run.addResources=true"
```

Abra `http://localhost:8081`. Para rodar os testes (outra janela, pasta `EstoqueSpring\e2e`):

```powershell
npm install
npx playwright test qualidade whatsapp      # Chrome, Firefox, Safari (WebKit), iPhone e Edge
$env:SHOTS = "atual"; npx playwright test screenshots --project=chromium
```

Os testes se recusam a rodar contra o site de produção.
