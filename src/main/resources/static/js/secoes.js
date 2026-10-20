/**
 * PAFANNY — secoes.js
 * Comportamento das seções novas: faixa animada (pausar), depoimentos (carrossel) e a galeria
 * estilo feed do Instagram. Opcional: sem ele o conteúdo continua lá, só sem a interação.
 */
(() => {
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
