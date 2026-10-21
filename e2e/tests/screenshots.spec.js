// Screenshots antes/depois: hero, cards, catálogo e celular.
// Uso: SHOTS=depois npx playwright test screenshots --project=chromium
//      (para o "antes", aponte LOJA_URL para a versão da tag de produção rodando localmente)
const { test } = require('@playwright/test');
const path = require('path');

const ROTULO = process.env.SHOTS || 'atual';
const destino = arquivo => path.join(__dirname, '..', 'screenshots', ROTULO, arquivo);

async function abrir(page, largura, altura, movel) {
  await page.setViewportSize({ width: largura, height: altura });
  await page.goto('/');
  await page.waitForSelector('.produto-card');
  await page.waitForTimeout(2500); // animações de entrada do topo
}

/** Rola até o seletor (topo dele a `margem` px do topo da tela), percorrendo o caminho, e fotografa a tela. */
async function quadro(page, seletor, margem, arquivo) {
  await page.evaluate(async ([sel, margem]) => {
    const alvo = document.querySelector(sel).getBoundingClientRect().top + scrollY - margem;
    for (let y = scrollY; y < alvo; y += innerHeight / 3) { scrollTo(0, y); await new Promise(r => setTimeout(r, 60)); }
    scrollTo(0, alvo);
  }, [seletor, margem]);
  await page.waitForTimeout(1600);
  await page.screenshot({ path: destino(arquivo) });
}

test('desktop', async ({ page }) => {
  await abrir(page, 1440, 900);
  await page.screenshot({ path: destino('desktop-hero.png') });
  await quadro(page, 'section[aria-labelledby="destaque-title"] .destaque-grid', 140, 'desktop-cards-linhas.png');
  await quadro(page, '#catalogo', 0, 'desktop-catalogo.png');
  // Card do catálogo com o mouse em cima.
  const card = page.locator('.produto-card').nth(1);
  await card.scrollIntoViewIfNeeded();
  const r = await card.boundingBox();
  await page.mouse.move(r.x + r.width * .7, r.y + r.height * .3);
  await page.waitForTimeout(1400);
  const caixa = await card.boundingBox();
  await page.screenshot({ path: destino('desktop-card-hover.png'), clip: { x: caixa.x - 40, y: caixa.y - 40, width: caixa.width + 80, height: caixa.height + 80 } });
  await quadro(page, 'section[aria-labelledby="experiencia-title"]', 60, 'desktop-essencia.png');
  await quadro(page, '.footer', 420, 'desktop-rodape.png');
});

test('celular', async ({ page }) => {
  await abrir(page, 390, 844, true);
  await page.screenshot({ path: destino('celular-hero.png') });
  await quadro(page, 'section[aria-labelledby="destaque-title"]', 40, 'celular-linhas.png');
  await quadro(page, '#catalogo', 0, 'celular-catalogo.png');
  await quadro(page, '.grid-produtos', 20, 'celular-cards.png');
  await quadro(page, 'section[aria-labelledby="comprar-title"]', 40, 'celular-como-comprar.png');
  await quadro(page, '.footer', 360, 'celular-rodape.png');
});

test('tablet', async ({ page }) => {
  await abrir(page, 768, 1024);
  await page.screenshot({ path: destino('tablet-hero.png') });
  await quadro(page, '#catalogo', 0, 'tablet-catalogo.png');
});
