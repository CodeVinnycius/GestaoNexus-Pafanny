// Armadilhas do CLAUDE.md: overflow horizontal, site sem JS, reduced-motion e nomes que bloqueadores barram.
const { test, expect } = require('@playwright/test');

const LARGURAS = [320, 375, 414, 768, 1024, 1440, 1920];

async function percorrerPagina(page) {
  await page.evaluate(async () => {
    for (let y = 0; y < document.body.scrollHeight; y += innerHeight / 2) {
      scrollTo(0, y); await new Promise(r => setTimeout(r, 60));
    }
    scrollTo(0, 0);
  });
}

test.describe('sem overflow horizontal', () => {
  for (const largura of LARGURAS) {
    test(`${largura}px`, async ({ page }) => {
      await page.setViewportSize({ width: largura, height: 900 });
      await page.goto('/');
      await page.waitForSelector('.produto-card');
      await percorrerPagina(page);
      const medidas = await page.evaluate(() => ({
        scroll: document.documentElement.scrollWidth,
        client: document.documentElement.clientWidth,
        culpados: [...document.querySelectorAll('body *')]
          .filter(el => el.getBoundingClientRect().right > document.documentElement.clientWidth + 1
            && !el.closest('.catalogo-chips, .produto-detalhe__galeria, .produto-detalhe__miniaturas, .hero, .sr-only, .skip-link'))
          .slice(0, 5).map(el => `${el.tagName}.${el.className}`)
      }));
      expect(medidas.culpados, 'elementos passando da largura da tela').toEqual([]);
      expect(medidas.scroll).toBeLessThanOrEqual(medidas.client);

      // O modal de detalhe também não pode vazar.
      await page.locator('.produto-card__link').first().click();
      await page.waitForSelector('#btn-quero-esse');
      const modal = await page.evaluate(() => {
        const s = document.getElementById('sheet-produto');
        return { scroll: s.scrollWidth, client: s.clientWidth };
      });
      expect(modal.scroll).toBeLessThanOrEqual(modal.client);
    });
  }
});

test.describe('sem JavaScript', () => {
  test.use({ javaScriptEnabled: false });

  test('conteúdo visível e completo', async ({ page }) => {
    await page.goto('/');
    for (const titulo of ['O conforto que', 'Escolha seu momento PAFANNY', 'Os pijamas PAFANNY',
      'Mais que um pijama, um momento seu.', 'Uma marca feita de carinho', 'Guia de Tamanhos',
      'Como comprar', 'Perguntas frequentes', 'Siga a PAFANNY']) {
      await expect(page.locator('main').getByText(titulo).first()).toBeVisible();
    }
    // Nada pode ficar preso em opacity 0 esperando o JS.
    const invisiveis = await page.evaluate(() => [...document.querySelectorAll('main *')]
      .filter(el => getComputedStyle(el).opacity === '0' && !el.closest('[hidden]'))
      .map(el => el.className || el.tagName));
    expect(invisiveis).toEqual([]);
    await expect(page.locator('#hero-img')).toBeVisible();
  });
});

test.describe('JS bloqueado ou API travada', () => {
  const semNadaInvisivel = page => page.evaluate(() => [...document.querySelectorAll('main .reveal')]
    .filter(el => getComputedStyle(el).opacity !== '1').map(el => el.className));

  test('loja.js bloqueado: conteúdo aparece mesmo assim', async ({ page }) => {
    await page.route('**/js/loja.js', r => r.abort());
    await page.goto('/');
    await page.waitForTimeout(5000);
    expect(await semNadaInvisivel(page)).toEqual([]);
  });

  test('API lenta: o conteúdo do <noscript> nunca aparece como texto', async ({ page }) => {
    await page.route('**/api/loja/produtos', () => { /* nunca responde */ });
    await page.goto('/');
    await page.waitForTimeout(1000);
    const texto = await page.locator('#catalogo').innerText();
    expect(texto).not.toContain('wa.me');
    expect(texto).not.toContain('<article');
    const largura = await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth);
    expect(largura).toBe(true);
  });

  test('API de produtos travada: hero aparece na hora', async ({ page }) => {
    await page.route('**/api/loja/**', () => { /* nunca responde */ });
    await page.goto('/');
    await page.waitForTimeout(2000);
    await expect(page.locator('#hero-img')).toBeVisible();
    expect(await page.locator('.hero__copy').evaluate(el => getComputedStyle(el).opacity)).toBe('1');
  });
});

test.describe('prefers-reduced-motion', () => {
  test.use({ reducedMotion: 'reduce' });

  test('sem movimento contínuo, conteúdo e estilo mantidos', async ({ page }) => {
    await page.goto('/');
    await page.waitForSelector('.produto-card');
    await percorrerPagina(page);
    await page.waitForTimeout(500);
    const emLoop = await page.evaluate(() => document.getAnimations()
      .filter(a => a.playState === 'running' && a.effect && a.effect.getTiming().iterations === Infinity)
      .map(a => a.animationName || a.constructor.name));
    expect(emLoop).toEqual([]);
    const opacidades = await page.evaluate(() => [...document.querySelectorAll('.reveal')]
      .map(el => getComputedStyle(el).opacity));
    expect(opacidades.every(o => o === '1')).toBe(true);
  });
});

test('nenhum nome que bloqueadores de anúncio costumam barrar', async ({ page }) => {
  const proibido = /(^|[^a-z])(ads?|advert\w*|banners?|track\w*|analytics|promo\w*|sponsor\w*)([^a-z]|$)/i;
  const urls = [];
  page.on('request', r => urls.push(new URL(r.url()).pathname));
  await page.goto('/');
  await page.waitForSelector('.produto-card');
  const nomes = await page.evaluate(() => [...document.querySelectorAll('*')]
    .flatMap(el => [el.id, ...el.classList]).filter(Boolean));
  expect(nomes.filter(n => proibido.test(n))).toEqual([]);
  expect(urls.filter(u => !u.startsWith('/uploads/') && proibido.test(u))).toEqual([]);
});
