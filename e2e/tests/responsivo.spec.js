// Auditoria de proporção e responsividade em todos os tamanhos combinados.
//   npx playwright test responsivo --project=chromium
// Gera screenshots em e2e/screenshots/responsivo/ (topo e fim da página de cada tamanho).
const { test, expect } = require('@playwright/test');
const path = require('path');

const TAMANHOS = [
  // celulares
  [320, 568], [360, 740], [375, 667], [390, 844], [414, 896], [430, 932],
  // tablets (retrato e paisagem)
  [768, 1024], [1024, 768], [820, 1180], [1180, 820], [1024, 1366], [1366, 1024],
  // desktops
  [1280, 720], [1366, 768], [1440, 900], [1920, 1080], [2560, 1440]
];

async function percorrer(page) {
  await page.evaluate(async () => {
    for (let y = 0; y <= document.documentElement.scrollHeight; y += innerHeight / 2) {
      scrollTo(0, y); await new Promise(r => setTimeout(r, 70));
    }
  });
}

for (const [w, h] of TAMANHOS) {
  test(`${w}x${h}`, async ({ page }) => {
    const toque = w < 1024 || (w === 1024 && h === 1366);
    await page.setViewportSize({ width: w, height: h });
    await page.goto('/');
    await page.waitForSelector('.produto-card');
    await page.waitForTimeout(600);
    const pasta = path.join(__dirname, '..', 'screenshots', 'responsivo');
    await page.screenshot({ path: path.join(pasta, `${w}x${h}-topo.png`) });
    // A foto principal precisa aparecer de verdade (já sumiu no Safari por um detalhe de layout).
    const foto = await page.locator('#hero-img').boundingBox();
    expect(foto && foto.width, 'largura da foto do hero').toBeGreaterThan(Math.min(240, w * .6));
    await percorrer(page);
    await page.waitForTimeout(900);

    const r = await page.evaluate(toque => {
      const vw = document.documentElement.clientWidth;
      const visivel = el => { const s = getComputedStyle(el); const b = el.getBoundingClientRect(); return s.visibility !== 'hidden' && s.display !== 'none' && b.width > 0 && b.height > 0 && !el.closest('[hidden], [aria-hidden="true"], .sr-only, .skip-link, .loja-overlay'); };
      const desc = el => `${el.tagName.toLowerCase()}.${String(el.className).split(' ')[0]}${el.id ? '#' + el.id : ''}`;

      // 1. Nada passa da largura (fora dos trilhos que rolam ou recortam de propósito).
      const vazando = [...document.querySelectorAll('body *')].filter(el => {
        if (el.closest('.catalogo-chips, .produto-detalhe__galeria, .produto-detalhe__miniaturas, .hero, .fx-faixa__janela, .fx-depoimentos__trilho, .destaque-grid, .sr-only, .skip-link, .fx-aura')) return false;
        const b = el.getBoundingClientRect(); return b.width > 0 && (b.right > vw + 1 || b.left < -1);
      }).map(desc);

      // 2. Alvos de toque com pelo menos 44x44 (celular e tablet).
      const pequenos = !toque ? [] : [...document.querySelectorAll('main button, main a, header button, footer a, .fab-whatsapp, .fx-topo')]
        .filter(visivel)
        .filter(el => !el.closest('p')) // links dentro de um parágrafo de texto contam como texto
        .filter(el => { const b = el.getBoundingClientRect(); return b.width < 43.5 || b.height < 43.5; })
        .map(el => `${desc(el)} ${Math.round(el.getBoundingClientRect().width)}x${Math.round(el.getBoundingClientRect().height)}`);

      // 3. Nenhum texto visível menor que 12px.
      const miudos = [...document.querySelectorAll('main *, header *, footer *')].filter(el =>
        visivel(el) && [...el.childNodes].some(n => n.nodeType === 3 && n.textContent.trim()) && parseFloat(getComputedStyle(el).fontSize) < 11.9
      ).map(el => `${desc(el)} ${getComputedStyle(el).fontSize}`);

      // 4. Imagens sem distorção (object-fit "fill" com proporção diferente da original).
      const distorcidas = [...document.querySelectorAll('img')].filter(img => {
        if (!img.naturalWidth || !visivel(img) || getComputedStyle(img).objectFit !== 'fill') return false;
        const b = img.getBoundingClientRect();
        return Math.abs(b.width / b.height - img.naturalWidth / img.naturalHeight) > .03;
      }).map(img => img.src.slice(-40));

      // 5. No fim da página, o botão do WhatsApp não cobre o rodapé.
      scrollTo(0, document.documentElement.scrollHeight);
      const fab = document.getElementById('fab-whatsapp').getBoundingClientRect();
      const cobertos = [...document.querySelectorAll('.footer__copy, .footer__social a, .footer__nav a')].filter(el => {
        const b = el.getBoundingClientRect();
        return !(b.right < fab.left || b.left > fab.right || b.bottom < fab.top || b.top > fab.bottom);
      }).map(desc);

      return { scroll: document.documentElement.scrollWidth, vw, vazando: [...new Set(vazando)].slice(0, 6), pequenos: [...new Set(pequenos)].slice(0, 8), miudos: [...new Set(miudos)].slice(0, 8), distorcidas, cobertos };
    }, toque);

    await page.waitForTimeout(400);
    await page.screenshot({ path: path.join(pasta, `${w}x${h}-fim.png`) });

    expect(r.scroll, 'rolagem horizontal').toBeLessThanOrEqual(r.vw);
    expect(r.vazando, 'elementos passando da largura').toEqual([]);
    expect(r.pequenos, 'alvos de toque menores que 44px').toEqual([]);
    expect(r.miudos, 'textos menores que 12px').toEqual([]);
    expect(r.distorcidas, 'imagens distorcidas').toEqual([]);
    expect(r.cobertos, 'WhatsApp cobrindo o rodapé').toEqual([]);
  });
}

test('usa 100dvh (não 100vh) para alturas de tela', async ({ request }) => {
  for (const css of ['/css/loja.css', '/css/efeitos.css']) {
    const texto = await (await request.get(css)).text();
    expect(texto.match(/\d+vh\b/g) || [], css).toEqual([]);
  }
});
