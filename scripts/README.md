# scripts/

Scripts que uso no dia a dia da loja, todos para Windows. Rode sempre a partir da raiz do
projeto (a pasta com o `pom.xml`), por exemplo `.\scripts\rodar-loja-local.ps1`: cada um
acha o caminho do projeto sozinho, mas os exemplos daqui partem de lá.

| Script | O que faz | Quando uso |
|---|---|---|
| `Abrir Loja PAFANNY.bat` | Sobe o sistema a partir do jar em `target\` e abre o navegador. Na primeira vez, chama o `gerar-credenciais.ps1`. | No computador da loja, com dois cliques. |
| `gerar-credenciais.ps1` | Gera a chave JWT e a senha do admin e grava em `.env.producao.bat`, na raiz (esse arquivo nunca vai para o Git). | Só na primeira vez; o `.bat` acima chama sozinho. |
| `rodar-loja-local.ps1` | Sobe a loja em `http://localhost:8081` usando uma **cópia** do banco e das fotos (em `%TEMP%\pafanny-teste`), com senhas descartáveis. | Antes de publicar qualquer mudança, e para rodar os testes do `e2e/`. |
| `atualizar-precos-nomes.ps1` | Muda preço e/ou nome dos modelos (categorias) pela API, mostrando uma prévia antes de gravar. As regras ficam no topo do arquivo. | Quando a loja reajusta preços ou renomeia um modelo. |
| `enviar-para-railway.ps1` | Baixa um backup do banco local, junta com as fotos e envia para o servidor no ar. O servidor aplica no próximo reinício. | Raramente: só para levar o banco local inteiro para produção. **Substitui todo o banco do servidor.** |
| `publicar-github.ps1` | `git add`/`commit`/`push` com uma varredura simples por segredos antes. | Era o jeito de publicar no começo; hoje publico com git direto, seguindo o `docs/RECUPERACAO.md`. Só roda na branch passada em `-Branch` (padrão `main`); em outra, para antes de commitar. Lembre que o push na `main` publica o site. |

Lembretes:

- Push na `main` publica o site (o Railway faz o deploy sozinho). O passo a passo seguro está
  em [`docs/RECUPERACAO.md`](../docs/RECUPERACAO.md).
- Os `.ps1` sem acento são de propósito: o Windows PowerShell 5.1 lê arquivo sem BOM como ANSI
  e estragaria os acentos. O `rodar-loja-local.ps1` tem acentos porque está salvo com BOM.
- Se o Windows bloquear a execução, rode uma vez na sessão:
  `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass`.
