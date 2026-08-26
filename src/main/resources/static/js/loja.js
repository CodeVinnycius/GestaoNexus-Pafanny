/**
 * PAFANNY — loja.js
 * Vitrine da marca: catálogo real (consumido de /api/loja), sem carrinho —
 * cada peça direciona direto para o WhatsApp. "O amor mora em cada detalhe."
 */
const $  = id => document.getElementById(id);
const API = '/api/loja';
const FAV_KEY = 'pafanny_favoritos';

// Ordem oficial das linhas PAFANNY (categorias sem produto real não aparecem — filtro é dinâmico).
const ORDEM_CATEGORIAS = [
  'Alcinha Viés', 'Alcinha Babado', 'Alcinha Botão',
  'Camiseta Botão',
  'Americano Curto', 'Americano Manga Longa', 'Americano Inverno'
];

const ROTULO_DISPONIBILIDADE = {
  DISPONIVEL: { texto: 'Disponível', classe: 'disponivel' },
  ULTIMAS_UNIDADES: { texto: 'Últimas unidades', classe: 'ultimas' },
  INDISPONIVEL: { texto: 'Indisponível', classe: 'indisponivel' },
  ESGOTADO: { texto: 'Esgotado', classe: 'esgotado' }
  // CONSULTAR não vira badge — vira o aviso "consulte pelo WhatsApp".
};

const PLACEHOLDER_IMG =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 200 200'%3E%3Crect width='200' height='200' fill='%23F4E9D8'/%3E%3Cpath d='M100 145s-38-22-38-52c0-15 12-27 27-27 6 0 12 3 11 8 5-5 11-8 17-8 15 0 27 12 27 27 0 30-44 52-44 52z' fill='none' stroke='%23D4AF37' stroke-width='2'/%3E%3C/svg%3E";

let produtos = [];
let config   = { nome: 'PAFANNY', whatsapp: '', instagram: '' };
let categoriaAtiva = null;
let mostrarSoFavoritos = false;
let produtoAtual = null;
let variacaoSelecionada = null;
let imagemAtivaIdx = 0;

// ══════════════════════════════════════════════════════════════════════════
//  CARREGAMENTO
// ══════════════════════════════════════════════════════════════════════════
async function iniciar() {
  try {
    const [resConfig, resProdutos] = await Promise.all([
      fetch(`${API}/config`),
      fetch(`${API}/produtos`)
    ]);
    config = await resConfig.json();
    produtos = ordenarPorCategoria(await resProdutos.json());

    document.title = `${config.nome} — Pijamas | O amor mora em cada detalhe`;
    ligarWhatsapp();
    ligarInstagram();
    renderHero();
    renderDestaque();
    renderCategorias();
    renderGrid();
    renderTamanhosChips();
    injetarDadosEstruturados();
    abrirProdutoPelaHash();
  } catch {
    $('grid-produtos').innerHTML = '<p class="loja-msg">Não foi possível carregar as peças agora. Tente novamente em instantes.</p>';
  }
  renderFaq();
  ativarMenuMobile();
  ativarReveal();
}

function ordenarPorCategoria(lista) {
  const peso = c => { const i = ORDEM_CATEGORIAS.indexOf(c); return i === -1 ? 999 : i; };
  return [...lista].sort((a, b) => peso(a.categoria) - peso(b.categoria));
}

// ══════════════════════════════════════════════════════════════════════════
//  WHATSAPP / INSTAGRAM
// ══════════════════════════════════════════════════════════════════════════
function mensagemWhatsapp(texto) {
  const numero = (config.whatsapp || '').replace(/\D/g, '');
  return `https://wa.me/${numero}?text=${encodeURIComponent(texto)}`;
}

function ligarWhatsapp() {
  const generica = `Olá! 💛 Gostaria de saber mais sobre os pijamas da ${config.nome || 'PAFANNY'}.`;
  ['cta-header-whatsapp', 'cta-mobile-whatsapp', 'cta-hero-whatsapp', 'fab-whatsapp', 'footer-whatsapp'].forEach(id => {
    const el = $(id);
    if (el) el.href = mensagemWhatsapp(generica);
  });
  const tamanhos = `Olá! 💛 Gostaria de ajuda para escolher o tamanho ideal de um pijama ${config.nome || 'PAFANNY'}.`;
  const elTam = $('cta-tamanhos-whatsapp');
  if (elTam) elTam.href = mensagemWhatsapp(tamanhos);
}

function ligarInstagram() {
  const handle = config.instagram || 'usepafanny';
  const url = `https://instagram.com/${handle}`;
  ['link-instagram', 'footer-instagram'].forEach(id => { const el = $(id); if (el) el.href = url; });
  const texto = $('texto-instagram');
  if (texto) texto.textContent = '@' + handle;
}

/** Mensagem do WhatsApp para um produto específico — formato pedido pela PAFANNY. */
function mensagemProduto(p) {
  const linhas = [
    `Olá! 💛`,
    `Vi este pijama no site da ${config.nome || 'PAFANNY'} e gostaria de saber se ele está disponível.`,
    '',
    `Produto: ${p.nome}`
  ];
  if (p.categoria) linhas.push(`Categoria: ${p.categoria}`);
  if (p.estampa)   linhas.push(`Estampa: ${p.estampa}`);
  linhas.push(`Preço: ${p.preco != null ? moeda(p.preco) : 'Consulte o valor'}`);
  if (p.codigo)    linhas.push(`Código: ${p.codigo}`);
  linhas.push('', 'Quais tamanhos estão disponíveis?');
  return linhas.join('\n');
}

// ══════════════════════════════════════════════════════════════════════════
//  FAVORITOS (localStorage, sem conta)
// ══════════════════════════════════════════════════════════════════════════
function getFavoritos() {
  try { return JSON.parse(localStorage.getItem(FAV_KEY)) || []; } catch { return []; }
}
function isFavorito(id) { return getFavoritos().includes(id); }
function alternarFavorito(id) {
  const atuais = getFavoritos();
  const idx = atuais.indexOf(id);
  if (idx >= 0) atuais.splice(idx, 1); else atuais.push(id);
  localStorage.setItem(FAV_KEY, JSON.stringify(atuais));
  return atuais.includes(id);
}

// ══════════════════════════════════════════════════════════════════════════
//  HERO + DESTAQUE
// ══════════════════════════════════════════════════════════════════════════
function renderHero() {
  const comFoto = produtos.find(p => p.imagens && p.imagens.length);
  if (comFoto) {
    $('hero-img').src = comFoto.imagens[0];
    $('hero-img').alt = `Pijama ${esc(comFoto.nome)} PAFANNY`;
    aplicarFallbackImagem($('hero-img'));
  } else {
    $('hero-figure').style.display = 'none';
  }
}

function renderDestaque() {
  const categorias = ordenarPorCategoria(
    [...new Set(produtos.map(p => p.categoria).filter(Boolean))].map(c => ({ categoria: c }))
  ).map(c => c.categoria);
  $('destaque-grid').innerHTML = categorias.map(cat => {
    const item = produtos.find(p => p.categoria === cat && p.imagens && p.imagens.length) || produtos.find(p => p.categoria === cat);
    if (!item) return '';
    return `
      <div class="destaque-card" data-cat="${escAttr(cat)}">
        ${item.imagens && item.imagens.length ? `<img src="${esc(item.imagens[0])}" alt="${escAttr(cat)}" loading="lazy"/>` : ''}
        <div class="destaque-card__label">
          <div class="destaque-card__nome">${esc(cat)}</div>
          <div class="destaque-card__preco">${item.preco != null ? 'a partir de ' + moeda(item.preco) : 'Consulte o valor'}</div>
        </div>
      </div>`;
  }).join('');
  $('destaque-grid').querySelectorAll('img').forEach(aplicarFallbackImagem);
  $('destaque-grid').querySelectorAll('.destaque-card').forEach(card => {
    card.onclick = () => {
      categoriaAtiva = card.dataset.cat;
      mostrarSoFavoritos = false;
      renderCategorias();
      renderGrid();
      document.getElementById('catalogo').scrollIntoView({ behavior: 'smooth', block: 'start' });
    };
  });
}

// ══════════════════════════════════════════════════════════════════════════
//  CATÁLOGO
// ══════════════════════════════════════════════════════════════════════════
function renderCategorias() {
  const categorias = ordenarPorCategoria(
    [...new Set(produtos.map(p => p.categoria).filter(Boolean))].map(c => ({ categoria: c }))
  ).map(c => c.categoria);
  if (categorias.length < 2) { $('categorias').style.display = 'none'; return; }
  $('categorias').style.display = 'flex';

  const temFavoritos = getFavoritos().length > 0;
  $('categorias').innerHTML = ['Todos', ...categorias].map(c => {
    const ativo = !mostrarSoFavoritos && ((c === 'Todos' && !categoriaAtiva) || c === categoriaAtiva);
    return `<button type="button" class="chip ${ativo ? 'active' : ''}" data-cat="${escAttr(c)}">${esc(c)}</button>`;
  }).join('') + (temFavoritos ? `<button type="button" class="chip ${mostrarSoFavoritos ? 'active' : ''}" id="chip-favoritos">♡ Favoritos</button>` : '');

  $('categorias').querySelectorAll('.chip[data-cat]').forEach(chip => {
    chip.onclick = () => {
      categoriaAtiva = chip.dataset.cat === 'Todos' ? null : chip.dataset.cat;
      mostrarSoFavoritos = false;
      renderCategorias();
      renderGrid();
    };
  });
  const chipFav = $('chip-favoritos');
  if (chipFav) chipFav.onclick = () => {
    mostrarSoFavoritos = !mostrarSoFavoritos;
    renderCategorias();
    renderGrid();
  };
}

function renderGrid() {
  let lista = produtos;
  if (mostrarSoFavoritos) lista = lista.filter(p => isFavorito(p.id));
  else if (categoriaAtiva) lista = lista.filter(p => p.categoria === categoriaAtiva);

  if (!lista.length) {
    $('grid-produtos').innerHTML = mostrarSoFavoritos
      ? '<p class="loja-msg">Você ainda não favoritou nenhum pijama. Toque no ♡ nas peças que gostar.</p>'
      : '<p class="loja-msg">Nenhuma peça disponível nessa categoria no momento.</p>';
    return;
  }

  $('grid-produtos').innerHTML = lista.map(p => {
    const badge = ROTULO_DISPONIBILIDADE[p.disponibilidade];
    const fav = isFavorito(p.id);
    return `
    <article class="produto-card" data-id="${p.id}">
      <div class="produto-card__img">
        ${p.imagens && p.imagens.length ? `<img src="${esc(p.imagens[0])}" alt="Pijama ${escAttr(p.nome)}" loading="lazy"/>` : '<span class="sem-foto" aria-hidden="true">♡</span>'}
        <button type="button" class="produto-card__fav ${fav ? 'ativo' : ''}" data-id="${p.id}" aria-label="${fav ? 'Remover dos favoritos' : 'Favoritar'}" aria-pressed="${fav}">
          <svg viewBox="0 0 24 24" fill="${fav ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M12 21s-7.5-4.6-10-9.3C.4 8.2 2 4.8 5.4 4.1c2-.4 4 .5 6.6 3 2.6-2.5 4.6-3.4 6.6-3C21.9 4.8 23.5 8.2 22 11.7 19.5 16.4 12 21 12 21z"/></svg>
        </button>
        ${badge ? `<span class="produto-card__badge ${badge.classe}">${badge.texto}</span>` : ''}
        <button type="button" class="produto-card__wpp" data-id="${p.id}" aria-label="Quero esse pijama pelo WhatsApp">
          <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12.04 2c-5.5 0-9.96 4.46-9.96 9.96 0 1.76.46 3.45 1.32 4.95L2 22l5.25-1.38a9.9 9.9 0 0 0 4.79 1.22h.01c5.5 0 9.96-4.46 9.96-9.96S17.54 2 12.04 2Zm5.86 14.06c-.25.7-1.24 1.29-2.02 1.45-.55.11-1.27.2-3.69-.79-3.1-1.28-5.1-4.41-5.25-4.61-.15-.2-1.26-1.67-1.26-3.19 0-1.52.79-2.26 1.08-2.57.24-.26.55-.35.78-.35.15 0 .3.01.43.02.34.02.51.04.72.5.25.55.85 1.98.92 2.13.07.15.12.32.02.52-.09.2-.14.32-.28.5-.14.17-.29.38-.42.5-.14.13-.29.28-.13.55.16.27.71 1.17 1.53 1.9 1.05.94 1.94 1.23 2.21 1.37.27.13.42.11.58-.07.16-.19.68-.79.87-1.06.18-.27.36-.22.6-.13.25.09 1.58.74 1.85.88.27.13.45.2.52.31.06.12.06.68-.19 1.38Z"/></svg>
        </button>
      </div>
      <div class="produto-card__info">
        ${p.categoria ? `<div class="produto-card__categoria">${esc(p.categoria)}</div>` : ''}
        <h3 class="produto-card__nome">${esc(p.nome)}</h3>
        <div class="produto-card__preco ${p.preco == null ? 'consultar' : ''}">${p.preco != null ? moeda(p.preco) : 'Consulte o valor'}</div>
        <span class="produto-card__link">Ver detalhes</span>
      </div>
    </article>`;
  }).join('');

  $('grid-produtos').querySelectorAll('img').forEach(aplicarFallbackImagem);
  $('grid-produtos').querySelectorAll('.produto-card').forEach(card => {
    card.addEventListener('click', e => {
      if (e.target.closest('.produto-card__wpp') || e.target.closest('.produto-card__fav')) return;
      abrirProduto(+card.dataset.id);
    });
  });
  $('grid-produtos').querySelectorAll('.produto-card__wpp').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const p = produtos.find(x => x.id === +btn.dataset.id);
      if (p) window.open(mensagemWhatsapp(mensagemProduto(p)), '_blank', 'noopener');
    });
  });
  $('grid-produtos').querySelectorAll('.produto-card__fav').forEach(btn => {
    btn.addEventListener('click', e => {
      e.stopPropagation();
      const ativo = alternarFavorito(+btn.dataset.id);
      btn.classList.toggle('ativo', ativo);
      btn.setAttribute('aria-pressed', String(ativo));
      btn.querySelector('svg').setAttribute('fill', ativo ? 'currentColor' : 'none');
      if (mostrarSoFavoritos) renderGrid();
      renderCategorias();
    });
  });
}

// ══════════════════════════════════════════════════════════════════════════
//  DETALHE DO PRODUTO
// ══════════════════════════════════════════════════════════════════════════
function abrirProduto(id) {
  const p = produtos.find(x => x.id === id);
  if (!p) return;
  produtoAtual = p;
  variacaoSelecionada = (p.variacoes && p.variacoes.length === 1) ? p.variacoes[0] : null;
  imagemAtivaIdx = 0;
  renderDetalhe();
  $('overlay-produto').classList.add('open');
  document.body.style.overflow = 'hidden';
  if (history.pushState) history.pushState(null, '', `#/pijamas/${slugProduto(p)}`);
}

function fecharProduto() {
  $('overlay-produto').classList.remove('open');
  document.body.style.overflow = '';
  if (location.hash && history.pushState) history.pushState(null, '', location.pathname + location.search);
}

function slugProduto(p) { return p.codigo || String(p.id); }

function abrirProdutoPelaHash() {
  const m = location.hash.match(/^#\/pijamas\/(.+)$/);
  if (!m) return;
  const slug = decodeURIComponent(m[1]);
  const p = produtos.find(x => x.codigo === slug || String(x.id) === slug);
  if (p) abrirProduto(p.id);
}

function renderDetalhe() {
  const p = produtoAtual;
  const imgs = p.imagens && p.imagens.length ? p.imagens : [];
  const galeria = imgs.length
    ? imgs.map((url, i) => `<img data-idx="${i}" src="${esc(url)}" alt="Foto do pijama ${escAttr(p.nome)}"/>`).join('')
    : '<span class="sem-foto" aria-hidden="true">♡</span>';

  const temVariacoes = p.variacoes && p.variacoes.length > 0;
  const badge = ROTULO_DISPONIBILIDADE[p.disponibilidade];
  const fav = isFavorito(p.id);

  $('conteudo-produto').innerHTML = `
    <div class="produto-detalhe__galeria" id="galeria-detalhe">${galeria}</div>
    ${imgs.length > 1 ? `<div class="produto-detalhe__dots" id="dots-detalhe">${imgs.map((_, i) => `<span class="${i === 0 ? 'ativo' : ''}"></span>`).join('')}</div>
    <div class="produto-detalhe__miniaturas" id="miniaturas-detalhe">${imgs.map((url, i) => `<img data-idx="${i}" src="${esc(url)}" class="${i === 0 ? 'ativa' : ''}" alt="Miniatura ${i + 1}"/>`).join('')}</div>` : ''}
    <div class="produto-detalhe__body">
      <div class="produto-detalhe__header">
        <div>
          ${p.categoria ? `<div class="produto-detalhe__categoria">${esc(p.categoria)}</div>` : ''}
          <h2 class="produto-detalhe__nome" id="detalhe-nome">${esc(p.nome)}</h2>
        </div>
        <div class="produto-detalhe__acoes">
          <button type="button" class="btn-icone-detalhe ${fav ? 'ativo' : ''}" id="btn-favoritar" aria-label="${fav ? 'Remover dos favoritos' : 'Favoritar'}" aria-pressed="${fav}">
            <svg viewBox="0 0 24 24" fill="${fav ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M12 21s-7.5-4.6-10-9.3C.4 8.2 2 4.8 5.4 4.1c2-.4 4 .5 6.6 3 2.6-2.5 4.6-3.4 6.6-3C21.9 4.8 23.5 8.2 22 11.7 19.5 16.4 12 21 12 21z"/></svg>
          </button>
          <button type="button" class="btn-icone-detalhe" id="btn-compartilhar" aria-label="Compartilhar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.6" y1="10.5" x2="15.4" y2="6.5"/><line x1="8.6" y1="13.5" x2="15.4" y2="17.5"/></svg>
          </button>
        </div>
      </div>
      ${p.estampa ? `<p style="font-size:.8125rem;color:var(--ink-mist);margin-bottom:10px;">Estampa: ${esc(p.estampa)}</p>` : ''}
      <div class="produto-detalhe__preco ${p.preco == null ? 'consultar' : ''}">${p.preco != null ? moeda(p.preco) : 'Consulte o valor'}</div>
      ${badge ? `<span class="disponibilidade-badge ${badge.classe}">${badge.texto}</span>` : ''}
      ${p.descricao ? `<p class="produto-detalhe__descricao">${esc(p.descricao)}</p>` : ''}

      ${temVariacoes ? `
        <span class="detalhe-label">Tamanho</span>
        <div class="tamanho-opcoes" id="opcoes-tamanho">
          ${p.variacoes.map(v => `
            <button type="button" class="tamanho-opcao ${variacaoSelecionada && variacaoSelecionada.id === v.id ? 'selecionado' : ''}" data-vid="${v.id}">
              ${esc(v.tamanho)}${v.cor ? ' · ' + esc(v.cor) : ''}
            </button>`).join('')}
        </div>` : ''}

      <button type="button" class="btn-whatsapp" id="btn-quero-esse">
        <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M12.04 2c-5.5 0-9.96 4.46-9.96 9.96 0 1.76.46 3.45 1.32 4.95L2 22l5.25-1.38a9.9 9.9 0 0 0 4.79 1.22h.01c5.5 0 9.96-4.46 9.96-9.96S17.54 2 12.04 2Zm5.86 14.06c-.25.7-1.24 1.29-2.02 1.45-.55.11-1.27.2-3.69-.79-3.1-1.28-5.1-4.41-5.25-4.61-.15-.2-1.26-1.67-1.26-3.19 0-1.52.79-2.26 1.08-2.57.24-.26.55-.35.78-.35.15 0 .3.01.43.02.34.02.51.04.72.5.25.55.85 1.98.92 2.13.07.15.12.32.02.52-.09.2-.14.32-.28.5-.14.17-.29.38-.42.5-.14.13-.29.28-.13.55.16.27.71 1.17 1.53 1.9 1.05.94 1.94 1.23 2.21 1.37.27.13.42.11.58-.07.16-.19.68-.79.87-1.06.18-.27.36-.22.6-.13.25.09 1.58.74 1.85.88.27.13.45.2.52.31.06.12.06.68-.19 1.38Z"/></svg>
        Quero esse pijama 💛
      </button>
      <p class="produto-detalhe__aviso">${temVariacoes ? 'Disponibilidade confirmada com você pelo WhatsApp.' : 'Tamanhos e disponibilidade: consulte pelo WhatsApp.'}</p>
    </div>`;

  $('conteudo-produto').querySelectorAll('img').forEach(aplicarFallbackImagem);

  const opcoes = $('opcoes-tamanho');
  if (opcoes) {
    opcoes.querySelectorAll('.tamanho-opcao').forEach(btn => {
      btn.onclick = () => {
        variacaoSelecionada = p.variacoes.find(v => v.id === +btn.dataset.vid);
        renderDetalhe();
      };
    });
  }

  ativarGaleria(imgs.length);

  $('btn-quero-esse').onclick = () => {
    window.open(mensagemWhatsapp(mensagemProduto(p)), '_blank', 'noopener');
  };
  $('btn-favoritar').onclick = () => {
    const ativo = alternarFavorito(p.id);
    renderDetalhe();
    renderGrid();
    renderCategorias();
  };
  $('btn-compartilhar').onclick = () => compartilharProduto(p);
}

function ativarGaleria(total) {
  if (total < 2) return;
  const galeria = $('galeria-detalhe');
  const dots = $('dots-detalhe') ? $('dots-detalhe').children : [];
  const minis = $('miniaturas-detalhe') ? $('miniaturas-detalhe').querySelectorAll('img') : [];

  minis.forEach(mini => {
    mini.addEventListener('click', () => {
      const idx = +mini.dataset.idx;
      const alvo = galeria.children[idx];
      if (alvo) alvo.scrollIntoView({ behavior: 'smooth', inline: 'start', block: 'nearest' });
    });
  });

  let ultimo = -1;
  galeria.addEventListener('scroll', () => {
    const idx = Math.round(galeria.scrollLeft / galeria.clientWidth);
    if (idx === ultimo) return;
    ultimo = idx;
    Array.from(dots).forEach((d, i) => d.classList.toggle('ativo', i === idx));
    minis.forEach((m, i) => m.classList.toggle('ativa', i === idx));
  }, { passive: true });
}

async function compartilharProduto(p) {
  const url = `${location.origin}${location.pathname}#/pijamas/${slugProduto(p)}`;
  const texto = `${p.nome} — ${config.nome || 'PAFANNY'}`;
  if (navigator.share) {
    try { await navigator.share({ title: texto, text: 'Olha esse pijama da PAFANNY 💛', url }); }
    catch { /* usuária cancelou — ok */ }
    return;
  }
  try {
    await navigator.clipboard.writeText(url);
    toast('Link copiado!');
  } catch {
    toast('Não foi possível copiar o link.');
  }
}

$('fechar-produto').onclick = fecharProduto;
$('overlay-produto').onclick = e => { if (e.target === $('overlay-produto')) fecharProduto(); };
document.addEventListener('keydown', e => { if (e.key === 'Escape') fecharProduto(); });
window.addEventListener('hashchange', () => {
  if (!location.hash) { $('overlay-produto').classList.remove('open'); document.body.style.overflow = ''; return; }
  abrirProdutoPelaHash();
});

// ══════════════════════════════════════════════════════════════════════════
//  GUIA DE TAMANHOS (a partir dos dados reais do catálogo, quando existirem)
// ══════════════════════════════════════════════════════════════════════════
function renderTamanhosChips() {
  const tamanhos = [...new Set(produtos.flatMap(p => (p.variacoes || []).map(v => v.tamanho)))];
  const el = $('tamanhos-chips');
  el.innerHTML = tamanhos.length ? tamanhos.map(t => `<span>${esc(t)}</span>`).join('') : '';
}

// ══════════════════════════════════════════════════════════════════════════
//  FAQ
// ══════════════════════════════════════════════════════════════════════════
const FAQ = [
  { p: 'Como faço meu pedido?', r: 'É bem simples: escolha o pijama no catálogo, toque em "Quero esse pijama 💛" e finalize direto com a gente pela conversa.' },
  { p: 'Como consultar os tamanhos?', r: 'Chame no WhatsApp direto pela peça escolhida — confirmamos tamanhos e caimento com você.' },
  { p: 'Como saber se meu tamanho está disponível?', r: 'A disponibilidade é confirmada na hora, pelo WhatsApp, junto com o restante do pedido.' },
  { p: 'Como funciona a entrega?', r: 'Os detalhes de entrega são combinados diretamente com você pelo WhatsApp, de acordo com sua localização.' },
  { p: 'Como funciona a troca?', r: 'Fale com a gente pelo WhatsApp — vamos combinar a melhor solução para o seu caso.' }
];

function renderFaq() {
  $('faq-lista').innerHTML = FAQ.map((f, i) => `
    <div class="faq-item" data-i="${i}">
      <button type="button" class="faq-item__pergunta" aria-expanded="false">
        <span>${esc(f.p)}</span>
        <svg class="faq-item__icone" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>
      <div class="faq-item__resposta"><p class="faq-item__resposta-inner">${esc(f.r)}</p></div>
    </div>`).join('');

  $('faq-lista').querySelectorAll('.faq-item').forEach(item => {
    const btn = item.querySelector('.faq-item__pergunta');
    btn.addEventListener('click', () => {
      const abrir = !item.classList.contains('open');
      item.classList.toggle('open', abrir);
      btn.setAttribute('aria-expanded', String(abrir));
    });
  });
}

// ══════════════════════════════════════════════════════════════════════════
//  DADOS ESTRUTURADOS (SEO) — sem afirmar disponibilidade que não temos
// ══════════════════════════════════════════════════════════════════════════
function injetarDadosEstruturados() {
  const itens = produtos.filter(p => p.imagens && p.imagens.length).map((p, i) => {
    const produto = {
      '@type': 'Product',
      name: p.nome,
      image: `${location.origin}${p.imagens[0]}`,
      description: p.descricao || undefined,
      sku: p.codigo || undefined,
      brand: { '@type': 'Brand', name: config.nome || 'PAFANNY' }
    };
    if (p.preco != null) {
      produto.offers = { '@type': 'Offer', priceCurrency: 'BRL', price: p.preco.toFixed(2) };
    }
    return { '@type': 'ListItem', position: i + 1, item: produto };
  });
  const dados = { '@context': 'https://schema.org', '@type': 'ItemList', itemListElement: itens };
  const script = document.createElement('script');
  script.type = 'application/ld+json';
  script.textContent = JSON.stringify(dados);
  document.head.appendChild(script);
}

// ══════════════════════════════════════════════════════════════════════════
//  MENU MOBILE
// ══════════════════════════════════════════════════════════════════════════
function ativarMenuMobile() {
  const btn = $('btn-hamburger');
  const nav = $('mobile-nav');
  btn.addEventListener('click', () => {
    const aberto = nav.classList.toggle('open');
    btn.classList.toggle('open', aberto);
    btn.setAttribute('aria-expanded', String(aberto));
  });
  nav.querySelectorAll('a').forEach(a => a.addEventListener('click', () => {
    nav.classList.remove('open'); btn.classList.remove('open'); btn.setAttribute('aria-expanded', 'false');
  }));
}

// ══════════════════════════════════════════════════════════════════════════
//  REVEAL ON SCROLL
// ══════════════════════════════════════════════════════════════════════════
function ativarReveal() {
  const alvos = document.querySelectorAll('.reveal');
  if (!('IntersectionObserver' in window)) { alvos.forEach(el => el.classList.add('in-view')); return; }
  const obs = new IntersectionObserver(entries => {
    entries.forEach(entry => {
      if (entry.isIntersecting) { entry.target.classList.add('in-view'); obs.unobserve(entry.target); }
    });
  }, { threshold: .12, rootMargin: '0px 0px -40px 0px' });
  alvos.forEach(el => obs.observe(el));
}

// ── Utils ─────────────────────────────────────────────────────────────────────
const moeda   = v => (+v || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const esc     = s => String(s ?? '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
const escAttr = s => String(s ?? '').replace(/'/g,"\\'").replace(/"/g,'&quot;');

/** Fallback elegante quando uma foto não carrega — nunca mostra o ícone quebrado do navegador. */
function aplicarFallbackImagem(img) {
  img.addEventListener('error', () => {
    if (img.dataset.fallbackAplicado) return;
    img.dataset.fallbackAplicado = '1';
    console.warn('[PAFANNY] Imagem quebrada:', img.src);
    img.src = PLACEHOLDER_IMG;
    img.style.objectFit = 'contain';
    img.style.padding = '20%';
  }, { once: true });
}

let tt;
function toast(msg) {
  const el = $('loja-toast');
  el.textContent = msg; el.classList.add('show');
  clearTimeout(tt); tt = setTimeout(() => el.classList.remove('show'), 2800);
}

iniciar();
