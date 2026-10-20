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

  if (grid) {
    mostrarSkeleton();
    new MutationObserver(() => { mostrarSkeleton(); animarEntradaDosCards(); })
      .observe(grid, { childList: true });
    animarEntradaDosCards();

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
  //  HEADER E WHATSAPP FLUTUANTE
  // ══════════════════════════════════════════════════════════════════════════
  const header = document.getElementById('site-header');
  if (header) {
    let agendado = false;
    const atualizar = () => { agendado = false; header.classList.toggle('fx-rolou', scrollY > 10); };
    addEventListener('scroll', () => { if (!agendado) { agendado = true; requestAnimationFrame(atualizar); } }, { passive: true });
    atualizar();
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
