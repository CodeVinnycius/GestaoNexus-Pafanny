/**
 * PAFANNY — efeitos.js
 * Comportamentos dos efeitos visuais (par do css/efeitos.css).
 * Nada aqui é necessário para o catálogo ou para o WhatsApp funcionarem:
 * remover o <script> e o <link> volta à loja original.
 */
(() => {
  const MENOS_MOVIMENTO = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const COM_MOUSE = matchMedia('(hover: hover) and (pointer: fine)').matches;
  const grid = document.getElementById('grid-produtos');

  // ══════════════════════════════════════════════════════════════════════════
  //  CATÁLOGO
  // ══════════════════════════════════════════════════════════════════════════
  const SKELETON = Array.from({ length: 10 }, () => `
    <div class="fx-skel" aria-hidden="true">
      <div class="fx-skel__img"></div>
      <div class="fx-skel__linha"></div>
      <div class="fx-skel__linha fx-skel__linha--curta"></div>
    </div>`).join('');

  /** Troca o "Carregando peças…" por cards-fantasma; o texto continua para leitores de tela. */
  function mostrarSkeleton() {
    const msg = grid.querySelector(':scope > p.loja-msg');
    if (!msg || !/^Carregando/.test(msg.textContent.trim()) || grid.querySelector('.fx-skel')) return;
    msg.classList.add('sr-only');
    msg.insertAdjacentHTML('afterend', SKELETON);
  }

  /** Cards novos entram em sequência. "Ver mais" anima só os acrescentados; favoritar não anima nada. */
  let idsAnteriores = [];
  function animarEntradaDosCards() {
    const cards = [...grid.querySelectorAll(':scope > .produto-card')];
    if (!cards.length) { idsAnteriores = []; return; }
    const ids = cards.map(c => c.dataset.id);
    const continuacao = idsAnteriores.length && idsAnteriores.every((id, i) => ids[i] === id);
    const inicio = continuacao ? idsAnteriores.length : 0;
    if (!MENOS_MOVIMENTO) {
      cards.slice(inicio).forEach((card, i) => {
        card.style.setProperty('--fx-i', Math.min(i, 9));
        card.classList.add('fx-entra');
      });
    }
    idsAnteriores = ids;
  }

  // Badge "Novidade" (opcional): coloque aqui o CÓDIGO das peças novas, ex.: ['1915', '747'].
  // Para só visualizar, sem mexer no arquivo: abra a loja com ?novidades=1915,747 no endereço.
  const CODIGOS_NOVIDADE = [];
  const novidades = new Set([
    ...CODIGOS_NOVIDADE.map(String),
    ...(new URLSearchParams(location.search).get('novidades') || '').split(',').map(c => c.trim()).filter(Boolean)
  ]);
  const OLHO = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" aria-hidden="true"><path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12Z"/><circle cx="12" cy="12" r="2.8"/></svg>';

  /** Enfeites dos cards recém-desenhados: "Novidade" e o selo "Olhar rápido". */
  function enfeitarCards() {
    // `produtos` é o catálogo carregado pelo loja.js (escopo global do script).
    const lista = typeof produtos !== 'undefined' ? produtos : [];
    grid.querySelectorAll(':scope > .produto-card:not([data-fx-enfeitado])').forEach(card => {
      card.dataset.fxEnfeitado = '1';
      const area = card.querySelector('.produto-card__img');
      if (!area) return;
      const p = lista.find(x => String(x.id) === card.dataset.id);
      if (p && p.codigo && novidades.has(String(p.codigo))) {
        area.insertAdjacentHTML('beforeend', '<span class="fx-novidade">Novidade</span>');
      }
      // Atalho visual para o mouse; teclado e leitor de tela usam o "Ver detalhes" do card.
      // O clique sobe até o card, que já abre o detalhe (loja.js).
      if (COM_MOUSE) area.insertAdjacentHTML('beforeend', `<span class="fx-olhar" aria-hidden="true">${OLHO}Olhar rápido</span>`);
    });
  }

  if (grid) {
    mostrarSkeleton();
    new MutationObserver(() => { mostrarSkeleton(); animarEntradaDosCards(); enfeitarCards(); })
      .observe(grid, { childList: true });
    animarEntradaDosCards();
    enfeitarCards();

    // Segunda foto no hover (só com mouse). Ela só é baixada no primeiro hover do card.
    if (COM_MOUSE) {
      grid.addEventListener('mouseover', e => {
        const card = e.target.closest('.produto-card');
        if (!card || card.dataset.fxFoto2) return;
        card.dataset.fxFoto2 = '1';
        const img = card.querySelector('.produto-card__img img[data-foto2]');
        if (!img) return;
        const foto2 = document.createElement('img');
        foto2.className = 'fx-foto2';
        foto2.alt = '';
        foto2.setAttribute('aria-hidden', 'true');
        foto2.decoding = 'async';
        foto2.addEventListener('load', () => foto2.classList.add('carregada'), { once: true });
        foto2.addEventListener('error', () => foto2.remove(), { once: true });
        foto2.src = img.dataset.foto2;
        img.after(foto2);
      });
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  CHIPS: o chip escolhido "infla" — só quando a linha escolhida muda
  // ══════════════════════════════════════════════════════════════════════════
  const chips = document.getElementById('categorias');
  if (chips) {
    let ativoAnterior = null;
    new MutationObserver(() => {
      const ativo = chips.querySelector('.chip.active');
      const chave = ativo ? (ativo.dataset.cat || ativo.id) : null;
      if (ativo && chave !== ativoAnterior && ativoAnterior !== null && !MENOS_MOVIMENTO) ativo.classList.add('fx-chip-novo');
      ativoAnterior = chave;
    }).observe(chips, { childList: true });
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  CARDS: inclinação 3D sutil seguindo o mouse (só desktop) e brilho no hover
  // ══════════════════════════════════════════════════════════════════════════
  if (COM_MOUSE && !MENOS_MOVIMENTO) {
    const INCLINACAO = 4; // graus, no máximo
    let cardAtual = null;
    let quadro = 0;
    document.addEventListener('pointermove', e => {
      const card = e.target.closest && e.target.closest('.produto-card, .destaque-card');
      if (cardAtual && card !== cardAtual) { cardAtual.classList.remove('fx-tilt'); cardAtual.style.removeProperty('--fx-rx'); cardAtual.style.removeProperty('--fx-ry'); }
      cardAtual = card;
      if (!card) return;
      cancelAnimationFrame(quadro);
      quadro = requestAnimationFrame(() => {
        const r = card.getBoundingClientRect();
        const x = (e.clientX - r.left) / r.width - .5;
        const y = (e.clientY - r.top) / r.height - .5;
        card.style.setProperty('--fx-rx', `${(-y * INCLINACAO).toFixed(2)}deg`);
        card.style.setProperty('--fx-ry', `${(x * INCLINACAO).toFixed(2)}deg`);
        card.classList.add('fx-tilt');
      });
    }, { passive: true });
    document.documentElement.addEventListener('mouseleave', () => { if (cardAtual) cardAtual.classList.remove('fx-tilt'); cardAtual = null; });
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  HEADER E WHATSAPP FLUTUANTE
  // ══════════════════════════════════════════════════════════════════════════
  const header = document.getElementById('site-header');
  if (header) {
    let agendado = false;
    const atualizar = () => { agendado = false; header.classList.toggle('fx-rolou', scrollY > 10); };
    addEventListener('scroll', () => { if (!agendado) { agendado = true; requestAnimationFrame(atualizar); } }, { passive: true });
    // Sem leitura inicial de scrollY (forçaria o layout da página no carregamento): se a página
    // abrir já rolada, o navegador dispara um evento de rolagem e o header se ajusta.
  }

  const fab = document.getElementById('fab-whatsapp');
  if (fab) fab.insertAdjacentHTML('afterend', '<span class="fx-fab-pulso" aria-hidden="true"></span>');

  // ══════════════════════════════════════════════════════════════════════════
  //  CORAÇÕES NO "QUERO ESSE"
  // ══════════════════════════════════════════════════════════════════════════
  // Ouvinte à parte, no document: roda depois do clique do loja.js, que já abriu o WhatsApp.
  const CORES_CORACAO = ['#C24B5E', '#D4AF37', '#E7CE84'];
  document.addEventListener('click', e => {
    const botao = e.target.closest('#btn-quero-esse');
    if (!botao || MENOS_MOVIMENTO) return;
    botao.classList.remove('fx-pulsando');
    void botao.offsetWidth; // reinicia a animação se clicar de novo
    botao.classList.add('fx-pulsando');
    const r = botao.getBoundingClientRect();
    for (let i = 0; i < 5; i++) {
      const c = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
      c.setAttribute('viewBox', '0 0 24 24');
      c.setAttribute('aria-hidden', 'true');
      c.classList.add('fx-coracao');
      c.innerHTML = `<path fill="${CORES_CORACAO[i % 3]}" d="M12 21s-7.5-4.6-10-9.3C.4 8.2 2 4.8 5.4 4.1c2-.4 4 .5 6.6 3 2.6-2.5 4.6-3.4 6.6-3C21.9 4.8 23.5 8.2 22 11.7 19.5 16.4 12 21 12 21z"/>`;
      c.style.left = `${r.left + r.width * (.3 + i * .1)}px`;
      c.style.top = `${r.top + 6}px`;
      c.style.setProperty('--fx-x', `${(i - 2) * 12 + (Math.random() * 10 - 5)}px`);
      c.style.setProperty('--fx-r', `${Math.random() * 30 - 15}deg`);
      c.style.animationDelay = `${i * 70}ms`;
      c.addEventListener('animationend', () => c.remove(), { once: true });
      document.body.appendChild(c);
    }
  });
})();
