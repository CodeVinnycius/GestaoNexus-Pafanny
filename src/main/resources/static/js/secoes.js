/**
 * PAFANNY — secoes.js
 * Comportamento das seções novas: faixa animada (pausar), depoimentos (carrossel) e a galeria
 * estilo feed do Instagram. Opcional: sem ele o conteúdo continua lá, só sem a interação.
 */
(() => {
  const MENOS_MOVIMENTO = matchMedia('(prefers-reduced-motion: reduce)').matches;

  // Conteúdo marcado como "só no teste local" (ex.: os exemplos de depoimento) aparece só no
  // localhost. No site publicado continua escondido até ser preenchido de verdade.
  if (/^(localhost|127\.0\.0\.1|\[::1\])$/.test(location.hostname)) {
    document.querySelectorAll('[data-somente-local][hidden]').forEach(el => el.removeAttribute('hidden'));
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  DEPOIMENTOS: carrossel (arrastar, setas, pontos e troca automática com pausa)
  // ══════════════════════════════════════════════════════════════════════════
  document.querySelectorAll('.fx-depoimentos__carrossel').forEach(carrossel => {
    const trilho = carrossel.querySelector('.fx-depoimentos__trilho');
    const slides = [...trilho.children];
    const pontos = carrossel.querySelector('.fx-depoimentos__pontos');
    const botaoPausa = carrossel.querySelector('[data-acao="pausar"]');
    if (!slides.length) return;
    pontos.innerHTML = slides.map(() => '<span></span>').join('');

    const atual = () => Math.round(trilho.scrollLeft / (slides[0].offsetWidth + parseFloat(getComputedStyle(trilho).columnGap || 0)));
    const porTela = () => Math.max(1, Math.round(trilho.clientWidth / slides[0].offsetWidth));
    const temMais = () => trilho.scrollWidth > trilho.clientWidth + 4;
    function irPara(i) {
      const ultimo = slides.length - porTela();
      const alvo = i > ultimo ? 0 : i < 0 ? ultimo : i; // dá a volta nas pontas
      trilho.scrollTo({ left: slides[alvo].offsetLeft - slides[0].offsetLeft, behavior: MENOS_MOVIMENTO ? 'auto' : 'smooth' });
    }
    function marcarPonto() {
      const i = atual();
      [...pontos.children].forEach((p, k) => p.classList.toggle('ativo', k >= i && k < i + porTela()));
      carrossel.classList.toggle('fx-sem-rolagem', !temMais());
    }
    trilho.addEventListener('scroll', () => requestAnimationFrame(marcarPonto), { passive: true });
    addEventListener('resize', marcarPonto);
    requestAnimationFrame(marcarPonto);

    carrossel.querySelector('[data-acao="anterior"]').addEventListener('click', () => irPara(atual() - 1));
    carrossel.querySelector('[data-acao="proximo"]').addEventListener('click', () => irPara(atual() + 1));

    // Troca automática a cada 6s: para no hover, no foco, fora da tela, com a aba escondida,
    // pelo botão de pausa e nunca roda com prefers-reduced-motion.
    let pausadoPeloBotao = MENOS_MOVIMENTO;
    let emVista = false, sobre = false;
    const atualizarBotao = () => {
      botaoPausa.setAttribute('aria-pressed', String(pausadoPeloBotao));
      botaoPausa.setAttribute('aria-label', pausadoPeloBotao ? 'Retomar a troca automática' : 'Pausar a troca automática');
      carrossel.classList.toggle('fx-pausado', pausadoPeloBotao);
    };
    atualizarBotao();
    botaoPausa.addEventListener('click', () => { pausadoPeloBotao = !pausadoPeloBotao; atualizarBotao(); });
    carrossel.addEventListener('mouseenter', () => { sobre = true; });
    carrossel.addEventListener('mouseleave', () => { sobre = false; });
    if ('IntersectionObserver' in window) new IntersectionObserver(([e]) => { emVista = e.isIntersecting; }).observe(carrossel);
    setInterval(() => {
      if (pausadoPeloBotao || sobre || !emVista || document.hidden || carrossel.contains(document.activeElement) || !temMais()) return;
      irPara(atual() + 1);
    }, 6000);
  });

  // ══════════════════════════════════════════════════════════════════════════
  //  GALERIA ESTILO FEED DO INSTAGRAM (fotos do próprio catálogo)
  // ══════════════════════════════════════════════════════════════════════════
  const galeria = document.querySelector('.fx-galeria');
  const ICONE_INSTA = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" aria-hidden="true"><rect x="3" y="3" width="18" height="18" rx="5"/><circle cx="12" cy="12" r="4"/><circle cx="17.5" cy="6.5" r="1"/></svg>';
  function montarGaleria() {
    // `produtos` e `miniatura` vêm do loja.js (escopo global do script).
    if (!galeria || galeria.childElementCount || typeof produtos === 'undefined' || !produtos.length) return false;
    const vistas = new Set();
    const fotos = [];
    for (const p of produtos) {
      if (!p.imagens || !p.imagens.length || vistas.has(p.categoria)) continue;
      vistas.add(p.categoria);
      fotos.push({ nome: p.nome, url: p.imagens[1] || p.imagens[0] }); // a 2ª foto, para não repetir as capas
      if (fotos.length === 6) break;
    }
    if (fotos.length < 3) return true;
    const link = document.getElementById('link-instagram');
    const destino = link && link.href && !link.href.endsWith('#') ? link.href : 'https://instagram.com/usepafanny';
    const reduzir = typeof miniatura === 'function' ? miniatura : u => u;
    galeria.replaceChildren(...fotos.map(f => {
      const li = document.createElement('li');
      const a = document.createElement('a');
      a.href = destino; a.target = '_blank'; a.rel = 'noopener';
      a.setAttribute('aria-label', `${f.nome} — ver mais no Instagram da PAFANNY`);
      const img = document.createElement('img');
      img.src = reduzir(f.url, 360); img.alt = ''; img.loading = 'lazy'; img.decoding = 'async';
      img.addEventListener('error', () => li.remove(), { once: true });
      a.append(img);
      a.insertAdjacentHTML('beforeend', `<span class="fx-galeria__vel" aria-hidden="true">${ICONE_INSTA}</span>`);
      li.append(a);
      return li;
    }));
    galeria.removeAttribute('hidden');
    return true;
  }
  const gridCatalogo = document.getElementById('grid-produtos');
  if (galeria && gridCatalogo && !montarGaleria()) {
    const obs = new MutationObserver(() => { if (montarGaleria()) obs.disconnect(); });
    obs.observe(gridCatalogo, { childList: true });
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  FAIXA: botão de pausar (no toque não existe hover para parar o movimento)
  // ══════════════════════════════════════════════════════════════════════════
  document.querySelectorAll('.fx-faixa').forEach(faixa => {
    const botao = faixa.querySelector('.fx-faixa__pausa');
    if (!botao) return;
    botao.addEventListener('click', () => {
      const pausada = faixa.classList.toggle('fx-pausada');
      botao.setAttribute('aria-pressed', String(pausada));
      botao.setAttribute('aria-label', pausada ? 'Continuar a faixa animada' : 'Pausar a faixa animada');
    });
  });
})();
