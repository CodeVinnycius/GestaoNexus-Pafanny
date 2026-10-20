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
})();
