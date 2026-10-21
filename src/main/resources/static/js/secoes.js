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
    marcarPonto();

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
