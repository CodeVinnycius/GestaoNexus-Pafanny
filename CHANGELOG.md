# Changelog

As mudanças que importam, da mais recente para a mais antiga. O projeto não tem versões
numeradas: cada push na `main` vai para o ar, então as entradas são organizadas por data.
O detalhe de cada item está no `git log`.

## Ainda não publicado

### Segurança
- Separação entre empresas reforçada: o cadastro de produto não aceita mais `id`, fotos nem
  variações vindos do cliente; remover uma foto confere se ela é do produto; o token só vale
  para a empresa que o recebeu, e o papel de admin vem do banco.
- Login não revela mais quais logins existem nem se a empresa está inativa para quem não
  sabe a senha.
- O script `publicar-github.ps1` não renomeia mais a branch de trabalho para `main`.

### Correções
- A receita do painel aparecia negativa.
- O ajuste de estoque aceita número negativo (por exemplo, para uma peça danificada).
- Mensagens de erro certas nas movimentações e no cadastro de empresas, sem erros internos
  do Java na tela.
- Duas vendas da mesma peça ao mesmo tempo não vendem mais do que o estoque.
- Nome de empresa com aspas não quebra mais o botão de remover no painel do admin.

### Testes
- Testes da API para separação entre empresas, login, tokens, movimentações e cadastro de
  empresas. O `mvn test` roda sem variáveis de ambiente e nunca abre o banco real.

### Organização
- Scripts saem da raiz para `scripts/`, com um `scripts/README.md` explicando cada um.
- README reescrito, com as decisões técnicas, desafios, próximos passos e ferramentas, e print
  da vitrine com o visual atual.
- `CONTRIBUTING.md` e este `CHANGELOG.md`.
- Comentários do back-end revisados e nomes padronizados em português, sem mudar endpoints
  nem JSON.

## 2026-10-27

### Vitrine
- Google passa a enxergar o catálogo: o `robots.txt` libera `/api/loja/`, de onde vêm os
  dados das peças.
- Imagem de compartilhamento horizontal (1200×630), para a prévia grande no WhatsApp e nas
  redes, e tags `twitter:card`.
- `favicon.ico` e ícone para a tela inicial do iPhone (antes respondiam 403).
- Dados das peças para o Google sempre com o domínio oficial, até 3 fotos, categoria e
  oferta completa; peças sem preço ficam de fora.

## 2026-10-23

- Depoimentos reais de clientes no lugar dos exemplos.
- Publicação de todas as melhorias da vitrine feitas em 20/10 (abaixo).

## 2026-10-20: vitrine com mais vida

### Visual e movimento
- Degradês entre as seções e uma textura leve de tecido.
- Hero com gradiente que "respira", foto em arco, traço desenhado no "abraça" e zoom lento.
- Botões com gradiente e brilho no hover; coraçõezinhos ao clicar em "Quero esse".
- Entrada suave dos elementos ao rolar, com animações guiadas pelo scroll em CSS e reserva
  por IntersectionObserver.
- Catálogo com skeleton no carregamento, segunda foto no hover, inclinação 3D e "olhar rápido".
- FAQ que abre e fecha com animação, header com blur ao rolar, menu mobile animado e
  pulsação discreta no botão do WhatsApp.
- Faixa com as palavras da marca, ondas entre seções, cards maiores em "Nossas linhas",
  galeria no estilo do Instagram e seção de depoimentos em carrossel.
- Rolagem suave no desktop (Lenis), barra de progresso e botão de voltar ao topo.

### Desempenho
- Respostas com gzip e cache de 1 ano nos arquivos da loja, com a versão no link.
- Fontes servidas pelo próprio site, com pré-carregamento.
- Hero em WebP responsivo e miniaturas das fotos das peças (as originais não mudam).

### Correções
- O hero e o FAQ não esperam mais a API para aparecer, e o conteúdo aparece mesmo se o
  JavaScript falhar.
- Sem JavaScript, a página já traz as peças principais e os links reais de WhatsApp.
- O HTML do `<noscript>` aparecia como texto no Chrome enquanto o catálogo carregava.
- A pulsação do WhatsApp passava da borda da tela no celular.
- Proporções e responsividade revistas em 17 tamanhos de tela.

### Testes e documentação
- Testes Playwright em `e2e/`: overflow horizontal, página sem JavaScript,
  `prefers-reduced-motion`, WhatsApp em todas as peças, bloqueador de anúncios real e
  screenshots antes/depois.
- `docs/RECUPERACAO.md` com publicação, rollback e como desligar cada efeito.

## 2026-10-18 e 19

- A raiz do domínio passa a abrir a loja; o sistema continua em `/login.html`.
- Recuperação de acesso do admin pela variável `APP_ADMIN_REDEFINIR_SENHA`.
- Script para atualizar preço e nome dos modelos pela API.
- SEO inicial: URL canônica, dados estruturados da loja e favicon.
- Foto principal fixa no topo da loja.
- Sai o crédito do desenvolvedor no rodapé.

## Setembro e começo de outubro de 2026

- Primeira publicação no Railway, com `Dockerfile`, `railway.toml` e volume para banco e fotos.
- Limite de tentativas de login (`429` depois de muitas tentativas erradas).
- Backup e restauração do banco e das fotos pelo painel do admin.
- `robots.txt` e `sitemap.xml` atualizados a cada troca de domínio, até `pafanny.com.br`.

## 2026-08-26: primeira versão no GitHub

- Sistema de estoque multi-tenant: empresas, produtos com custo e preço, movimentações,
  painel financeiro, admin e vitrine pública com WhatsApp.
- Autenticação com JWT e senhas com BCrypt; segredos só por variável de ambiente.
- Dados reais removidos do `application.properties` antes de publicar o repositório.
- Screenshots no README.
