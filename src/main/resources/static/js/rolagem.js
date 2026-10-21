/**
 * PAFANNY — rolagem.js
 * Tudo o que acompanha a rolagem: scroll suave (Lenis, só no desktop com mouse), header que some ao
 * descer e volta ao subir, barra de progresso, botão "voltar ao topo", seção ativa no menu, tom de
 * fundo por seção, títulos palavra por palavra e o modo de animações guiadas pelo scroll (.fx-sd).
 * Opcional: a loja funciona igual sem este arquivo.
 */
(() => {
  const raiz = document.documentElement;
  const MENOS_MOVIMENTO = matchMedia('(prefers-reduced-motion: reduce)').matches;
  const COM_MOUSE = matchMedia('(hover: hover) and (pointer: fine)').matches;
  // Animações guiadas pelo scroll direto no CSS (Chrome, Edge, Safari 26+). Sem suporte, o reveal
  // por IntersectionObserver do loja.js continua valendo.
  const SCROLL_DRIVEN = !MENOS_MOVIMENTO && window.CSS && CSS.supports('animation-timeline: view()');
  if (SCROLL_DRIVEN) raiz.classList.add('fx-sd');
  const scriptAtual = document.currentScript;

  let lenis = null;
  const ALTURA_HEADER = 84;

  /** Rola até um elemento (ou topo), suave quando permitido; o foco vai junto para quem usa teclado. */
  function irPara(alvo, focar) {
    if (lenis) lenis.scrollTo(alvo === 0 ? 0 : alvo, { offset: alvo === 0 ? 0 : -ALTURA_HEADER });
    else if (alvo === 0) scrollTo({ top: 0, behavior: MENOS_MOVIMENTO ? 'auto' : 'smooth' });
    else alvo.scrollIntoView({ behavior: MENOS_MOVIMENTO ? 'auto' : 'smooth', block: 'start' });
    if (focar) {
      if (!focar.hasAttribute('tabindex') && !focar.matches('a, button, input, select, textarea')) focar.setAttribute('tabindex', '-1');
      focar.focus({ preventScroll: true });
    }
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  BARRA DE PROGRESSO, HEADER QUE SOME/VOLTA E "VOLTAR AO TOPO"
  // ══════════════════════════════════════════════════════════════════════════
  const header = document.getElementById('site-header');
  const botaoMenu = document.getElementById('btn-hamburger');

  document.body.insertAdjacentHTML('afterbegin',
    '<div class="fx-progresso" aria-hidden="true"><span></span></div>' +
    '<div class="fx-aura" aria-hidden="true"><i class="fx-aura__tom" data-tom="1"></i><i class="fx-aura__tom" data-tom="2"></i><i class="fx-aura__tom" data-tom="3"></i></div>');
  const barra = document.querySelector('.fx-progresso span');

  const fab = document.getElementById('fab-whatsapp');
  const topo = document.createElement('button');
  topo.type = 'button';
  topo.className = 'fx-topo';
  topo.setAttribute('aria-label', 'Voltar ao topo');
  topo.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M12 19V5M5.5 11.5 12 5l6.5 6.5"/></svg>';
  (fab || document.body.lastElementChild).before(topo);
  topo.addEventListener('click', () => irPara(0, header && header.querySelector('.brand')));

  let ultimoY = null; // lido no primeiro quadro (ler scrollY já forçaria o layout da página)
  let agendado = false;
  let subindo = false;
  const telaEstreita = matchMedia('(max-width: 699px)');
  function aoRolar() {
    agendado = false;
    const y = scrollY;
    if (ultimoY === null) ultimoY = y;
    const menuAberto = botaoMenu && botaoMenu.getAttribute('aria-expanded') === 'true';
    const focoNoHeader = header && header.contains(document.activeElement);
    if (header) {
      if (y > 160 && y > ultimoY + 6 && !menuAberto && !focoNoHeader) header.classList.add('fx-escondido');
      else if (y < ultimoY - 6 || y <= 160) header.classList.remove('fx-escondido');
    }
    // No celular o botão só aparece rolando para cima (não fica sobre o conteúdo durante a leitura).
    if (y < ultimoY - 6) subindo = true; else if (y > ultimoY + 6) subindo = false;
    topo.classList.toggle('fx-visivel', y > 900 && (!telaEstreita.matches || subindo));
    if (!SCROLL_DRIVEN && barra) {
      const max = document.documentElement.scrollHeight - innerHeight;
      barra.style.transform = `scaleX(${max > 0 ? Math.min(1, y / max) : 0})`;
    }
    ultimoY = y;
  }
  addEventListener('scroll', () => { if (!agendado) { agendado = true; requestAnimationFrame(aoRolar); } }, { passive: true });
  if (header) header.addEventListener('focusin', () => header.classList.remove('fx-escondido'));
  // Sem chamada inicial: ler scrollY no carregamento forçaria o layout; o primeiro scroll ajusta tudo.

  // ══════════════════════════════════════════════════════════════════════════
  //  SEÇÃO ATIVA NO MENU E TOM DE FUNDO POR SEÇÃO
  // ══════════════════════════════════════════════════════════════════════════
  const secoes = [...document.querySelectorAll('main > section')];
  const TONS = [1, 1, 2, 3, 1, 2, 3, 1, 2, 3, 1]; // um tom (da paleta) para cada seção, na ordem da página
  secoes.forEach((s, i) => { s.dataset.fxTom = TONS[i % TONS.length]; });
  raiz.dataset.fxTom = '1';

  const links = [...document.querySelectorAll('.nav__link[href^="#"], .mobile-nav__link[href^="#"]')];
  if ('IntersectionObserver' in window) {
    const obs = new IntersectionObserver(entradas => {
      entradas.forEach(e => {
        if (!e.isIntersecting) return;
        raiz.dataset.fxTom = e.target.dataset.fxTom;
        const id = e.target.id;
        links.forEach(l => {
          const ativo = !!id && l.getAttribute('href') === `#${id}`;
          l.classList.toggle('ativo', ativo);
          if (ativo) l.setAttribute('aria-current', 'location'); else l.removeAttribute('aria-current');
        });
      });
    }, { rootMargin: '-45% 0px -50% 0px' }); // a seção que cruza o meio da tela
    secoes.forEach(s => obs.observe(s));
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  TÍTULOS PALAVRA POR PALAVRA
  // ══════════════════════════════════════════════════════════════════════════
  // O título continua legível para leitores de tela (aria-label com o texto inteiro).
  if (!MENOS_MOVIMENTO) {
    document.querySelectorAll('.section__title').forEach(titulo => {
      if (titulo.children.length) return;
      const texto = titulo.textContent.trim();
      titulo.setAttribute('aria-label', texto);
      titulo.textContent = '';
      texto.split(/\s+/).forEach((palavra, i) => {
        if (i) titulo.append(' ');
        const s = document.createElement('span');
        s.className = 'fx-palavra';
        s.setAttribute('aria-hidden', 'true');
        s.style.setProperty('--p', i);
        s.textContent = palavra;
        titulo.append(s);
      });
      titulo.classList.add('fx-palavras');
    });
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  "COMO COMPRAR": LINHA LIGANDO OS NÚMEROS
  // ══════════════════════════════════════════════════════════════════════════
  // Mede o centro do 1º e do último número; o CSS desenha a linha entre eles (vertical no
  // celular, horizontal a partir de 800px) e a anima com a rolagem.
  const passos = document.querySelector('.passos');
  function medirLinhaDosPassos() {
    const nums = passos ? passos.querySelectorAll('.passo__num') : [];
    if (nums.length < 2) return;
    // Posição de layout (offset*), que ignora as animações de entrada em andamento.
    const centro = el => {
      let x = el.offsetWidth / 2, y = el.offsetHeight / 2;
      for (let n = el; n && n !== passos; n = n.offsetParent) { x += n.offsetLeft; y += n.offsetTop; }
      return { x, y };
    };
    const a = centro(nums[0]), b = centro(nums[nums.length - 1]);
    const horizontal = Math.abs(b.x - a.x) > Math.abs(b.y - a.y);
    passos.style.setProperty('--fx-linha-ini', `${horizontal ? a.x : a.y}px`);
    passos.style.setProperty('--fx-linha-tam', `${Math.abs(horizontal ? b.x - a.x : b.y - a.y)}px`);
    passos.style.setProperty('--fx-linha-x', `${a.x}px`);
    passos.style.setProperty('--fx-linha-y', `${a.y}px`);
    passos.classList.add('fx-com-linha');
  }
  if (passos) {
    // Mede quando a seção se aproxima da tela (o layout já está pronto, sem forçar um cálculo extra).
    if ('IntersectionObserver' in window) {
      const obs = new IntersectionObserver(([e]) => { if (e.isIntersecting) { medirLinhaDosPassos(); obs.disconnect(); } }, { rootMargin: '600px 0px' });
      obs.observe(passos);
    } else requestAnimationFrame(medirLinhaDosPassos);
    let espera;
    addEventListener('resize', () => { clearTimeout(espera); espera = setTimeout(medirLinhaDosPassos, 150); });
    if (document.fonts) document.fonts.ready.then(medirLinhaDosPassos);
  }

  // ══════════════════════════════════════════════════════════════════════════
  //  SCROLL SUAVE (LENIS) — só desktop com mouse, nunca no toque nem com menos movimento
  // ══════════════════════════════════════════════════════════════════════════
  function ligarLenis() {
    const src = scriptAtual && scriptAtual.dataset.lenis;
    if (!src || MENOS_MOVIMENTO || !COM_MOUSE) return;
    const s = document.createElement('script');
    s.src = src;
    s.async = true;
    s.onload = () => {
      if (!window.Lenis) return;
      lenis = new window.Lenis({
        lerp: 0.12,
        smoothWheel: true,
        syncTouch: false,
        autoRaf: true,
        // Áreas que rolam sozinhas continuam com a rolagem nativa.
        prevent: no => !!(no.closest && no.closest('.loja-overlay, .catalogo-chips, .produto-detalhe__galeria, .produto-detalhe__miniaturas, .fx-depoimentos__trilho'))
      });
      // Modal aberto ou menu mobile aberto: a página para (o modal rola por conta própria).
      const overlay = document.getElementById('overlay-produto');
      if (overlay) new MutationObserver(() => overlay.classList.contains('open') ? lenis.stop() : lenis.start())
        .observe(overlay, { attributes: true, attributeFilter: ['class'] });
    };
    document.head.appendChild(s);
  }
  if (document.readyState === 'complete') setTimeout(ligarLenis, 300);
  else addEventListener('load', () => setTimeout(ligarLenis, 300), { once: true });

  // Links internos (#secao): rolagem suave até a seção, compensando o header, e foco junto.
  document.addEventListener('click', e => {
    const a = e.target.closest('a[href^="#"]');
    if (!a || e.defaultPrevented || e.button !== 0 || e.metaKey || e.ctrlKey) return;
    const id = a.getAttribute('href').slice(1);
    if (!id || id.startsWith('/')) return; // "#" vazio ou rota de produto (#/pijamas/...)
    const alvo = document.getElementById(id);
    if (!alvo) return;
    e.preventDefault();
    if (history.pushState) history.pushState(null, '', `#${id}`);
    irPara(alvo === document.getElementById('topo') ? 0 : alvo, alvo);
  });

  window.pafannyRolagem = { irPara };
})();
